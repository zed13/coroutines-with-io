# Investigation Journal: Coroutines with IO

## Date: 2025-12-04

## Investigation Goal
Understanding how coroutines interact with different HTTP client engines (CIO vs OkHttp) and how dispatchers affect the duration of 100 concurrent requests to a server with 1-second delay per request.

---

## Initial Problem Statement

### Observed Behavior

**CIO Engine:**
- Duration: ~5 seconds for all dispatcher configurations
- Predictable performance
- No matter what dispatcher used (IO or single-threaded)

**OkHttp Engine (Initial):**
- Duration: ~60 seconds
- Last enqueued requests taking 20+ seconds to complete
- Poor performance regardless of dispatcher (single-threaded or unlimited)

### Expectation
Expected OkHttp with unlimited threads (Dispatchers.IO or cached thread executor) to show similar performance to CIO.

---

## Root Cause Analysis

### First Hypothesis: Connection Pool Limits
**Initial thought:** OkHttp's default connection pool of 5 idle connections was the bottleneck.

**Configuration attempted:**
```kotlin
connectionPool(ConnectionPool(
    maxIdleConnections = 100,
    keepAliveDuration = 5,
    timeUnit = TimeUnit.MINUTES,
))
```

**Result:** Did NOT fix the issue. Still seeing 20+ second delays.

### Second Hypothesis: OkHttp Dispatcher Limits (CORRECT)
**The real bottleneck:** OkHttp's `Dispatcher` has request queuing limits that control how many concurrent requests can be executed.

**Default limits:**
- `maxRequests = 64` (total concurrent requests)
- `maxRequestsPerHost = 5` (per host) ← **This was the actual bottleneck**

**Explanation:**
When creating `Dispatcher(Executors.newCachedThreadPool())`, only the thread pool was configured, but the Dispatcher still enforced its default limit of 5 concurrent requests per host. This caused requests to be processed in batches of ~5, resulting in:
- 100 requests / 5 concurrent = 20 batches
- 20 batches × 1 second delay = 20+ seconds total time

**Solution:**
```kotlin
val dispatcher = Dispatcher(Executors.newCachedThreadPool()).apply {
    maxRequests = 100
    maxRequestsPerHost = 100
}
dispatcher(dispatcher)
```

**Result:** FIXED! Performance dramatically improved.

---

## Final Performance Results

After applying the Dispatcher configuration fix:

- **OkHttp + any dispatcher**: ~1.2 seconds
- **CIO + any dispatcher**: ~1.7 seconds

---

## Key Insights

### 1. Dispatcher vs Connection Pool
- **Dispatcher** controls request queuing (how many requests can be actively executed)
- **Connection Pool** manages connection reuse (how idle connections are kept for reuse)
- The Dispatcher's `maxRequestsPerHost` limit gates requests BEFORE they reach the connection pool

### 2. Coroutine Dispatcher Independence

**Surprising finding:** The coroutine dispatcher choice (single-threaded vs Dispatchers.IO) has minimal impact on performance for both engines.

**OkHttp behavior:**
- Uses its own internal thread pool (via Dispatcher) for I/O operations
- Coroutine suspends immediately when making a request
- OkHttp's internal threads handle the actual network I/O in parallel
- Coroutine resumes when response arrives
- Even with a single-threaded coroutine dispatcher, OkHttp achieves full I/O parallelism

**CIO behavior:**
- Pure coroutine-based, non-blocking I/O (Java NIO)
- No separate internal thread pool
- Event-driven parallelism
- Doesn't need threads to "wait" for network responses
- More coroutine threads provide minimal benefit because I/O is already non-blocking

### 3. Architecture Differences

**OkHttp:**
- Thread-based parallelism model
- Internal ExecutorService handles I/O
- Blocking I/O model abstracted behind async interface
- HTTP/2 support (multiplexing)

**CIO:**
- Event-driven parallelism model
- Coroutine-native implementation
- Non-blocking I/O (NIO)
- HTTP/1.x only (no HTTP/2)
- Lightweight, minimal overhead

### 4. Performance Characteristics

**Why OkHttp is faster (1.2s vs 1.7s):**
Possible explanations:
1. **HTTP/2 multiplexing** - OkHttp supports HTTP/2 by default, CIO only supports HTTP/1.x
2. **Connection establishment** - Different overhead for connection setup
3. **Internal buffering strategies** - Different approaches to reading/writing data
4. **Server interaction** - The Jetty server configuration might interact differently with each client

**CPU-bound work consideration:**
Both engines likely handle CPU-bound operations (JSON serialization/deserialization via ContentNegotiation plugin) similarly, but the exact integration points may differ. This could be a factor in the performance difference and warrants further investigation.

---

## Architecture Flow Diagrams

### OkHttp Request Flow
```
Coroutine (on your dispatcher)
    ↓
api.getDatetime() called
    ↓
Coroutine SUSPENDS (frees up thread)
    ↓
OkHttp Dispatcher (internal thread pool)
    ↓ (up to maxRequestsPerHost concurrent requests)
Network I/O on OkHttp's threads (100 parallel)
    ↓
Response received
    ↓
Coroutine RESUMES (on your dispatcher)
    ↓
Result returned
```

