package com.softsuave.resumecreationapp.core.common.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.PromptInfo
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * Manager for handling biometric authentication with all edge cases
 */
class BiometricPromptManager(
    private val context: Context
) {
    private val resultChannel = Channel<BiometricResult>()
    val biometricResults: Flow<BiometricResult> = resultChannel.receiveAsFlow()

    /**
     * Checks if biometric authentication is available and ready to use
     */
    fun canAuthenticate(): BiometricStatus {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.Ready
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.ErrorNoHardware
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricStatus.ErrorHwUnavailable
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.ErrorNoneEnrolled
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> BiometricStatus.ErrorSecurityUpdateRequired
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> BiometricStatus.ErrorUnsupported
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> BiometricStatus.ErrorUnknown
            else -> BiometricStatus.ErrorUnknown
        }
    }

    /**
     * Checks if biometric authentication with weak biometrics is available
     */
    fun canAuthenticateWithWeakBiometrics(): BiometricStatus {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.Ready
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.ErrorNoHardware
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricStatus.ErrorHwUnavailable
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.ErrorNoneEnrolled
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> BiometricStatus.ErrorSecurityUpdateRequired
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> BiometricStatus.ErrorUnsupported
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> BiometricStatus.ErrorUnknown
            else -> BiometricStatus.ErrorUnknown
        }
    }

    /**
     * Shows the biometric prompt for authentication
     */
    fun showBiometricPrompt(
        activity: FragmentActivity,
        title: String,
        subtitle: String? = null,
        description: String? = null,
        negativeButtonText: String = "Cancel",
        confirmationRequired: Boolean = true
    ) {
        val promptInfo = PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setDescription(description)
            .setNegativeButtonText(negativeButtonText)
            .setConfirmationRequired(confirmationRequired)
            .build()

        val biometricPrompt = BiometricPrompt(
            activity,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    resultChannel.trySend(BiometricResult.AuthenticationSuccess)
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    resultChannel.trySend(BiometricResult.AuthenticationFailed)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    val errorType = when (errorCode) {
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON -> BiometricResult.BiometricAuthErrorType.NegativeButton
                        BiometricPrompt.ERROR_USER_CANCELED -> BiometricResult.BiometricAuthErrorType.UserCanceled
                        BiometricPrompt.ERROR_TIMEOUT -> BiometricResult.BiometricAuthErrorType.Timeout
                        BiometricPrompt.ERROR_LOCKOUT -> BiometricResult.BiometricAuthErrorType.Lockout
                        BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> BiometricResult.BiometricAuthErrorType.LockoutPermanent
                        BiometricPrompt.ERROR_CANCELED -> BiometricResult.BiometricAuthErrorType.Canceled
                        BiometricPrompt.ERROR_HW_UNAVAILABLE -> BiometricResult.BiometricAuthErrorType.HwUnavailable
                        BiometricPrompt.ERROR_HW_NOT_PRESENT -> BiometricResult.BiometricAuthErrorType.HwNotPresent
                        BiometricPrompt.ERROR_NO_SPACE -> BiometricResult.BiometricAuthErrorType.NoSpace
                        BiometricPrompt.ERROR_UNABLE_TO_PROCESS -> BiometricResult.BiometricAuthErrorType.UnableToProcess
                        BiometricPrompt.ERROR_NO_BIOMETRICS -> BiometricResult.BiometricAuthErrorType.NoBiometrics
                        else -> BiometricResult.BiometricAuthErrorType.Unknown(errorCode, errString.toString())
                    }
                    resultChannel.trySend(BiometricResult.AuthenticationError(errorType))
                }
            }
        )

        biometricPrompt.authenticate(promptInfo)
    }

    /**
     * Closes the result channel
     */
    fun close() {
        resultChannel.close()
    }
}

/**
 * Status of biometric authentication availability
 */
sealed class BiometricStatus {
    data object Ready : BiometricStatus()
    data object ErrorNoHardware : BiometricStatus()
    data object ErrorHwUnavailable : BiometricStatus()
    data object ErrorNoneEnrolled : BiometricStatus()
    data object ErrorSecurityUpdateRequired : BiometricStatus()
    data object ErrorUnsupported : BiometricStatus()
    data object ErrorUnknown : BiometricStatus()
}

/**
 * Result of biometric authentication attempt
 */
sealed class BiometricResult {
    data object AuthenticationSuccess : BiometricResult()
    data object AuthenticationFailed : BiometricResult()
    data class AuthenticationError(val error: BiometricAuthErrorType) : BiometricResult()

    sealed class BiometricAuthErrorType {
        data object NegativeButton : BiometricAuthErrorType()
        data object UserCanceled : BiometricAuthErrorType()
        data object Timeout : BiometricAuthErrorType()
        data object Lockout : BiometricAuthErrorType()
        data object LockoutPermanent : BiometricAuthErrorType()
        data object Canceled : BiometricAuthErrorType()
        data object HwUnavailable : BiometricAuthErrorType()
        data object HwNotPresent : BiometricAuthErrorType()
        data object NoSpace : BiometricAuthErrorType()
        data object UnableToProcess : BiometricAuthErrorType()
        data object NoBiometrics : BiometricAuthErrorType()
        data class Unknown(val errorCode: Int, val message: String) : BiometricAuthErrorType()
    }
}
