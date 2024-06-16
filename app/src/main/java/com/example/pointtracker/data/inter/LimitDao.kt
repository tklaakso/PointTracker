package com.example.pointtracker.data.inter

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.pointtracker.data.entity.Conversion
import com.example.pointtracker.data.entity.Limit
import com.example.pointtracker.data.entity.Recipe

@Dao
interface LimitDao {
    @Query("SELECT * FROM limits")
    suspend fun getAll(): List<Limit>

    @Query("SELECT * FROM limits WHERE id = :id")
    suspend fun getById(id: Int): Limit?

    @Query("SELECT * FROM limits WHERE unit = :unit AND daily = :daily")
    suspend fun getByParameters(unit: Int, daily: Boolean) : Limit?

    @Update
    suspend fun update(limit: Limit)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(limit: Limit)

    @Delete
    suspend fun delete(limit: Limit)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(limits: List<Limit>)

    @Query("DELETE FROM limits")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(limits: List<Limit>) {
        deleteAll()
        insertAll(limits)
    }
}