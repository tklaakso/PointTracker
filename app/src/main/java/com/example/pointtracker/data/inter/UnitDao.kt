package com.example.pointtracker.data.inter

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.pointtracker.data.entity.Conversion
import com.example.pointtracker.data.entity.Unit

@Dao
interface UnitDao {
    @Query("SELECT * FROM units")
    suspend fun getAll(): List<Unit>

    @Query("SELECT * FROM units WHERE prompt_conversion = 1")
    suspend fun getAllFlaggedForConversion(): List<Unit>

    @Query("SELECT * FROM units WHERE id = :id")
    suspend fun getById(id: Int): Unit?

    @Query("SELECT * FROM units WHERE name = :name")
    suspend fun getByName(name: String): Unit?

    @Update
    suspend fun update(unit: Unit)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(unit: Unit)

    @Delete
    suspend fun delete(unit: Unit)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(units: List<Unit>)

    @Query("DELETE FROM units")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(units: List<Unit>) {
        deleteAll()
        insertAll(units)
    }
}