package com.example.pointtracker.data

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.pointtracker.data.entity.Conversion
import com.example.pointtracker.data.entity.FoodItem
import com.example.pointtracker.data.entity.Ingredient
import com.example.pointtracker.data.entity.Limit
import com.example.pointtracker.data.entity.Recipe
import com.example.pointtracker.data.entity.TrackerItem
import com.example.pointtracker.data.entity.Unit
import com.example.pointtracker.data.inter.ConversionDao
import com.example.pointtracker.data.inter.FoodItemDao
import com.example.pointtracker.data.inter.IngredientDao
import com.example.pointtracker.data.inter.LimitDao
import com.example.pointtracker.data.inter.RecipeDao
import com.example.pointtracker.data.inter.TrackerItemDao
import com.example.pointtracker.data.inter.UnitDao

@Database(entities = [Conversion::class, FoodItem::class, Ingredient::class, Recipe::class, Unit::class, Limit::class, TrackerItem::class], version = 3, autoMigrations = [
    AutoMigration (from = 1, to = 2), AutoMigration (from = 2, to = 3)
])
abstract class PointDatabase : RoomDatabase() {
    abstract fun conversionDao(): ConversionDao
    abstract fun foodItemDao(): FoodItemDao
    abstract fun ingredientDao(): IngredientDao
    abstract fun recipeDao(): RecipeDao
    abstract fun unitDao(): UnitDao
    abstract fun limitDao(): LimitDao
    abstract fun trackerItemDao(): TrackerItemDao
}