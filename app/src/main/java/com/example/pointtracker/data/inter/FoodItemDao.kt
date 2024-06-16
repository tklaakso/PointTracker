package com.example.pointtracker.data.inter

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.pointtracker.data.entity.Conversion
import com.example.pointtracker.data.entity.FoodItem
import com.example.pointtracker.data.entity.Recipe

@Dao
interface FoodItemDao {
    @Query("SELECT * FROM food_items")
    suspend fun getAll(): List<FoodItem>

    @Query("SELECT * FROM food_items WHERE constituent = :recipe")
    suspend fun getByRecipe(recipe: Int): List<FoodItem>

    @Update
    suspend fun update(foodItem: FoodItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(foodItem: FoodItem)

    @Delete
    suspend fun delete(foodItem: FoodItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(foodItems: List<FoodItem>)

    @Query("DELETE FROM food_items")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(foodItems: List<FoodItem>) {
        deleteAll()
        insertAll(foodItems)
    }
}