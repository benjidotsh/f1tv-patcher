package sh.benji.f1tvpatcher

import org.junit.Assert.assertEquals
import org.junit.Test
import sh.benji.f1tvpatcher.domain.DeviceProfile
import sh.benji.f1tvpatcher.domain.SplitSelector
import java.io.File

class SplitSelectorTest {
    @Test
    fun selectsBaseAbiLanguageAndDensitySplits() {
        val files = listOf(
            "base.apk",
            "split_required_player.apk",
            "config.arm64_v8a.apk",
            "config.armeabi_v7a.apk",
            "config.en.apk",
            "config.fr.apk",
            "config.xhdpi.apk",
            "config.xxhdpi.apk",
        ).map(::File)

        val selected = SplitSelector.select(
            files,
            DeviceProfile(
                supportedAbis = listOf("arm64-v8a", "armeabi-v7a"),
                language = "fr",
                densityDpi = 320,
            ),
        )

        assertEquals(
            listOf(
                "base.apk",
                "split_required_player.apk",
                "config.arm64_v8a.apk",
                "config.fr.apk",
                "config.xhdpi.apk",
            ),
            selected.map { it.name },
        )
    }

    @Test
    fun fallsBackToEnglishLanguageSplit() {
        val files = listOf("base.apk", "config.en.apk", "config.xhdpi.apk").map(::File)

        val selected = SplitSelector.select(
            files,
            DeviceProfile(supportedAbis = emptyList(), language = "nl", densityDpi = 320),
        )

        assertEquals(listOf("base.apk", "config.en.apk", "config.xhdpi.apk"), selected.map { it.name })
    }

    @Test
    fun supportsApkMirrorAndGooglePlayConfigSplitNames() {
        val files = listOf(
            "base.apk",
            "split_config.x86.apk",
            "split_config.fr.apk",
            "com.formulaone.production.config.xhdpi.apk",
        ).map(::File)

        val selected = SplitSelector.select(
            files,
            DeviceProfile(supportedAbis = listOf("x86"), language = "fr", densityDpi = 320),
        )

        assertEquals(
            listOf(
                "base.apk",
                "split_config.x86.apk",
                "split_config.fr.apk",
                "com.formulaone.production.config.xhdpi.apk",
            ),
            selected.map { it.name },
        )
    }

    @Test
    fun choosesClosestAvailableDensitySplit() {
        val files = listOf("base.apk", "config.xhdpi.apk", "config.xxhdpi.apk").map(::File)

        val selected = SplitSelector.select(
            files,
            DeviceProfile(supportedAbis = emptyList(), language = "nl", densityDpi = 400),
        )

        assertEquals(listOf("base.apk", "config.xhdpi.apk"), selected.map { it.name })
    }
}
