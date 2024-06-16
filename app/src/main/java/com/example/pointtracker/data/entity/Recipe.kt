package com.example.pointtracker.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recipes")
data class Recipe (
    @PrimaryKey(autoGenerate = true) val id: Int,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "scale") val scale: Double,
    @ColumnInfo(name = "portions") val portions: Double,
    @ColumnInfo(name = "final_weight_amount") val finalWeightAmount : Double?,
    @ColumnInfo(name = "final_weight_unit") val finalWeightUnit : Int?,
)