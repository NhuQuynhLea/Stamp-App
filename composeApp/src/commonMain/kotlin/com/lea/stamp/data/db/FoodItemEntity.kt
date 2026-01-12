package com.lea.stamp.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "food_items",
    foreignKeys = [
        ForeignKey(
            entity = MealEntity::class,
            parentColumns = ["id"],
            childColumns = ["mealId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["mealId"])]
)
data class FoodItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mealId: Long,
    val label: String,
    val box2dY0: Int,
    val box2dX0: Int,
    val box2dY1: Int,
    val box2dX1: Int,
    val maskBase64: String,
    val calories: Int?,
    val protein: Double?,
    val fiber: Double?,
    val addedSugar: Double?,
    val saturatedFat: Double?,
    val sodium: Double?,
    val vegetableContent: Double?,
    val water: Double?,
    val processingLevel: Int?,
    val isHealthy: Boolean?
)
