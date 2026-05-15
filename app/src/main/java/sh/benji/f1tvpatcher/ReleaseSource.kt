package sh.benji.f1tvpatcher

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.DigestOutputStream
import java.security.MessageDigest

class GithubHttpException(
    val status: Int,
    val rateLimitResetEpoch: Long?,
    message: String,
) : IOException(message)

class ReleaseSource(private val context: Context) {
    fun fetchLatestRelease(): ReleaseInfo {
        val repo = UpdateRepository(context)
        val cachedJson = repo.releaseJson
        val cachedEtag = if (cachedJson != null) repo.releaseEtag else null

        val connection = (URL(Constants.RELEASE_API).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "F1-TV-Patcher")
            if (cachedEtag != null) setRequestProperty("If-None-Match", cachedEtag)
            connectTimeout = 15_000
            readTimeout = 20_000
        }

        val json = try {
            when (val code = connection.responseCode) {
                200 -> {
                    val body = connection.inputStream.bufferedReader().use { it.readText() }
                    repo.saveReleaseCache(connection.getHeaderField("ETag"), body)
                    body
                }
                304 -> cachedJson
                    ?: throw IOException("304 from GitHub but no cached release available")
                else -> {
                    val errBody = (connection.errorStream ?: connection.inputStream)
                        ?.bufferedReader()?.use { it.readText() } ?: ""
                    val parsed = runCatching { JSONObject(errBody).optString("message") }
                        .getOrNull()?.ifBlank { null }
                        ?.substringBefore(" (")
                    val display = parsed
                        ?: errBody.take(240).ifBlank { connection.responseMessage ?: "HTTP $code" }
                    val reset = connection.getHeaderField("X-RateLimit-Reset")?.toLongOrNull()
                    throw GithubHttpException(code, reset, display)
                }
            }
        } finally {
            connection.disconnect()
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
        val target = File(releasesDir, "${release.tagName.safeFileName()}-${release.asset.name}")
        if (target.isFile &&
            (release.asset.size <= 0L || target.length() == release.asset.size) &&
            target.matchesDigest(release.asset.digest)
        ) {
            return target
        }

        val temp = File(target.parentFile, "${target.name}.tmp")
        val expected = parseSha256Digest(release.asset.digest)
        val md = expected?.let { MessageDigest.getInstance("SHA-256") }
        (URL(release.asset.downloadUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("User-Agent", "F1-TV-Patcher")
            connectTimeout = 15_000
            readTimeout = 60_000
        }.inputStream.use { input ->
            val raw = temp.outputStream()
            val out = if (md != null) DigestOutputStream(raw, md) else raw
            out.use { input.copyTo(it) }
        }
        if (md != null) {
            val actual = md.digest().joinToString("") { "%02x".format(it) }
            check(actual == expected) {
                "Downloaded APKM does not match GitHub asset digest"
            }
        }
        Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        return target
    }
}

private fun parseSha256Digest(digest: String?): String? {
    if (digest.isNullOrBlank()) return null
    val parts = digest.split(":", limit = 2)
    if (parts.size != 2 || !parts[0].equals("sha256", ignoreCase = true)) return null
    return parts[1].lowercase()
}

private fun File.matchesDigest(digest: String?): Boolean {
    val expected = parseSha256Digest(digest) ?: return true
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
