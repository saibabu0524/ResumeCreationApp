package com.softsuave.resumecreationapp.core.common.security

import android.os.Build

/**
 * Interface for checking device security status
 */
interface DeviceSecurityChecker {
    /**
     * Checks if the device is rooted
     */
    fun isDeviceRooted(): Boolean

    /**
     * Checks if developer options are enabled
     */
    fun isDeveloperOptionsEnabled(): Boolean

    /**
     * Checks if the app is debuggable
     */
    fun isDebuggable(): Boolean

    /**
     * Checks if the device passes all security checks
     */
    fun isDeviceSecure(): Boolean =
        !isDeviceRooted() && !isDeveloperOptionsEnabled() && !isDebuggable()
}

/**
 * Default implementation of DeviceSecurityChecker
 */
class DeviceSecurityCheckerImpl(
    private val buildInfo: BuildInfo = BuildInfo(),
    private val systemProperties: Map<String, String> = System.getProperties()
        .mapKeys { it.key.toString() }
        .mapValues { it.value.toString() }
) : DeviceSecurityChecker {

    override fun isDeviceRooted(): Boolean {
        return checkRootMethod1() || checkRootMethod2() || checkRootMethod3()
    }

    override fun isDeveloperOptionsEnabled(): Boolean {
        return android.provider.Settings.Global.getInt(
            buildInfo.contentResolver,
            android.provider.Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
            0
        ) != 0
    }

    override fun isDebuggable(): Boolean {
        return buildInfo.isDebuggable
    }

    private fun checkRootMethod1(): Boolean {
        val buildTags = buildInfo.buildTags
        return buildTags != null && buildTags.contains("test-keys")
    }

    private fun checkRootMethod2(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su",
            "/su/bin",
            "/system/xbin/daemonsu"
        )
        return paths.any { path ->
            try {
                java.io.File(path).exists()
            } catch (e: Exception) {
                false
            }
        }
    }

    private fun checkRootMethod3(): Boolean {
        var process: java.lang.Process? = null
        return try {
            process = Runtime.getRuntime().exec(arrayOf("/system/xbin/which", "su"))
            val bufferedReader = java.io.BufferedReader(
                java.io.InputStreamReader(process.inputStream)
            )
            bufferedReader.readLine() != null
        } catch (e: Exception) {
            false
        } finally {
            process?.destroy()
        }
    }
}

/**
 * Wrapper for Build information to enable testing
 */
data class BuildInfo(
    val buildTags: String? = Build.TAGS,
    val contentResolver: android.content.ContentResolver? = null,
    val isDebuggable: Boolean = false
)
