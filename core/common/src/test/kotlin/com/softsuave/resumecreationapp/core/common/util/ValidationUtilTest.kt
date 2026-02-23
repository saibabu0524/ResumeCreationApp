package com.softsuave.resumecreationapp.core.common.util

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ValidationUtilTest {

    @Test
    fun `isValidEmail returns true for valid email`() {
        assertTrue(ValidationUtil.isValidEmail("test@example.com"))
        assertTrue(ValidationUtil.isValidEmail("user.name@domain.co.uk"))
    }

    @Test
    fun `isValidEmail returns false for invalid email`() {
        assertFalse(ValidationUtil.isValidEmail("invalid-email"))
        assertFalse(ValidationUtil.isValidEmail(""))
        assertFalse(ValidationUtil.isValidEmail(null))
    }

    @Test
    fun `isValidPhoneNumber returns true for valid phone numbers`() {
        assertTrue(ValidationUtil.isValidPhoneNumber("+1234567890"))
        assertTrue(ValidationUtil.isValidPhoneNumber("123-456-7890"))
    }

    @Test
    fun `isValidPhoneNumber returns false for invalid phone numbers`() {
        assertFalse(ValidationUtil.isValidPhoneNumber(""))
        assertFalse(ValidationUtil.isValidPhoneNumber(null))
    }

    @Test
    fun `isValidUrl returns true for valid URL`() {
        assertTrue(ValidationUtil.isValidUrl("https://example.com"))
        assertTrue(ValidationUtil.isValidUrl("http://www.example.com"))
    }

    @Test
    fun `isValidUrl returns false for invalid URL`() {
        assertFalse(ValidationUtil.isValidUrl("not-a-url"))
        assertFalse(ValidationUtil.isValidUrl(""))
        assertFalse(ValidationUtil.isValidUrl(null))
    }

    @Test
    fun `isValidPassword returns true for strong password`() {
        assertTrue(ValidationUtil.isValidPassword("Password123"))
        assertTrue(ValidationUtil.isValidPassword("Str0ngP@ss"))
    }

    @Test
    fun `isValidPassword returns false for weak password`() {
        assertFalse(ValidationUtil.isValidPassword("weak"))
        assertFalse(ValidationUtil.isValidPassword("12345678"))
        assertFalse(ValidationUtil.isValidPassword("password"))
        assertFalse(ValidationUtil.isValidPassword("PASSWORD"))
        assertFalse(ValidationUtil.isValidPassword(""))
        assertFalse(ValidationUtil.isValidPassword(null))
    }

    @Test
    fun `doStringsMatch returns true when strings match`() {
        assertTrue(ValidationUtil.doStringsMatch("hello", "hello"))
    }

    @Test
    fun `doStringsMatch returns false when strings don't match`() {
        assertFalse(ValidationUtil.doStringsMatch("hello", "world"))
        assertFalse(ValidationUtil.doStringsMatch("hello", ""))
        assertFalse(ValidationUtil.doStringsMatch("hello", null))
        assertFalse(ValidationUtil.doStringsMatch(null, null))
    }

    @Test
    fun `isValidUsername returns true for valid username`() {
        assertTrue(ValidationUtil.isValidUsername("user123"))
        assertTrue(ValidationUtil.isValidUsername("john_doe"))
        assertTrue(ValidationUtil.isValidUsername("a"))
    }

    @Test
    fun `isValidUsername returns false for invalid username`() {
        assertFalse(ValidationUtil.isValidUsername("us"))
        assertFalse(ValidationUtil.isValidUsername("user@name"))
        assertFalse(ValidationUtil.isValidUsername("user name"))
        assertFalse(ValidationUtil.isValidUsername(""))
        assertFalse(ValidationUtil.isValidUsername(null))
    }

    @Test
    fun `isValidCreditCardNumber returns true for valid card numbers`() {
        // Test with valid Luhn numbers
        assertTrue(ValidationUtil.isValidCreditCardNumber("4532015112830366")) // Visa
        assertTrue(ValidationUtil.isValidCreditCardNumber("5555555555554444")) // Mastercard
    }

    @Test
    fun `isValidCreditCardNumber returns false for invalid card numbers`() {
        assertFalse(ValidationUtil.isValidCreditCardNumber("1234567890123456"))
        assertFalse(ValidationUtil.isValidCreditCardNumber(""))
        assertFalse(ValidationUtil.isValidCreditCardNumber(null))
        assertFalse(ValidationUtil.isValidCreditCardNumber("123"))
    }

    @Test
    fun `isValidCreditCardNumber strips non-digit characters`() {
        assertTrue(ValidationUtil.isValidCreditCardNumber("4532 0151 1283 0366"))
        assertTrue(ValidationUtil.isValidCreditCardNumber("4532-0151-1283-0366"))
    }
}
