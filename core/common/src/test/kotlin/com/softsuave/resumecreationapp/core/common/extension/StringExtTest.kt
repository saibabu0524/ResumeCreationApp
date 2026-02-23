package com.softsuave.resumecreationapp.core.common.extension

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class StringExtTest {

    @Test
    fun `isValidEmail returns true for valid email`() {
        assertTrue("test@example.com".isValidEmail())
        assertTrue("user.name@domain.co.uk".isValidEmail())
        assertTrue("user+tag@example.com".isValidEmail())
    }

    @Test
    fun `isValidEmail returns false for invalid email`() {
        assertFalse("invalid-email".isValidEmail())
        assertFalse("@example.com".isValidEmail())
        assertFalse("user@".isValidEmail())
        assertFalse("".isValidEmail())
    }

    @Test
    fun `isValidUrl returns true for valid URL`() {
        assertTrue("https://example.com".isValidUrl())
        assertTrue("http://www.example.com".isValidUrl())
        assertTrue("https://example.com/path?query=value".isValidUrl())
    }

    @Test
    fun `isValidUrl returns false for invalid URL`() {
        assertFalse("not-a-url".isValidUrl())
        assertFalse("".isValidUrl())
    }

    @Test
    fun `isValidPhoneNumber returns true for valid phone numbers`() {
        assertTrue("+1234567890".isValidPhoneNumber())
        assertTrue("123-456-7890".isValidPhoneNumber())
        assertTrue("(123) 456-7890".isValidPhoneNumber())
    }

    @Test
    fun `toTitleCase converts string to title case`() {
        assertEquals("Hello World", "hello world".toTitleCase())
        assertEquals("Hello", "hello".toTitleCase())
        assertEquals("", "".toTitleCase())
    }

    @Test
    fun `truncate limits string length with ellipsis`() {
        assertEquals("Hello...", "Hello World".truncate(8))
        assertEquals("Hello", "Hello".truncate(10))
        assertEquals("", "".truncate(5))
    }

    @Test
    fun `truncate uses custom ellipsis`() {
        assertEquals("Hello---", "Hello World".truncate(8, "---"))
    }

    @Test
    fun `isNullOrBlank returns true for null or blank strings`() {
        assertTrue(null.isNullOrBlank())
        assertTrue("".isNullOrBlank())
        assertTrue("   ".isNullOrBlank())
        assertFalse("hello".isNullOrBlank())
    }

    @Test
    fun `orDefault returns default value for null or blank`() {
        assertEquals("default", null.orDefault("default"))
        assertEquals("default", "".orDefault("default"))
        assertEquals("hello", "hello".orDefault("default"))
    }
}
