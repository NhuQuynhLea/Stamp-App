package com.lea.stamp.data.db

import androidx.room.Embedded
import androidx.room.Relation

data class MealWithFoodItems(
    @Embedded val meal: MealEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "mealId"
    )
    val foodItems: List<FoodItemEntity>
)
