package com.lea.stamp.data.db

expect fun getDatabaseBuilder(): AppDatabase

object DatabaseProvider {
    private var database: AppDatabase? = null
    
    fun getDatabase(): AppDatabase {
        return database ?: getDatabaseBuilder().also { database = it }
    }
}
