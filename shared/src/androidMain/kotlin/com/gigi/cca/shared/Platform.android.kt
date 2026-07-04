package com.gigi.cca.shared

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

actual fun platform() = "Android"

fun getDatabaseBuilder(context: Context): RoomDatabase.Builder<AppDatabase> {
    val appContext = context.applicationContext
    return Room.databaseBuilder<AppDatabase>(
        context = context.applicationContext,
        name = appContext.getDatabasePath("main.db").absolutePath
    )
}