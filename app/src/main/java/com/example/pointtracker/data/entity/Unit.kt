package com.example.pointtracker.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "units")
data class Unit (
    @PrimaryKey(autoGenerate = true) val id: Int,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "show_in_analysis") val showInAnalysis: Boolean,
    @ColumnInfo(name = "prompt_conversion") val promptConversion: Boolean,
    @ColumnInfo(name = "deletable") val deletable: Boolean,
)