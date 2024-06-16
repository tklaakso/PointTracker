package com.example.pointtracker.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "tracker_items",
    foreignKeys = [
        ForeignKey(
            entity = Unit::class,
            parentColumns = ["id"],
            childColumns = ["unit"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Recipe::class,
            parentColumns = ["id"],
            childColumns = ["recipe"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Ingredient::class,
            parentColumns = ["id"],
            childColumns = ["ingredient"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class TrackerItem (
    @PrimaryKey(autoGenerate = true) val id: Int,
    @ColumnInfo(name = "unit") val unit: Int,
    @ColumnInfo(name = "amount") val amount: Double,
    @ColumnInfo(name = "is_recipe") val isRecipe: Boolean,
    @ColumnInfo(name = "recipe") val recipe: Int?,
    @ColumnInfo(name = "ingredient") val ingredient: Int?,
    @ColumnInfo(name = "date") val date: Long,
)