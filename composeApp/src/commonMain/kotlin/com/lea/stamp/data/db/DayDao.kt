package com.lea.stamp.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface DayDao {
    @Transaction
    @Query("SELECT * FROM days ORDER BY dateTimestamp DESC LIMIT :limit OFFSET :offset")
    fun getDaysWithMealsPaginated(limit: Int, offset: Int): Flow<List<DayWithMeals>>

    @Transaction
    @Query("SELECT * FROM days WHERE id = :dayId")
    fun getDayWithMealsFlow(dayId: String): Flow<DayWithMeals?>
    
    @Query("SELECT * FROM days WHERE id = :dayId")
    suspend fun getDayById(dayId: String): DayEntity?
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDay(day: DayEntity): Long
    
    @Query("SELECT COUNT(*) FROM days")
    suspend fun getDayCount(): Int

    @Query("SELECT COUNT(*) FROM days WHERE dateTimestamp > :dateTimestamp")
    suspend fun getDaysNewerThan(dateTimestamp: Long): Int
    
    @Transaction
    @Query("SELECT * FROM days WHERE id = :dayId")
    suspend fun getDayWithMeals(dayId: String): DayWithMeals?
}
