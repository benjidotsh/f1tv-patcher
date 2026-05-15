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

    var lastError: String?
        get() = prefs.getString("lastError", null)
        set(value) = prefs.edit { putString("lastError", value) }

    var releaseEtag: String?
        get() = prefs.getString("releaseEtag", null)
        set(value) = prefs.edit { putString("releaseEtag", value) }

    var releaseJson: String?
        get() = prefs.getString("releaseJson", null)
        set(value) = prefs.edit { putString("releaseJson", value) }

    fun recordRelease(release: ReleaseInfo) {
        prefs.edit {
            putString("lastReleaseTag", release.tagName)
            putString("lastReleaseTitle", release.title)
            remove("lastError")
        }
    }
}
