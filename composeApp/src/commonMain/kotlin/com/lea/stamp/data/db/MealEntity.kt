package com.lea.stamp.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "meals",
    foreignKeys = [
        ForeignKey(
            entity = DayEntity::class,
            parentColumns = ["id"],
            childColumns = ["dayId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["dayId"])]    
)
data class MealEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dayId: String, // Foreign key to DayEntity (format: ddMMyyyy)
    val capturedDate: Long?, // Timestamp when image was captured
    val mealType: String, // BREAKFAST, LUNCH, DINNER
    val imageUri: String?,
    val calories: Int?,
    val carbs: Double?,
    val protein: Double?,
    val fat: Double?,
    val fiber: Double?,
    val addedSugar: Double?,
    val sodium: Double?,
    val saturatedFat: Double?,
    val vegetableContent: Double?,
    val water: Double?,
    val aiComment: String?,
    val isHealthy: Boolean?
)
