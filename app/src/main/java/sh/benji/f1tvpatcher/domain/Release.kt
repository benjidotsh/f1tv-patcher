package sh.benji.f1tvpatcher.domain

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
