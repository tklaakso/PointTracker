package com.example.pointtracker.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "ingredients",
    foreignKeys = [
        ForeignKey(
            entity = Ingredient::class,
            parentColumns = ["id"],
            childColumns = ["parent"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = Unit::class,
            parentColumns = ["id"],
            childColumns = ["per_unit"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class Ingredient (
    @PrimaryKey(autoGenerate = true) val id: Int,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "parent") val parent: Int?,
    @ColumnInfo(name = "deletable") val deletable: Boolean,
    @ColumnInfo(name = "per_unit") val perUnit: Int?,
    @ColumnInfo(name = "per_unit_amount") val perUnitAmount: Double?,
)