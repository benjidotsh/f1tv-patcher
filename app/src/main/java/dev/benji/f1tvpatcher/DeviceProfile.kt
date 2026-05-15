package dev.benji.f1tvpatcher

import android.content.Context
import android.os.Build
import java.util.Locale

data class DeviceProfile(
    val supportedAbis: List<String>,
    val language: String,
    val densityDpi: Int,
) {
    companion object {
        fun from(context: Context): DeviceProfile =
            DeviceProfile(
                supportedAbis = Build.SUPPORTED_ABIS.toList(),
                language = Locale.getDefault().language.ifBlank { "en" },
                densityDpi = context.resources.displayMetrics.densityDpi,
            )
    }
}
