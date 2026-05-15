package dev.benji.f1tvpatcher

object ReleaseSelector {
    fun selectApkmAsset(assets: List<ReleaseAsset>): ReleaseAsset? =
        assets.firstOrNull { asset ->
            asset.name.equals("f1tv-uhd-patched.apkm", ignoreCase = true)
        } ?: assets.firstOrNull { asset ->
            asset.name.endsWith(".apkm", ignoreCase = true)
        }
}
