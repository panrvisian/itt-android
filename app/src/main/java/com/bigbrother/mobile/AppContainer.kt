package com.bigbrother.mobile

import android.content.Context
import androidx.room.Room
import com.bigbrother.mobile.data.AppDatabase
import com.bigbrother.mobile.data.AppRepository
import com.bigbrother.mobile.data.SettingsStore

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val settingsStore = SettingsStore(appContext)
    val database: AppDatabase = Room.databaseBuilder(
        appContext,
        AppDatabase::class.java,
        "big_brother_mobile.db"
    ).addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4)
        .build()
    val repository = AppRepository(appContext, database, settingsStore)
}
