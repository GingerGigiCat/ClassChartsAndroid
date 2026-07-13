package com.gigi.cca.shared

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.UriHandler
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.core.net.toUri

actual fun platform() = "Android"

var appContext: Context? = null

private fun String.getMimeType(): String? {
    return MimeTypeMap.getFileExtensionFromUrl(toString())?.run {
        MimeTypeMap.getSingleton().getMimeTypeFromExtension(lowercase())
    }?: "text/html"
}

actual fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    if (appContext != null) {
        return Room.databaseBuilder<AppDatabase>(
            context = appContext!!,
            name = appContext!!.getDatabasePath("main.db").absolutePath
        )
    }
    error("uhh there's no context to make a database with")
}

actual fun openUriMime(uri: String) {
    if (appContext != null) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(
                uri.toUri(),
                uri.getMimeType()
            )
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        appContext!!.startActivity(intent)
    }
}