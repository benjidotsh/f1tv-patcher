package sh.benji.f1tvpatcher

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class UpdateDeciderTest {
    private val release = ReleaseInfo(
        tagName = "v1",
        title = "F1TV UHD Patched",
        publishedAt = null,
        asset = ReleaseAsset("f1tv-uhd-patched.apkm", "https://example.invalid/app", 1, null),
    )

    private fun downloaded(versionCode: Long, signer: String) = DownloadedApkm(
        release = release,
        apkmFile = File("app.apkm"),
        extractedDir = File("extracted"),
        metadata = ApkMetadata(Constants.TARGET_PACKAGE, "1.0", versionCode, signer),
        apkFiles = listOf(File("base.apk")),
    )

    @Test
    fun noInstalledAppMeansNotInstalled() {
        assertTrue(UpdateDecider.decide(null, downloaded(10, "patched")) is UpdateStatus.NotInstalled)
    }

    @Test
    fun matchingSignerAndCurrentVersionMeansCurrent() {
        val installed = InstalledApp(Constants.TARGET_PACKAGE, "1.0", 10, "patched")

        assertTrue(UpdateDecider.decide(installed, downloaded(10, "patched")) is UpdateStatus.PatchedCurrent)
    }

    @Test
    fun matchingSignerAndOlderVersionMeansUpdateAvailable() {
        val installed = InstalledApp(Constants.TARGET_PACKAGE, "0.9", 9, "patched")

        assertTrue(UpdateDecider.decide(installed, downloaded(10, "patched")) is UpdateStatus.UpdateAvailable)
    }

    @Test
    fun signerMismatchMeansOriginalOrUnknown() {
        val installed = InstalledApp(Constants.TARGET_PACKAGE, "1.0", 10, "original")

        assertTrue(
            UpdateDecider.decide(installed, downloaded(10, "patched")) is
                UpdateStatus.OriginalOrUnknownInstalled,
        )
    }
}
