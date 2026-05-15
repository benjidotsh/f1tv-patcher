package sh.benji.f1tvpatcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReleaseSelectorTest {
    @Test
    fun prefersNamedPatchedApkmAsset() {
        val assets = listOf(
            ReleaseAsset("other.apkm", "https://example.invalid/other", 1, null),
            ReleaseAsset("f1tv-uhd-patched.apkm", "https://example.invalid/f1", 2, "sha256:abc"),
        )

        assertEquals("f1tv-uhd-patched.apkm", ReleaseSelector.selectApkmAsset(assets)?.name)
    }

    @Test
    fun fallsBackToAnyApkmAsset() {
        val assets = listOf(ReleaseAsset("release.apkm", "https://example.invalid/release", 1, null))

        assertEquals("release.apkm", ReleaseSelector.selectApkmAsset(assets)?.name)
    }

    @Test
    fun returnsNullWhenReleaseHasNoApkm() {
        val assets = listOf(ReleaseAsset("notes.txt", "https://example.invalid/notes", 1, null))

        assertNull(ReleaseSelector.selectApkmAsset(assets))
    }
}
