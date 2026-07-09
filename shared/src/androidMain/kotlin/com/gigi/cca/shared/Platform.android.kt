package com.gigi.cca.shared

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.room.Room
import androidx.room.RoomDatabase
import com.gigi.classchartsandroid.MainActivity

actual fun platform() = "Android"

actual fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val appContext = android.content.Context
    return Room.databaseBuilder<AppDatabase>(
        context = appContext,
        name = appContext.getDatabasePath("main.db").absolutePath
    )
}

