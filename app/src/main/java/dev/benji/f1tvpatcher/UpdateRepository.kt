package dev.benji.f1tvpatcher

import android.content.Context
import androidx.core.content.edit

class UpdateRepository(context: Context) {
    private val prefs = context.getSharedPreferences("updates", Context.MODE_PRIVATE)

    var lastReleaseTag: String?
        get() = prefs.getString("lastReleaseTag", null)
        set(value) = prefs.edit { putString("lastReleaseTag", value) }

    var lastReleaseTitle: String?
        get() = prefs.getString("lastReleaseTitle", null)
        set(value) = prefs.edit { putString("lastReleaseTitle", value) }

    var lastCheckedAt: Long
        get() = prefs.getLong("lastCheckedAt", 0L)
        set(value) = prefs.edit { putLong("lastCheckedAt", value) }

    var pendingInstallTag: String?
        get() = prefs.getString("pendingInstallTag", null)
        set(value) = prefs.edit { putString("pendingInstallTag", value) }

    var lastError: String?
        get() = prefs.getString("lastError", null)
        set(value) = prefs.edit { putString("lastError", value) }

    fun recordRelease(release: ReleaseInfo) {
        prefs.edit {
            putString("lastReleaseTag", release.tagName)
            putString("lastReleaseTitle", release.title)
            putLong("lastCheckedAt", System.currentTimeMillis())
            remove("lastError")
        }
    }
}
