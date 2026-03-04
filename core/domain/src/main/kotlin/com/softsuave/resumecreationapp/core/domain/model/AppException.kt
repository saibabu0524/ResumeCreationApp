package com.softsuave.resumecreationapp.core.domain.model

/**
 * Sealed hierarchy for all typed application exceptions.
 * Every error that surfaces to the UI must be expressed as one of these subtypes.
 * Raw JVM or framework exceptions must NEVER reach the presentation layer.
 */
sealed class AppException(
    override val message: String,
    override val cause: Throwable? = null,
) : Exception(message, cause) {

    // ─── Network / HTTP ──────────────────────────────────────────────────────

    /** HTTP 401 — the caller is not authenticated. */
    data class Unauthorized(
        override val message: String = "Unauthorized. Please sign in again.",
        override val cause: Throwable? = null,
    ) : AppException(message, cause)

    /** HTTP 403 — authenticated but not permitted. */
    data class Forbidden(
        override val message: String = "You don't have permission to perform this action.",
        override val cause: Throwable? = null,
    ) : AppException(message, cause)

    /** HTTP 404 — requested resource does not exist. */
    data class NotFound(
        override val message: String = "The requested resource was not found.",
        override val cause: Throwable? = null,
    ) : AppException(message, cause)

    /** HTTP 5xx — server-side failure. */
    data class ServerError(
        val code: Int = 500,
        override val message: String = "A server error occurred. Please try again later.",
        override val cause: Throwable? = null,
    ) : AppException(message, cause)

    /** Network call failed due to connectivity issues. */
    data class NetworkError(
        override val message: String = "A network error occurred. Please check your connection.",
        override val cause: Throwable? = null,
    ) : AppException(message, cause)

    /** Network call timed out. */
    data class Timeout(
        override val message: String = "The request timed out. Please try again.",
        override val cause: Throwable? = null,
    ) : AppException(message, cause)

    /** Device has no active internet connection. */
    data class NoInternet(
        override val message: String = "No internet connection. Please check your network settings.",
        override val cause: Throwable? = null,
    ) : AppException(message, cause)

    // ─── Validation ──────────────────────────────────────────────────────────

    /** One or more input fields failed validation. */
    data class ValidationError(
        val fields: Map<String, String> = emptyMap(),
        override val message: String = "Validation failed.",
        override val cause: Throwable? = null,
    ) : AppException(message, cause)

    // ─── HTTP Client Errors ───────────────────────────────────────────────────

    /** HTTP 400 — the request was malformed or validation failed server-side (e.g. inactive account). */
    data class BadRequest(
        override val message: String = "The request was invalid. Please check your input.",
        override val cause: Throwable? = null,
    ) : AppException(message, cause)

    /** HTTP 409 — a unique constraint was violated (e.g. duplicate email on register). */
    data class Conflict(
        override val message: String = "This resource already exists.",
        override val cause: Throwable? = null,
    ) : AppException(message, cause)

    /** HTTP 413 — the uploaded file exceeds the server size limit. */
    data class FileTooLarge(
        override val message: String = "The file is too large to upload.",
        override val cause: Throwable? = null,
    ) : AppException(message, cause)

    /** HTTP 415 — the file MIME type is not accepted by the server. */
    data class UnsupportedMediaType(
        override val message: String = "This file type is not supported.",
        override val cause: Throwable? = null,
    ) : AppException(message, cause)

    /** HTTP 429 — too many requests; the client is being rate-limited. */
    data class RateLimited(
        override val message: String = "Too many requests. Please wait a moment and try again.",
        override val cause: Throwable? = null,
    ) : AppException(message, cause)

    // ─── Auth ────────────────────────────────────────────────────────────────

    /** Attempt to access a resource before the user is authenticated. */
    data class NotAuthenticated(
        override val message: String = "You must be signed in to continue.",
        override val cause: Throwable? = null,
    ) : AppException(message, cause)

    /** Session has expired and the user must re-authenticate. */
    data class SessionExpired(
        override val message: String = "Your session has expired. Please sign in again.",
        override val cause: Throwable? = null,
    ) : AppException(message, cause)

    // ─── General ─────────────────────────────────────────────────────────────

    /** Catch-all for exceptions that don't fit a specific subtype. */
    data class Unknown(
        override val message: String = "An unexpected error occurred.",
        override val cause: Throwable? = null,
    ) : AppException(message, cause)
}
