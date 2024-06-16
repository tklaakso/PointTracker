package com.example.pointtracker.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "limits",
    foreignKeys = [
        ForeignKey(
            entity = Unit::class,
            parentColumns = ["id"],
            childColumns = ["unit"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Limit (
    @PrimaryKey(autoGenerate = true) val id: Int,
    @ColumnInfo(name = "unit") val unit: Int,
    @ColumnInfo(name = "amount") val amount: Double,
    @ColumnInfo(name = "daily") val daily: Boolean,
)