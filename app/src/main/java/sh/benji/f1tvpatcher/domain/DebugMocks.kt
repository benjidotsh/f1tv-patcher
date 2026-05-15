package sh.benji.f1tvpatcher.domain

object DebugMocks {
    val release = ReleaseInfo(
        tagName = "v2026.05",
        title = "May 2026 patch",
        publishedAt = "2026-05-15T00:00:00Z",
        asset = ReleaseAsset(
            name = "f1tv-2026.05.apkm",
            downloadUrl = "https://example.invalid/mock",
            size = 218_400_000L,
            digest = null,
        ),
    )
    val installed = InstalledApp(
        packageName = "com.formulaone.production",
        versionName = "v2026.04",
        versionCode = 20260400L,
        signerDigest = "mock-signer",
    )
}
