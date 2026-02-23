package com.softsuave.resumecreationapp.core.common.util

import com.softsuave.resumecreationapp.core.common.extension.isValidEmail
import com.softsuave.resumecreationapp.core.common.extension.isValidPhoneNumber
import com.softsuave.resumecreationapp.core.common.extension.isValidUrl

/**
 * Utility class for validation operations
 */
object ValidationUtil {

    /**
     * Validates an email address
     */
    fun isValidEmail(email: String?): Boolean {
        return !email.isNullOrBlank() && email.isValidEmail()
    }

    /**
     * Validates a phone number
     */
    fun isValidPhoneNumber(phone: String?): Boolean {
        return !phone.isNullOrBlank() && phone.isValidPhoneNumber()
    }

    /**
     * Validates a URL
     */
    fun isValidUrl(url: String?): Boolean {
        return !url.isNullOrBlank() && url.isValidUrl()
    }

    /**
     * Validates password strength
     * Returns true if password meets minimum requirements
     */
    fun isValidPassword(password: String?): Boolean {
        if (password.isNullOrBlank()) return false

        // Minimum 8 characters, at least one uppercase, one lowercase, one digit
        val passwordRegex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$".toRegex()
        return password.matches(passwordRegex)
    }

    /**
     * Validates if two strings match (e.g., password confirmation)
     */
    fun doStringsMatch(str1: String?, str2: String?): Boolean {
        return str1 == str2 && !str1.isNullOrBlank()
    }

    /**
     * Validates a username
     * Returns true if username is valid (3-20 characters, alphanumeric with underscores)
     */
    fun isValidUsername(username: String?): Boolean {
        if (username.isNullOrBlank()) return false

        val usernameRegex = "^[a-zA-Z0-9_]{3,20}$".toRegex()
        return username.matches(usernameRegex)
    }

    /**
     * Validates a credit card number using Luhn algorithm
     */
    fun isValidCreditCardNumber(cardNumber: String?): Boolean {
        if (cardNumber.isNullOrBlank()) return false

        val digits = cardNumber.replace("\\D".toRegex(), "")
        if (digits.length < 13 || digits.length > 19) return false

        var sum = 0
        var isAlternate = false

        for (i in digits.length - 1 downTo 0) {
            var digit = digits[i].toString().toInt()

            if (isAlternate) {
                digit *= 2
                if (digit > 9) {
                    digit = (digit % 10) + (digit / 10)
                }
            }

            sum += digit
            isAlternate = !isAlternate
        }

        return sum % 10 == 0
    }
}
