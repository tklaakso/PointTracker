package com.example.pointtracker.data

import android.content.Context
import com.example.pointtracker.data.entity.Conversion
import com.example.pointtracker.data.entity.FoodItem
import com.example.pointtracker.data.entity.Ingredient
import com.example.pointtracker.data.entity.Limit
import com.example.pointtracker.data.entity.Recipe
import com.example.pointtracker.data.entity.TrackerItem
import com.example.pointtracker.data.entity.Unit

class DatabaseContainer(@Transient var context: Context) {

    var conversions : List<Conversion>? = emptyList()
    var foodItems : List<FoodItem>? = emptyList()
    var ingredients : List<Ingredient>? = emptyList()
    var limits : List<Limit>? = emptyList()
    var recipes : List<Recipe>? = emptyList()
    var trackerItems : List<TrackerItem>? = emptyList()
    var units : List<Unit>? = emptyList()

    suspend fun populateFromDB() : DatabaseContainer {
        val db = DatabaseClient(context).getDB()
        val conversionDao = db.conversionDao()
        val foodItemDao = db.foodItemDao()
        val ingredientDao = db.ingredientDao()
        val limitDao = db.limitDao()
        val recipeDao = db.recipeDao()
        val trackerItemDao = db.trackerItemDao()
        val unitDao = db.unitDao()
        conversions = conversionDao.getAll()
        foodItems = foodItemDao.getAll()
        ingredients = ingredientDao.getAll()
        limits = limitDao.getAll()
        recipes = recipeDao.getAll()
        trackerItems = trackerItemDao.getAll()
        units = unitDao.getAll()
        return this
    }

    fun setContext(context: Context) : DatabaseContainer {
        this.context = context
        return this
    }

    suspend fun writeToDB() {
        val db = DatabaseClient(context).getDB()
        val conversionDao = db.conversionDao()
        val foodItemDao = db.foodItemDao()
        val ingredientDao = db.ingredientDao()
        val limitDao = db.limitDao()
        val recipeDao = db.recipeDao()
        val trackerItemDao = db.trackerItemDao()
        val unitDao = db.unitDao()
        unitDao.deleteAll()
        ingredientDao.deleteAll()
        limitDao.deleteAll()
        trackerItemDao.deleteAll()
        conversionDao.deleteAll()
        foodItemDao.deleteAll()
        recipeDao.deleteAll()
        recipeDao.insertAll(recipes!!)
        unitDao.insertAll(units!!)
        ingredientDao.insertAll(ingredients!!)
        limitDao.insertAll(limits!!)
        trackerItemDao.insertAll(trackerItems!!)
        conversionDao.insertAll(conversions!!)
        foodItemDao.insertAll(foodItems!!)
    }
}