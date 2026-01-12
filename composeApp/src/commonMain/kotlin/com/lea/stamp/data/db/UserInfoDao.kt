package com.lea.stamp.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserInfoDao {
    @Query("SELECT * FROM user_info WHERE id = 0")
    fun getUserInfo(): Flow<UserInfoEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserInfo(userInfo: UserInfoEntity)

    @Insert
    suspend fun insertWeight(weightHistory: WeightHistoryEntity)

    @Query("SELECT * FROM weight_history ORDER BY dateTimestamp ASC")
    fun getWeightHistory(): Flow<List<WeightHistoryEntity>>
}
