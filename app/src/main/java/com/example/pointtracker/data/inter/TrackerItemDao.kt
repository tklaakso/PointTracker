package com.example.pointtracker.data.inter

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.pointtracker.data.entity.Conversion
import com.example.pointtracker.data.entity.TrackerItem
import com.example.pointtracker.data.entity.Unit

@Dao
interface TrackerItemDao {
    @Query("SELECT * FROM tracker_items")
    suspend fun getAll(): List<TrackerItem>

    @Query("SELECT * FROM tracker_items WHERE id = :id")
    suspend fun getById(id: Int): TrackerItem?

    @Query("SELECT * FROM tracker_items WHERE date = :date")
    suspend fun getByDate(date: Long) : List<TrackerItem>

    @Update
    suspend fun update(trackerItem: TrackerItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(trackerItem: TrackerItem)

    @Delete
    suspend fun delete(trackerItem: TrackerItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(trackerItems: List<TrackerItem>)

    @Query("DELETE FROM tracker_items")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(trackerItems: List<TrackerItem>) {
        deleteAll()
        insertAll(trackerItems)
    }
}