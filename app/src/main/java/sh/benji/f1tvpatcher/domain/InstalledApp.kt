package sh.benji.f1tvpatcher.domain

data class InstalledApp(
    val packageName: String,
    val versionName: String?,
    val versionCode: Long,
    val signerDigest: String?,
)
