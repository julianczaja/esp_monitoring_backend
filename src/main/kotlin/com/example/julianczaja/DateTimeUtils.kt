package com.example.julianczaja

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField


// LocalDateTime
private val dateTimeFormatter: DateTimeFormatter = DateTimeFormatterBuilder()
    .appendValue(ChronoField.YEAR_OF_ERA, 4)
    .appendValue(ChronoField.MONTH_OF_YEAR, 2)
    .appendValue(ChronoField.DAY_OF_MONTH, 2)
    .appendValue(ChronoField.HOUR_OF_DAY, 2)
    .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
    .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
    .appendValue(ChronoField.MILLI_OF_SECOND, 3)
    .toFormatter()

private val currentDateTime get() = LocalDateTime.now(ZoneId.of("Europe/Warsaw"))

val currentDateTimeString: String get() = dateTimeFormatter.format(currentDateTime)

fun String.toLocalDateTime(): LocalDateTime = LocalDateTime.parse(this, dateTimeFormatter)

fun LocalDateTime.toDefaultString(): String = this.format(dateTimeFormatter)

// LocalDate
private val dateFormatter: DateTimeFormatter = DateTimeFormatterBuilder()
    .appendValue(ChronoField.YEAR_OF_ERA, 4)
    .appendValue(ChronoField.MONTH_OF_YEAR, 2)
    .appendValue(ChronoField.DAY_OF_MONTH, 2)
    .toFormatter()

val currentDateString: String get() = dateFormatter.format(currentDateTime)

fun String.toLocalDate(): LocalDate = LocalDate.parse(this, dateFormatter)

fun String.toLocalDateOrNull() = try {
    this.toLocalDate()
} catch (e: Exception) {
    null
}

fun LocalDate.toDefaultString(): String = this.format(dateFormatter)
