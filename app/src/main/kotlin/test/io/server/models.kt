package test.io.server

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalTime

@Serializable
class DateTime(
    val date: String = LocalDate.now().toString(),
    val time: String = LocalTime.now().toString(),
)