package sh.benji.f1tvpatcher

object Constants {
    const val TARGET_PACKAGE = "com.formulaone.production"
    const val RELEASE_OWNER = "Alexvbp"
    const val RELEASE_REPO = "f1tv-4k-patch"
    const val RELEASE_API =
        "https://api.github.com/repos/$RELEASE_OWNER/$RELEASE_REPO/releases/latest"
    const val INSTALL_ACTION = "sh.benji.f1tvpatcher.INSTALL_STATUS"
    const val INSTALL_FAILED_ACTION = "sh.benji.f1tvpatcher.INSTALL_FAILED"
    const val EXTRA_INSTALL_FAILURE_MESSAGE = "message"
}
