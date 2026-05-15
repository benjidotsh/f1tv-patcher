package sh.benji.f1tvpatcher

import java.io.File

data class ReleaseAsset(
    val name: String,
    val downloadUrl: String,
    val size: Long,
    val digest: String?,
)

data class ReleaseInfo(
    val tagName: String,
    val title: String,
    val publishedAt: String?,
    val asset: ReleaseAsset,
)

data class InstalledApp(
    val packageName: String,
    val versionName: String?,
    val versionCode: Long,
    val signerDigest: String?,
)

data class ApkMetadata(
    val packageName: String,
    val versionName: String?,
    val versionCode: Long,
    val signerDigest: String?,
)

data class DownloadedApkm(
    val release: ReleaseInfo,
    val apkmFile: File,
    val extractedDir: File,
    val metadata: ApkMetadata,
    val apkFiles: List<File>,
)

sealed interface UpdateStatus {
    data class NotInstalled(val release: ReleaseInfo) : UpdateStatus
    data class OriginalOrUnknownInstalled(val installed: InstalledApp, val release: ReleaseInfo) :
        UpdateStatus
    data class PatchedCurrent(val installed: InstalledApp, val release: ReleaseInfo) : UpdateStatus
    data class UpdateAvailable(val installed: InstalledApp, val release: ReleaseInfo) : UpdateStatus
}
