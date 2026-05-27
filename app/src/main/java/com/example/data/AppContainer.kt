package com.example.data

import android.content.Context

object AppContainer {
    @Volatile
    private var isInitialized = false

    lateinit var database: AppDatabase
        private set

    lateinit var repository: TictokAdvertRepository
        private set

    fun initialize(context: Context) {
        if (isInitialized) return
        synchronized(this) {
            if (isInitialized) return
            database = AppDatabase.getDatabase(context)
            repository = TictokAdvertRepository(database.adDao())
            isInitialized = true
        }
    }
}
