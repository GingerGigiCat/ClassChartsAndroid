package com.gigi.cca.shared

import android.content.Context
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

actual fun createStore(context: Any?): KeyValueStore {
    val appContext = context as? Context ?: error("Android needs a Context to store data")
    return FileStore(File(appContext.filesDir, "classcharts"))
}

actual fun usesTouchInput(): Boolean = true

actual val isDesktopApp: Boolean = false

actual fun netConfig(): NetConfig = NetConfig.Direct

@Composable
actual fun platformColorScheme(dark: Boolean): ColorScheme? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
    val context = LocalContext.current
    return if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
}

/** One JSON file per key, written atomically so a crash mid-write cannot corrupt the cache. */
internal class FileStore(private val dir: File) : KeyValueStore {
    private fun file(key: String) = File(dir, key.replace(Regex("[^A-Za-z0-9_-]"), "_") + ".json")

    override fun get(key: String): String? = runCatching { file(key).takeIf { it.exists() }?.readText() }.getOrNull()

    override fun put(key: String, value: String) {
        runCatching {
            dir.mkdirs()
            val target = file(key)
            val temp = File(dir, target.name + ".tmp")
            temp.writeText(value)
            runCatching {
                Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            }.onFailure {
                Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }

    override fun remove(key: String) {
        runCatching { file(key).delete() }
    }
}
