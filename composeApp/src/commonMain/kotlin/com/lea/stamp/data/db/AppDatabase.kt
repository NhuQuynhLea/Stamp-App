package com.lea.stamp.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [DayEntity::class, MealEntity::class, FoodItemEntity::class, UserInfoEntity::class, WeightHistoryEntity::class],
    version = 8,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dayDao(): DayDao
    abstract fun mealDao(): MealDao
    abstract fun foodItemDao(): FoodItemDao
    abstract fun userInfoDao(): UserInfoDao
}
