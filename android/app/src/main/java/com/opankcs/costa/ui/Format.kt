package com.opankcs.costa.ui

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val rupiahFormat = DecimalFormat("#,##0", DecimalFormatSymbols(Locale("id", "ID")))

fun formatRupiah(value: Long?): String = "Rp" + rupiahFormat.format(value ?: 0L)

fun parseAmount(input: String): Long? {
    var text = input.lowercase().trim().replace("rp", "").replace(" ", "")
    if (text.isEmpty()) return null

    val multiplier = when {
        text.endsWith("ribu") -> 1000L
        text.endsWith("rb") -> 1000L
        text.endsWith("k") -> 1000L
        else -> 1L
    }

    text = text.removeSuffix("ribu").removeSuffix("rb").removeSuffix("k")
    if (text.isEmpty()) return null

    val separatorCount = text.count { it == '.' || it == ',' }

    if (separatorCount == 0) {
        return text.toLongOrNull()?.times(multiplier)
    }

    if (multiplier > 1L) {
        // Shorthand like "12.5k" / "2,5k": treat the last separator as decimal
        val normalized = text.replace(',', '.')
        val parts = normalized.split('.')

        if (parts.size == 2 && parts[0].isNotEmpty() && parts[1].length in 1..2) {
            normalized.toDoubleOrNull()?.let { value ->
                return (value * multiplier).toLong()
            }
        }

        val digits = normalized.replace(".", "")
        return digits.toLongOrNull()?.times(multiplier)
    }

    return text.replace(".", "").replace(",", "").toLongOrNull()
}

fun parseSpentAt(value: String?): LocalDateTime? = try {
    if (value.isNullOrBlank()) {
        null
    } else {
        OffsetDateTime.parse(value).toLocalDateTime()
    }
} catch (_: Exception) {
    null
}

fun formatTime(value: String?): String {
    val dateTime = parseSpentAt(value) ?: return ""
    return dateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
}

fun formatDateLabel(date: LocalDate?): String {
    if (date == null) return "Tanpa tanggal"
    val today = LocalDate.now()
    return when (date) {
        today -> "Hari ini"
        today.minusDays(1) -> "Kemarin"
        else -> date.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale("id", "ID")))
    }
}

fun utcMillisToLocalDate(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

fun localDateToUtcMillis(date: LocalDate): Long =
    date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

fun spentAtIsoFromMillis(millis: Long?): String? {
    if (millis == null) return null
    val date = utcMillisToLocalDate(millis)
    if (date == LocalDate.now()) return null
    return date
        .atTime(LocalTime.NOON)
        .atZone(ZoneId.systemDefault())
        .toOffsetDateTime()
        .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
}

fun spentAtIsoForEdit(millis: Long?): String? {
    if (millis == null) return null

    val date = utcMillisToLocalDate(millis)
    val time = if (date == LocalDate.now()) LocalTime.now() else LocalTime.NOON

    return date
        .atTime(time)
        .atZone(ZoneId.systemDefault())
        .toOffsetDateTime()
        .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
}

fun formatDateMillisLabel(millis: Long?): String =
    if (millis == null) "Hari ini" else formatDateLabel(utcMillisToLocalDate(millis))

fun dateMillisFromIso(value: String?): Long? {
    val date = parseSpentAt(value)?.toLocalDate() ?: return null
    return localDateToUtcMillis(date)
}
fun formatDateShort(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("d MMM", Locale("id", "ID")))

fun formatDateLong(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale("id", "ID")))

fun localDateFromIsoDate(value: String?): LocalDate? = try {
    if (value.isNullOrBlank()) null else LocalDate.parse(value)
} catch (_: Exception) {
    null
}