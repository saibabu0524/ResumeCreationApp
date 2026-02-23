package com.softsuave.resumecreationapp.shortcuts

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.softsuave.resumecreationapp.MainActivity
import timber.log.Timber

/**
 * Helper for managing static and dynamic app shortcuts.
 *
 * Static shortcuts are defined in `res/xml/shortcuts.xml` and registered
 * automatically by the system.
 *
 * Dynamic shortcuts are added/updated programmatically via this helper.
 *
 * Usage:
 * ```kotlin
 * ShortcutHelper.addDynamicShortcut(context, "profile", "My Profile", R.drawable.ic_person)
 * ```
 */
object ShortcutHelper {

    /**
     * Adds or updates a dynamic shortcut.
     *
     * @param context Application context.
     * @param shortcutId Unique shortcut identifier.
     * @param label User-visible shortcut label.
     * @param iconResId Resource ID for the shortcut icon.
     * @param intentAction Intent action for the shortcut.
     */
    fun addDynamicShortcut(
        context: Context,
        shortcutId: String,
        label: String,
        iconResId: Int,
        intentAction: String = Intent.ACTION_VIEW,
    ) {
        val shortcut = ShortcutInfoCompat.Builder(context, shortcutId)
            .setShortLabel(label)
            .setIcon(IconCompat.createWithResource(context, iconResId))
            .setIntent(
                Intent(context, MainActivity::class.java).apply {
                    action = intentAction
                    putExtra(EXTRA_SHORTCUT_ID, shortcutId)
                },
            )
            .build()

        ShortcutManagerCompat.pushDynamicShortcut(context, shortcut)
        Timber.d("ShortcutHelper: added dynamic shortcut id=$shortcutId")
    }

    /**
     * Removes a dynamic shortcut by ID.
     */
    fun removeDynamicShortcut(context: Context, shortcutId: String) {
        ShortcutManagerCompat.removeDynamicShortcuts(context, listOf(shortcutId))
        Timber.d("ShortcutHelper: removed dynamic shortcut id=$shortcutId")
    }

    /**
     * Removes all dynamic shortcuts.
     */
    fun removeAllDynamicShortcuts(context: Context) {
        ShortcutManagerCompat.removeAllDynamicShortcuts(context)
        Timber.d("ShortcutHelper: removed all dynamic shortcuts")
    }

    const val EXTRA_SHORTCUT_ID = "shortcut_id"
}
