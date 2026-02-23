package com.softsuave.resumecreationapp.core.common.util

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.Date

class DateTimeUtilTest {

    @Test
    fun `formatToIso8601 formats date correctly`() {
        val date = Date(1704067200000) // Jan 1, 2024
        val formatted = DateTimeUtil.formatToIso8601(date)

        assertTrue(formatted.matches(Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z")))
    }

    @Test
    fun `parseFromIso8601 parses valid date string`() {
        val dateString = "2024-01-01T00:00:00.000Z"
        val date = DateTimeUtil.parseFromIso8601(dateString)

        assertNotNull(date)
        assertEquals(1704067200000, date?.time)
    }

    @Test
    fun `parseFromIso8601 returns null for invalid date string`() {
        val date = DateTimeUtil.parseFromIso8601("invalid-date")
        assertNull(date)
    }

    @Test
    fun `formatToDisplayDate formats date correctly`() {
        val date = Date(1704067200000) // Jan 1, 2024
        val formatted = DateTimeUtil.formatToDisplayDate(date)

        assertEquals("Jan 01, 2024", formatted)
    }

    @Test
    fun `formatToDisplayTime formats time correctly`() {
        val date = Date(1704067200000) // Jan 1, 2024 00:00:00 UTC
        val formatted = DateTimeUtil.formatToDisplayTime(date)

        assertTrue(formatted.matches(Regex("\\d{2}:\\d{2}:\\d{2}")))
    }

    @Test
    fun `formatToDisplayDateTime formats date and time correctly`() {
        val date = Date(1704067200000) // Jan 1, 2024
        val formatted = DateTimeUtil.formatToDisplayDateTime(date)

        assertTrue(formatted.startsWith("Jan 01, 2024"))
    }

    @Test
    fun `getCurrentTimeMillis returns current time`() {
        val before = System.currentTimeMillis()
        val current = DateTimeUtil.getCurrentTimeMillis()
        val after = System.currentTimeMillis()

        assertTrue(current in before..after)
    }

    @Test
    fun `isOlderThan returns true for old timestamp`() {
        val oldTimestamp = System.currentTimeMillis() - 10000 // 10 seconds ago
        assertTrue(DateTimeUtil.isOlderThan(oldTimestamp, 5000))
    }

    @Test
    fun `isOlderThan returns false for recent timestamp`() {
        val recentTimestamp = System.currentTimeMillis() - 1000 // 1 second ago
        assertFalse(DateTimeUtil.isOlderThan(recentTimestamp, 5000))
    }

    @Test
    fun `isWithin returns true for recent timestamp`() {
        val recentTimestamp = System.currentTimeMillis() - 1000 // 1 second ago
        assertTrue(DateTimeUtil.isWithin(recentTimestamp, 5000))
    }

    @Test
    fun `isWithin returns false for old timestamp`() {
        val oldTimestamp = System.currentTimeMillis() - 10000 // 10 seconds ago
        assertFalse(DateTimeUtil.isWithin(oldTimestamp, 5000))
    }

    @Test
    fun `getRelativeTimeString returns correct strings`() {
        val now = System.currentTimeMillis()

        assertEquals("Just now", DateTimeUtil.getRelativeTimeString(now - 5000))
        assertEquals("30 seconds ago", DateTimeUtil.getRelativeTimeString(now - 30000))
        assertEquals("5 minutes ago", DateTimeUtil.getRelativeTimeString(now - 300000))
        assertEquals("2 hours ago", DateTimeUtil.getRelativeTimeString(now - 7200000))
        assertEquals("Yesterday", DateTimeUtil.getRelativeTimeString(now - 86400000))
        assertEquals("2 days ago", DateTimeUtil.getRelativeTimeString(now - 172800000))
    }
}
