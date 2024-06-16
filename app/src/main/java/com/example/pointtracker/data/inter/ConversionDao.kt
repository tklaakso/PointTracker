package com.example.pointtracker.data.inter

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.pointtracker.data.entity.Conversion
import com.example.pointtracker.data.entity.Recipe

@Dao
interface ConversionDao {
    @Query("SELECT * FROM conversions")
    suspend fun getAll(): List<Conversion>

    @Query("SELECT * FROM conversions WHERE include_in_calculation = 0")
    suspend fun getAllIndividual(): List<Conversion>

    @Query("SELECT * FROM conversions WHERE include_in_calculation = 1")
    suspend fun getAllCalculations() : List<Conversion>

    @Query("SELECT * FROM conversions WHERE ingredient = :ingredient AND unit1 = :unit1 AND unit2 = :unit2 AND include_in_calculation = :includeInCalculation")
    suspend fun getByParameters(ingredient: Int, unit1: Int, unit2: Int, includeInCalculation: Boolean): Conversion?

    @Query("DELETE FROM conversions WHERE ingredient = :ingredient AND unit1 = :unit1 AND unit2 = :unit2 AND include_in_calculation = :includeInCalculation")
    suspend fun deleteByParameters(ingredient: Int, unit1: Int, unit2: Int, includeInCalculation: Boolean)

    @Transaction
    suspend fun replaceByParameters(conversion: Conversion) {
        deleteByParameters(conversion.ingredient, conversion.unit1, conversion.unit2, conversion.includeInCalculation)
        insert(conversion)
    }

    @Query("SELECT * FROM conversions WHERE id = :id")
    suspend fun getById(id: Int): Conversion?

    @Update
    suspend fun update(conversion: Conversion)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(conversion: Conversion)

    @Delete
    suspend fun delete(conversion: Conversion)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(conversions: List<Conversion>)

    @Query("DELETE FROM conversions")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(conversions: List<Conversion>) {
        deleteAll()
        insertAll(conversions)
    }
}