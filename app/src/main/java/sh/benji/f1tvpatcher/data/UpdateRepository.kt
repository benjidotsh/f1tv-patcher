package sh.benji.f1tvpatcher.data

import android.content.Context
import androidx.core.content.edit

private const val KEY_LAST_CHECKED_AT = "lastCheckedAt"
private const val KEY_RELEASE_ETAG = "releaseEtag"
private const val KEY_RELEASE_JSON = "releaseJson"
private const val KEY_AWAITING_INSTALL_PERMISSION = "awaitingInstallPermission"

class UpdateRepository(context: Context) {
    private val prefs = context.getSharedPreferences("updates", Context.MODE_PRIVATE)

    var lastCheckedAt: Long
        get() = prefs.getLong(KEY_LAST_CHECKED_AT, 0L)
        set(value) = prefs.edit { putLong(KEY_LAST_CHECKED_AT, value) }

    var releaseEtag: String?
        get() = prefs.getString(KEY_RELEASE_ETAG, null)
        set(value) = prefs.edit { putString(KEY_RELEASE_ETAG, value) }

    var releaseJson: String?
        get() = prefs.getString(KEY_RELEASE_JSON, null)
        set(value) = prefs.edit { putString(KEY_RELEASE_JSON, value) }

    var awaitingInstallPermission: Boolean
        get() = prefs.getBoolean(KEY_AWAITING_INSTALL_PERMISSION, false)
        set(value) = prefs.edit { putBoolean(KEY_AWAITING_INSTALL_PERMISSION, value) }

    fun markChecked() {
        lastCheckedAt = System.currentTimeMillis()
    }

    fun saveReleaseCache(etag: String?, json: String) {
        prefs.edit {
            putString(KEY_RELEASE_ETAG, etag)
            putString(KEY_RELEASE_JSON, json)
        }
    }
}
