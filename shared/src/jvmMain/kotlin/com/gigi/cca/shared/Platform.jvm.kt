package com.gigi.cca.shared

import androidx.room.Room
import androidx.room.RoomDatabase
import me.sujanpoudel.utils.paths.appDataDirectory
import java.awt.Desktop
import java.io.File

actual fun platform() = "jvm"
val packageName = "com.gigi.cca.shared"

actual fun getDatabaseBuilder(context: Any?): RoomDatabase.Builder<AppDatabase> {
    return Room.databaseBuilder<AppDatabase>(
        name = File(appDataDirectory(packageName).toString(), "main.db").absolutePath
    )
}

actual fun openUriMime(uri: String) {
    Desktop.getDesktop().open(File(uri))
}