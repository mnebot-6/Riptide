package com.mnebot.riptide.data.local.db

import android.content.Context
import androidx.room.Room

object DatabaseProvider {
    private var instance: RiptideDatabase? = null

    fun getDatabase(context: Context): RiptideDatabase {
        return instance ?: synchronized(this) {
            Room.databaseBuilder(
                context.applicationContext,
                RiptideDatabase::class.java,
                "riptide.db"
            )
                .fallbackToDestructiveMigration(true)
                .build().also { instance = it }
        }
    }
}