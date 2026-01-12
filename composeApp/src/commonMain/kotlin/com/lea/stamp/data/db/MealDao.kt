package com.lea.stamp.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MealDao {
    @Query("SELECT * FROM meals WHERE dayId = :dayId ORDER BY mealType ASC")
    suspend fun getMealsByDayId(dayId: String): List<MealEntity>
    
    @Query("SELECT * FROM meals WHERE id = :id")
    suspend fun getMealById(id: Long): MealEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeal(meal: MealEntity): Long
    
    @Update
    suspend fun updateMeal(meal: MealEntity)
    
    @Query("DELETE FROM meals WHERE id = :id")
    suspend fun deleteMeal(id: Long)
    
    @Query("SELECT COUNT(*) FROM meals WHERE dayId = :dayId AND imageUri IS NOT NULL")
    suspend fun getCapturedMealCountForDay(dayId: String): Int
    
    @Query("SELECT COUNT(*) FROM meals WHERE dayId = :dayId AND isHealthy = 1")
    suspend fun getHealthyMealCountForDay(dayId: String): Int
    
    @Query("SELECT * FROM meals WHERE id = :mealId")
    suspend fun getMealWithFoodItems(mealId: Long): MealWithFoodItems?
}
