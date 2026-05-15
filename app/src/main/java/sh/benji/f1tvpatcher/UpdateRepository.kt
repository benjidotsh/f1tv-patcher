package sh.benji.f1tvpatcher

import android.content.Context
import androidx.core.content.edit

private const val KEY_LAST_RELEASE_TAG = "lastReleaseTag"
private const val KEY_LAST_RELEASE_TITLE = "lastReleaseTitle"
private const val KEY_LAST_CHECKED_AT = "lastCheckedAt"
private const val KEY_LAST_ERROR = "lastError"
private const val KEY_RELEASE_ETAG = "releaseEtag"
private const val KEY_RELEASE_JSON = "releaseJson"

class UpdateRepository(context: Context) {
    private val prefs = context.getSharedPreferences("updates", Context.MODE_PRIVATE)

    var lastReleaseTag: String?
        get() = prefs.getString(KEY_LAST_RELEASE_TAG, null)
        set(value) = prefs.edit { putString(KEY_LAST_RELEASE_TAG, value) }

    var lastReleaseTitle: String?
        get() = prefs.getString(KEY_LAST_RELEASE_TITLE, null)
        set(value) = prefs.edit { putString(KEY_LAST_RELEASE_TITLE, value) }

    var lastCheckedAt: Long
        get() = prefs.getLong(KEY_LAST_CHECKED_AT, 0L)
        set(value) = prefs.edit { putLong(KEY_LAST_CHECKED_AT, value) }

    var lastError: String?
        get() = prefs.getString(KEY_LAST_ERROR, null)
        set(value) = prefs.edit { putString(KEY_LAST_ERROR, value) }

    var releaseEtag: String?
        get() = prefs.getString(KEY_RELEASE_ETAG, null)
        set(value) = prefs.edit { putString(KEY_RELEASE_ETAG, value) }

    var releaseJson: String?
        get() = prefs.getString(KEY_RELEASE_JSON, null)
        set(value) = prefs.edit { putString(KEY_RELEASE_JSON, value) }

    fun recordRelease(release: ReleaseInfo) {
        prefs.edit {
            putString(KEY_LAST_RELEASE_TAG, release.tagName)
            putString(KEY_LAST_RELEASE_TITLE, release.title)
            putLong(KEY_LAST_CHECKED_AT, System.currentTimeMillis())
            remove(KEY_LAST_ERROR)
        }
    }

    fun saveReleaseCache(etag: String?, json: String) {
        prefs.edit {
            putString(KEY_RELEASE_ETAG, etag)
            putString(KEY_RELEASE_JSON, json)
        }
    }
}