### CIO Request Flow
```
Coroutine (on your dispatcher)
    ↓
api.getDatetime() called
    ↓
Coroutine SUSPENDS (frees up thread)
    ↓
Non-blocking NIO (event-driven)
    ↓ (no thread waiting)
Network I/O (100 parallel, event-driven)
    ↓
Response received (I/O event)
    ↓
Coroutine RESUMES (on your dispatcher)
    ↓
Result returned
```

---

## Conclusions

1. **OkHttp requires explicit Dispatcher configuration** for high-concurrency scenarios. The default `maxRequestsPerHost = 5` is too restrictive for parallel load testing.

2. **Connection pool size is secondary** to Dispatcher limits. Increasing connection pool without adjusting Dispatcher limits has no effect.

3. **Coroutine dispatchers matter less than expected** for I/O-bound operations. Both engines achieve parallelism through their own mechanisms:
   - OkHttp: Internal thread pool
   - CIO: Non-blocking I/O

4. **Both engines perform well** once properly configured, with OkHttp showing slight edge (~1.2s vs ~1.7s), possibly due to HTTP/2 support.

5. **The coroutine abstraction works** - suspend functions provide a uniform interface regardless of the underlying I/O model (thread-based vs event-driven).

---

## Further Investigation Opportunities

1. **HTTP protocol analysis**: Confirm if OkHttp is using HTTP/2 vs CIO's HTTP/1.x
2. **JSON parsing overhead**: Measure time spent in serialization/deserialization
3. **Connection reuse patterns**: Monitor how each engine reuses connections
4. **Disable HTTP/2 in OkHttp**: Test if performance equalizes with HTTP/1.1 only
5. **Raw requests without JSON**: Isolate network performance from serialization overhead
6. **Server-side logging**: Understand how server processes requests from each client
7. **Memory profiling**: Compare memory usage patterns between engines
8. **CPU profiling**: Identify where CPU time is spent in each engine

---

## References

### OkHttp Documentation
- [maxRequestsPerHost - OkHttp 5.x](https://square.github.io/okhttp/5.x/okhttp/okhttp3/-dispatcher/max-requests-per-host.html)
- [Dispatcher (OkHttp 3.14.0 API)](https://square.github.io/okhttp/3.x/okhttp/okhttp3/Dispatcher.html)
- [ConnectionPool (OkHttp 3.14.0 API)](https://square.github.io/okhttp/3.x/okhttp/okhttp3/ConnectionPool.html)

### Stack Overflow Discussions
- [OkHttpClient connection pool size dilemma](https://stackoverflow.com/questions/49069297/okhttpclient-connection-pool-size-dilemma)
- [Connection Pool - OkHttp](https://stackoverflow.com/questions/63047533/connection-pool-okhttp)
- [OkHttp how to set maximum connection pool size](https://stackoverflow.com/questions/46206267/okhttp-how-to-set-maximum-connection-pool-size-not-max-idle-connections)
- [maxRequests and maxRequestsPerHost of Dispatcher · Issue #4220](https://github.com/square/okhttp/issues/4220)

### Ktor Documentation
- [Client engines | Ktor Documentation](https://ktor.io/docs/client-engines.html)
- [CIOEngineConfig | Ktor API](https://api.ktor.io/ktor-client/ktor-client-cio/io.ktor.client.engine.cio/-c-i-o-engine-config/index.html)

---

## Project Configuration Summary

### Test Parameters
- Number of concurrent requests: 100
- Server endpoint delay: 1 second
- Server: Ktor with Jetty engine
- Server thread pools:
  - connectionGroupSize: 2
  - workerGroupSize: 5
  - callGroupSize: 10

### Client Configuration (Final)

**OkHttp:**
```kotlin
fun newOkHttpClient(): HttpClient {
    return HttpClient(OkHttp) {
        engine {
            config {
                connectionPool(ConnectionPool(
                    maxIdleConnections = 100,
                    keepAliveDuration = 5,
                    timeUnit = TimeUnit.MINUTES,
                ))
                val dispatcher = Dispatcher(Executors.newCachedThreadPool()).apply {
                    maxRequests = 100
                    maxRequestsPerHost = 100  // KEY FIX
                }
                dispatcher(dispatcher)
            }
        }
        install(ContentNegotiation) {
            json(newJsonConfiguration())
        }
    }
}
```

**CIO:**
```kotlin
fun newCioClient(): HttpClient {
    return HttpClient(CIO) {
        install(ContentNegotiation) {
            json(newJsonConfiguration())
        }
    }
}
// No special configuration needed - works well out of the box
```

### Dependencies
- Ktor: 3.3.3
- Kotlin: 2.2.20
- Kotlin Serialization: 1.9.0

---

## Lessons Learned

1. **Default configurations matter**: Library defaults are often optimized for common use cases, not high-concurrency scenarios. Always review default limits when scaling.

2. **Connection pooling ≠ concurrency limits**: These are separate concerns that work at different layers.

3. **Abstractions can hide performance bottlenecks**: The suspend function abstraction made it non-obvious that OkHttp had internal queuing limits.

4. **Different engines, different trade-offs**:
   - OkHttp: More mature, HTTP/2 support, requires more configuration
   - CIO: Lightweight, coroutine-native, simpler configuration, HTTP/1.x only

5. **Testing reveals real-world behavior**: Performance testing under load uncovered configuration issues that wouldn't be apparent in single-request scenarios.
