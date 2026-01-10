## Цели тестирования
Определить показатели производительности различных конфигураций HTTP-клиента и диспатчеров корутин для того, что бы понять как количество потоков влияет на пропускную способность, задержку и использование ресурсов при параллельном вызове HTTP API для приложений Андроид.
  
1. Пропускная способность 
   1. сколько вызовов ручки может обрабатывать OkHttp клиент одновременно
2. Задержка
   1. время передачи запроса в обработку
   2. время начала выполнения запроса
   3. время окончания обработки запроса
   4. время ожидания (min, max, avg) - на основе времени и конфигурации количества потоков можно строить характеристику эффективности утилизации потоков 
   5. время выполнения всех запросов (min, max, avg) - сравнение конфигураций в целом
3. Использование ресурсов
   1. количество потоков для выполнения одного запроса к серверу


## Test stand description
Have test in which are provided following things:
1. Server with echo endpoint
2. Configured OkHttpClient
3. Factory of api which creates api which uses OkHttp client
4. Configured dispatcher
5. Logger or Stub logger
6. Request factory
7. Number N of parallel calls to server

Test do following:
1. Creates api
2. Creates request
3. Launches N coroutines which makes calls to api and waits for server response

## Data Structures

### Test configuration
1. Logging enabled/disabled
2. OkHttp threads count
3. Runner threads count(to configure dispatcher) to call retrofit calls

### Test measurements 
1. Overall test time
2. Minimal call to the server time
3. Maximum call to the server time
4. Average call to the server time
5. Call data:
   1. Thread name
   2. Call start time
   3. Call end time

