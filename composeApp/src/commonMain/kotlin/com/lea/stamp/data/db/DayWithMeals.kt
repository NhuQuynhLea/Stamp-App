package com.lea.stamp.data.db

import androidx.room.Embedded
import androidx.room.Relation

data class DayWithMeals(
    @Embedded val day: DayEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "dayId"
    )
    val meals: List<MealEntity>
)
