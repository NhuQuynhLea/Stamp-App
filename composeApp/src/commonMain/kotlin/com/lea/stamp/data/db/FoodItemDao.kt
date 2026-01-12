package com.lea.stamp.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface FoodItemDao {
    @Query("SELECT * FROM food_items WHERE mealId = :mealId")
    suspend fun getFoodItemsByMealId(mealId: Long): List<FoodItemEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoodItem(foodItem: FoodItemEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoodItems(foodItems: List<FoodItemEntity>)
    
    @Update
    suspend fun updateFoodItem(foodItem: FoodItemEntity)
    
    @Query("DELETE FROM food_items WHERE mealId = :mealId")
    suspend fun deleteFoodItemsByMealId(mealId: Long)
    
    @Query("DELETE FROM food_items WHERE id = :id")
    suspend fun deleteFoodItem(id: Long)

    @Query("SELECT * FROM food_items WHERE mealId IN (:mealIds)")
    suspend fun getFoodItemsByMealIds(mealIds: List<Long>): List<FoodItemEntity>
}
