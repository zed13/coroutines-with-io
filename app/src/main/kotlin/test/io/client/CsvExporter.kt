package test.io.client

import java.io.File

object CsvExporter {

    fun exportCallsData(file: File, data: List<CallData>) {
        val header = "call_id, enqueued_at, started_at, completed_at, waiting_time_ms, exec_time_ms, total_time_ms"
        val body = data.asSequence().map {
            "${it.callId}, ${it.enqueuedAt}, ${it.startedAt}, ${it.completedAt}, ${it.waitingTimeMillis}, ${it.executionTimeMillis}, ${it.totalTimeMillis}"
        }
        file.bufferedWriter().use { writer ->
            (sequenceOf(header) + body).forEach { row ->
                writer.write(row)
                writer.write("\n")
            }
        }
    }
}