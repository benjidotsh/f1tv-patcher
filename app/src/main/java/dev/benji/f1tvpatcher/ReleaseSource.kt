package dev.benji.f1tvpatcher

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

class ReleaseSource(private val context: Context) {
    fun fetchLatestRelease(): ReleaseInfo {
        val json = URL(Constants.RELEASE_API).openConnection().let { connection ->
            (connection as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "F1-TV-Patcher")
                connectTimeout = 15_000
                readTimeout = 20_000
            }.inputStream.bufferedReader().use { it.readText() }
        }

        val root = JSONObject(json)
        val assetsJson = root.getJSONArray("assets")
        val assets = buildList {
            for (index in 0 until assetsJson.length()) {
                val asset = assetsJson.getJSONObject(index)
                add(
                    ReleaseAsset(
                        name = asset.getString("name"),
                        downloadUrl = asset.getString("browser_download_url"),
                        size = asset.optLong("size"),
                        digest = asset.optString("digest").ifBlank { null },
                    ),
                )
            }
        }

        val selected = ReleaseSelector.selectApkmAsset(assets)
            ?: error("Latest release does not contain a .apkm asset")

        return ReleaseInfo(
            tagName = root.getString("tag_name"),
            title = root.optString("name").ifBlank { root.getString("tag_name") },
            publishedAt = root.optString("published_at").ifBlank { null },
            asset = selected,
        )
    }

    fun download(release: ReleaseInfo): File {
        val releasesDir = File(context.cacheDir, "releases").apply { mkdirs() }
        val safeTag = release.tagName.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val target = File(releasesDir, "$safeTag-${release.asset.name}")
        if (target.isFile &&
            (release.asset.size <= 0L || target.length() == release.asset.size) &&
            target.matchesDigest(release.asset.digest)
        ) {
            return target
        }

        val temp = File(target.parentFile, "${target.name}.tmp")
        URL(release.asset.downloadUrl).openConnection().let { connection ->
            (connection as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", "F1-TV-Patcher")
                connectTimeout = 15_000
                readTimeout = 60_000
            }.inputStream.use { input ->
                temp.outputStream().use { output -> input.copyTo(output) }
            }
        }
        check(temp.matchesDigest(release.asset.digest)) {
            "Downloaded APKM does not match GitHub asset digest"
        }
        if (target.exists()) target.delete()
        check(temp.renameTo(target)) { "Could not move downloaded APKM into place" }
        return target
    }
}

private fun File.matchesDigest(digest: String?): Boolean {
    if (digest.isNullOrBlank()) return true
    val parts = digest.split(":", limit = 2)
    if (parts.size != 2 || !parts[0].equals("sha256", ignoreCase = true)) return true
    val expected = parts[1].lowercase()
    val actual = inputStream().use { input ->
        val messageDigest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            messageDigest.update(buffer, 0, read)
        }
        messageDigest.digest().joinToString("") { "%02x".format(it) }
    }
    return actual == expected
}
