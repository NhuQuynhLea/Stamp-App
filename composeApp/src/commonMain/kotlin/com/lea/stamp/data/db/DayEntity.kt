package com.lea.stamp.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "days")
data class DayEntity(
    @PrimaryKey
    val id: String, // Format: ddMMyyyy (e.g., "11012026" for Jan 11, 2026)
    val dateTimestamp: Long // Start of day timestamp in milliseconds
)
