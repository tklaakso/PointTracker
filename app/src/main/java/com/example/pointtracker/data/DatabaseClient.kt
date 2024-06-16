package com.example.pointtracker.data

import android.content.Context
import androidx.room.Room

class DatabaseClient(context: Context) {

    private var db : PointDatabase? = null

    init {
        db = Room.databaseBuilder(
            context,
            PointDatabase::class.java, "point-database"
        ).build()
    }

    fun getDB() : PointDatabase {
        return db!!
    }

}