package com.softsuave.resumecreationapp.core.common.extension

/**
 * Extension functions for String operations
 */

/**
 * Checks if the string is a valid email address
 */
fun String.isValidEmail(): Boolean =
    android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()

/**
 * Checks if the string is a valid URL
 */
fun String.isValidUrl(): Boolean =
    android.util.Patterns.WEB_URL.matcher(this).matches()

/**
 * Checks if the string is a valid phone number
 */
fun String.isValidPhoneNumber(): Boolean =
    android.util.Patterns.PHONE.matcher(this).matches()

/**
 * Converts string to title case (first letter of each word capitalized)
 */
fun String.toTitleCase(): String =
    split(" ").joinToString(" ") { word ->
        word.replaceFirstChar { it.uppercase() }
    }

/**
 * Truncates string to specified length with ellipsis
 */
fun String.truncate(maxLength: Int, ellipsis: String = "..."): String =
    if (length <= maxLength) this
    else take(maxLength - ellipsis.length) + ellipsis

/**
 * Checks if string is null or blank
 */
fun String?.isNullOrBlank(): Boolean = this?.isBlank() ?: true

/**
 * Returns string if not null/blank, otherwise returns default value
 */
fun String?.orDefault(default: String = ""): String = if (this.isNullOrBlank()) default else this!!!!
