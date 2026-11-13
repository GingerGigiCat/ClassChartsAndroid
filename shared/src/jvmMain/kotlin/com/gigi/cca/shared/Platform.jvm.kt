package com.gigi.cca.shared

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

private fun dataDirectory(): File {
    val home = System.getProperty("user.home")
    val os = System.getProperty("os.name").lowercase()
    val base = when {
        "win" in os -> System.getenv("APPDATA") ?: "$home/AppData/Roaming"
        "mac" in os -> "$home/Library/Application Support"
        else -> System.getenv("XDG_DATA_HOME") ?: "$home/.local/share"
    }
    return File(base, "ClassChartsAndroid")
}

actual fun createStore(context: Any?): KeyValueStore = FileStore(dataDirectory())

actual fun usesTouchInput(): Boolean = false

actual val isDesktopApp: Boolean = true

actual fun netConfig(): NetConfig = NetConfig.Direct

@Composable
actual fun platformColorScheme(dark: Boolean): ColorScheme? = null

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
