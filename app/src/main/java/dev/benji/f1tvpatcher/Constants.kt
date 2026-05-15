package dev.benji.f1tvpatcher

object Constants {
    const val TARGET_PACKAGE = "com.formulaone.production"
    const val RELEASE_OWNER = "Alexvbp"
    const val RELEASE_REPO = "f1tv-4k-patch"
    const val RELEASE_API =
        "https://api.github.com/repos/$RELEASE_OWNER/$RELEASE_REPO/releases/latest"
    const val NOTIFICATION_CHANNEL = "updates"
    const val INSTALL_ACTION = "dev.benji.f1tvpatcher.INSTALL_STATUS"
}
