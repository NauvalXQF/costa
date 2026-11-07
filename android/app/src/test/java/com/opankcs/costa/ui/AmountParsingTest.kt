package com.opankcs.costa.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class AmountParsingTest {

    @Test
    fun plainNumbersAreParsedAsRupiah() {
        assertEquals(25_000L, parseAmount("25000") ?: -1L)
        assertEquals(25_000L, parseAmount("25.000") ?: -1L)
        assertEquals(25_000L, parseAmount("25,000") ?: -1L)
        assertEquals(1_250_000L, parseAmount("1.250.000") ?: -1L)
    }

    @Test
    fun integerShorthandIsMultiplied() {
        assertEquals(25_000L, parseAmount("25k") ?: -1L)
        assertEquals(25_000L, parseAmount("25K") ?: -1L)
        assertEquals(25_000L, parseAmount("25rb") ?: -1L)
        assertEquals(25_000L, parseAmount("25ribu") ?: -1L)
    }

    @Test
    fun decimalShorthandIsNotMultipliedTenTimes() {
        assertEquals(12_500L, parseAmount("12.5k") ?: -1L)
        assertEquals(12_500L, parseAmount("12,5k") ?: -1L)
        assertEquals(2_500L, parseAmount("2,5k") ?: -1L)
        assertEquals(1_500L, parseAmount("1.5rb") ?: -1L)
    }

    @Test
    fun invalidInputReturnsNull() {
        assertNull(parseAmount(""))
        assertNull(parseAmount("k"))
        assertNull(parseAmount("abc"))
        assertNull(parseAmount("rp"))
    }

    @Test
    fun editingTodayProducesIsoWhileAddingTodayKeepsNull() {
        val todayMillis = localDateToUtcMillis(LocalDate.now())

        assertNull(spentAtIsoFromMillis(todayMillis))
        assertNotNull(spentAtIsoForEdit(todayMillis))

        val yesterdayMillis = localDateToUtcMillis(LocalDate.now().minusDays(1))

        assertNotNull(spentAtIsoFromMillis(yesterdayMillis))
        assertNotNull(spentAtIsoForEdit(yesterdayMillis))
    }
}
