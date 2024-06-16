package com.example.pointtracker.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlin.math.abs

@Entity(tableName = "conversions",
    foreignKeys = [
        ForeignKey(
            entity = Ingredient::class,
            parentColumns = ["id"],
            childColumns = ["ingredient"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Unit::class,
            parentColumns = ["id"],
            childColumns = ["unit1"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Unit::class,
            parentColumns = ["id"],
            childColumns = ["unit2"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Conversion (
    @PrimaryKey(autoGenerate = true) val id: Int,
    @ColumnInfo(name = "ingredient") val ingredient: Int,
    @ColumnInfo(name = "unit1") val unit1: Int,
    @ColumnInfo(name = "unit2") val unit2: Int,
    @ColumnInfo(name = "quantity1") val quantity1: Double,
    @ColumnInfo(name = "quantity2") val quantity2: Double,
    @ColumnInfo(name = "include_in_calculation") val includeInCalculation: Boolean,
) {
    fun doConversion(unit : Int, quantity : Double): Double {
        if (unit == unit1) {
            return (quantity2 / quantity1) * quantity
        }
        else if (unit == unit2) {
            return (quantity1 / quantity2) * quantity
        }
        throw IllegalArgumentException("Invalid unit")
    }

    fun isConvertible(unit : Int): Boolean {
        if (unit == unit1)
            return abs(quantity1) > 0
        if (unit == unit2)
            return abs(quantity2) > 0
        throw IllegalArgumentException("Invalid unit")
    }

    fun getOtherUnit(unit : Int): Int {
        if (unit == unit1) {
            return unit2
        }
        else if (unit == unit2) {
            return unit1
        }
        throw IllegalArgumentException("Invalid unit")
    }
}