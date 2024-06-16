package com.example.pointtracker.data.inter

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.pointtracker.data.entity.Conversion
import com.example.pointtracker.data.entity.Ingredient
import com.example.pointtracker.data.entity.Recipe

@Dao
interface IngredientDao {
    @Query("SELECT * FROM ingredients")
    suspend fun getAll(): List<Ingredient>

    @Query("SELECT * FROM ingredients WHERE id = :id")
    suspend fun getById(id: Int): Ingredient?

    @Query("SELECT * FROM ingredients WHERE name = :name")
    suspend fun getByName(name: String): Ingredient?

    @Update
    suspend fun update(ingredient: Ingredient)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(ingredient: Ingredient)

    @Delete
    suspend fun delete(ingredient: Ingredient)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(ingredients: List<Ingredient>)

    @Query("DELETE FROM ingredients")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(ingredients: List<Ingredient>) {
        deleteAll()
        insertAll(ingredients)
    }
}