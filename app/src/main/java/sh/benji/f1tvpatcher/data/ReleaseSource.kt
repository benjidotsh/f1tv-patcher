package sh.benji.f1tvpatcher.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import sh.benji.f1tvpatcher.Constants
import sh.benji.f1tvpatcher.domain.ReleaseAsset
import sh.benji.f1tvpatcher.domain.ReleaseInfo
import sh.benji.f1tvpatcher.domain.ReleaseSelector
import sh.benji.f1tvpatcher.domain.safeFileName
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.DigestOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class GithubHttpException(
    val status: Int,
    val rateLimitResetEpoch: Long?,
    message: String,
) : IOException(message)

class ReleaseSource(private val context: Context) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun fetchLatestRelease(): ReleaseInfo = withContext(Dispatchers.IO) {
        val repo = UpdateRepository(context)
        val cachedJson = repo.releaseJson
        val cachedEtag = if (cachedJson != null) repo.releaseEtag else null

        val request = Request.Builder()
            .url(Constants.RELEASE_API)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "F1-TV-Patcher")
            .apply { if (cachedEtag != null) header("If-None-Match", cachedEtag) }
            .build()

        val json = client.newCall(request).execute().use { response ->
            when (response.code) {
                200 -> {
                    val body = response.body?.string()
                        ?: throw IOException("Empty body from GitHub")
                    repo.saveReleaseCache(response.header("ETag"), body)
                    body
                }
                304 -> cachedJson
                    ?: throw IOException("304 from GitHub but no cached release available")
                else -> throw response.toGithubError()
            }
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

        ReleaseInfo(
            tagName = root.getString("tag_name"),
            title = root.optString("name").ifBlank { root.getString("tag_name") },
            publishedAt = root.optString("published_at").ifBlank { null },
            asset = selected,
        )
    }

    suspend fun download(release: ReleaseInfo): File = withContext(Dispatchers.IO) {
        val releasesDir = File(context.cacheDir, "releases").apply { mkdirs() }
        val target = File(releasesDir, "${release.tagName.safeFileName()}-${release.asset.name}")
        if (target.isFile &&
            (release.asset.size <= 0L || target.length() == release.asset.size) &&
            target.matchesDigest(release.asset.digest)
        ) {
            return@withContext target
        }

        val temp = File(target.parentFile, "${target.name}.tmp")
        val expected = parseSha256Digest(release.asset.digest)
        val md = expected?.let { MessageDigest.getInstance("SHA-256") }

        val request = Request.Builder()
            .url(release.asset.downloadUrl)
            .header("User-Agent", "F1-TV-Patcher")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw response.toGithubError()
            val source = response.body?.byteStream()
                ?: throw IOException("Empty body when downloading ${release.asset.name}")
            source.use { input ->
                val raw = temp.outputStream()
                val out = if (md != null) DigestOutputStream(raw, md) else raw
                out.use { input.copyTo(it) }
            }
        }

        if (md != null) {
            val actual = md.digest().joinToString("") { "%02x".format(it) }
            check(actual == expected) {
                "Downloaded APKM does not match GitHub asset digest"
            }
        }
        Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        target
    }
}

private fun Response.toGithubError(): GithubHttpException {
    val errBody = runCatching { body?.string().orEmpty() }.getOrDefault("")
    val parsed = runCatching { JSONObject(errBody).optString("message") }
        .getOrNull()?.ifBlank { null }
        ?.substringBefore(" (")
    val display = parsed
        ?: errBody.take(240).ifBlank { message.ifBlank { "HTTP $code" } }
    val reset = header("X-RateLimit-Reset")?.toLongOrNull()
    return GithubHttpException(code, reset, display)
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
