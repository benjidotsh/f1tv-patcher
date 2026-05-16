package sh.benji.f1tvpatcher.domain

import java.io.File

data class ApkMetadata(
    val packageName: String,
    val versionName: String?,
    val versionCode: Long,
    val signerDigest: String?,
)

data class DownloadedApkm(
    val release: ReleaseInfo,
    val metadata: ApkMetadata,
    val apkFiles: List<File>,
)
