package com.example.pointtracker.util

import android.content.Context
import com.example.pointtracker.data.DatabaseClient
import com.example.pointtracker.data.entity.Conversion
import com.example.pointtracker.data.entity.FoodItem
import com.example.pointtracker.data.entity.Ingredient
import com.example.pointtracker.data.entity.Recipe
import com.example.pointtracker.data.entity.Unit

class RecipeAnalyzer {
    companion object {
        private suspend fun getIngredientHierarchy(context : Context, ingredientId : Int) : List<Int> {
            val result = mutableListOf<Int>()
            val db = DatabaseClient(context).getDB()
            var ingredient = db.ingredientDao().getById(ingredientId)!!
            result.add(ingredient.id)
            while (ingredient.parent != null) {
                ingredient = db.ingredientDao().getById(ingredient.parent!!)!!
                result.add(ingredient.id)
            }
            return result
        }

        private suspend fun getAmounts(context : Context, ingredientAmount : IngredientAmount, units : List<Unit>, conversions : List<Conversion>, calculations : List<List<Conversion>>) : Map<Int, Double> {
            val conversionMap = HashMap<Pair<Int, Int>, MutableSet<Conversion>>()
            for (conversion in conversions) {
                if (!conversionMap.containsKey(Pair(conversion.unit1, conversion.ingredient)))
                    conversionMap[Pair(conversion.unit1, conversion.ingredient)] = mutableSetOf()
                if (!conversionMap.containsKey(Pair(conversion.unit2, conversion.ingredient)))
                    conversionMap[Pair(conversion.unit2, conversion.ingredient)] = mutableSetOf()
                conversionMap[Pair(conversion.unit1, conversion.ingredient)]!!.add(conversion)
                conversionMap[Pair(conversion.unit2, conversion.ingredient)]!!.add(conversion)
            }
            val calculationMap = HashMap<Pair<Int, Int>, MutableSet<List<Conversion>>>()
            for (calculation in calculations) {
                for (conversion in calculation) {
                    if (!calculationMap.containsKey(Pair(conversion.unit1, conversion.ingredient)))
                        calculationMap[Pair(conversion.unit1, conversion.ingredient)] = mutableSetOf()
                    calculationMap[Pair(conversion.unit1, conversion.ingredient)]!!.add(calculation)
                }
            }
            val foodItemAmountMap = units.associateBy({ it.id }, { -1.0 }).toMutableMap()
            foodItemAmountMap[ingredientAmount.unit] = ingredientAmount.amount
            val ingredientHierarchy = getIngredientHierarchy(context, ingredientAmount.ingredient)
            var modified = true
            while (modified) {
                modified = false
                for ((unit, amount) in foodItemAmountMap) {
                    if (amount >= 0) {
                        for (ingredient in ingredientHierarchy) {
                            if (!conversionMap.containsKey(Pair(unit, ingredient)))
                                continue
                            for (conversion in conversionMap[Pair(unit, ingredient)]!!) {
                                if (conversion.isConvertible(unit) && foodItemAmountMap.getValue(conversion.getOtherUnit(unit)) < 0) {
                                    foodItemAmountMap[conversion.getOtherUnit(unit)] = conversion.doConversion(unit, amount)
                                    modified = true
                                }
                            }
                        }
                    }
                }
                if (!modified) {
                    for ((unit, amount) in foodItemAmountMap) {
                        if (amount >= 0) {
                            for (ingredient in ingredientHierarchy) {
                                if (!calculationMap.containsKey(Pair(unit, ingredient)))
                                    continue
                                for (calculation in calculationMap[Pair(unit, ingredient)]!!) {
                                    var applicable = true
                                    for (conversion in calculation) {
                                        if (!foodItemAmountMap.containsKey(conversion.unit1) || foodItemAmountMap[conversion.unit1]!! < 0) {
                                            applicable = false
                                            break
                                        }
                                    }
                                    if (foodItemAmountMap.containsKey(calculation[0].unit2) && foodItemAmountMap[calculation[0].unit2]!! >= 0)
                                        continue
                                    if (applicable)
                                        foodItemAmountMap[calculation[0].unit2] = calculation.sumOf { it.doConversion(it.unit1, foodItemAmountMap[it.unit1]!!) }
                                    modified = true
                                }
                            }
                        }
                    }
                }
            }
            return foodItemAmountMap
        }

        suspend fun analyzeIngredientAmounts(context: Context, ingredientAmounts: List<IngredientAmount>, scale: Double, portions: Double): Map<Int, Double> {
            val db = DatabaseClient(context).getDB()
            val units: List<Unit> = db.unitDao().getAll()
            val conversions: List<Conversion> = db.conversionDao().getAllIndividual()
            val calculationConversions: List<Conversion> = db.conversionDao().getAllCalculations()
            val calculations : List<List<Conversion>> = calculationConversions.groupBy { Pair(it.ingredient, it.unit2) }.values.toList()
            val unitsMap = units.associateBy({ it.id }, { 0.0 }).toMutableMap()
            for (ingredientAmount in ingredientAmounts) {
                var foodItemAmountMap : Map<Int, Double>?
                if (ingredientAmount.isRecipe) {
                    val recipe = db.recipeDao().getById(ingredientAmount.ingredient)!!
                    foodItemAmountMap = analyzeRecipe(context, ingredientAmount, recipe)
                }
                else {
                    foodItemAmountMap = getAmounts(context, ingredientAmount, units, conversions, calculations)
                }
                for ((unit, amount) in foodItemAmountMap) {
                    if (unitsMap[unit]!! >= 0) {
                        if (amount >= 0) {
                            unitsMap[unit] = unitsMap[unit]!! + amount * scale
                        } else {
                            unitsMap[unit] = -(ingredientAmount.ingredient + 1).toDouble()
                        }
                    }
                }
            }
            return unitsMap
        }

        suspend fun analyzeFoodItems(context : Context, scale : Double, portions : Double, foodItems : List<FoodItem>) : Map<Int, Double> {
            return analyzeIngredientAmounts(context, foodItems.map { IngredientAmount(it.unit, if (it.isRecipe) it.recipe!! else it.ingredient!!, it.isRecipe, it.quantity) }, scale, portions)
        }

        suspend fun analyzeRecipe(context : Context, ingredientAmount: IngredientAmount, recipe : Recipe): Map<Int, Double> {
            val db = DatabaseClient(context).getDB()
            val foodItems = db.foodItemDao().getByRecipe(recipe.id)
            val result = analyzeFoodItems(context, recipe.scale, recipe.portions, foodItems).toMutableMap()
            if (recipe.finalWeightUnit != null && recipe.finalWeightAmount != null) {
                result[recipe.finalWeightUnit] = recipe.finalWeightAmount
            }
            var scaleFactor = ingredientAmount.amount
            val ingredientUnit = ingredientAmount.unit
            val ingredientUnitObj = db.unitDao().getById(ingredientUnit)
            if (ingredientUnitObj!!.name == "whole") {
                scaleFactor = ingredientAmount.amount
            }
            else if (ingredientUnitObj.name == "portion") {
                scaleFactor = ingredientAmount.amount / recipe.portions
            }
            else if (ingredientUnit in result) {
                scaleFactor = ingredientAmount.amount / result[ingredientUnit]!!
            }
            for ((unit, amount) in result) {
                result[unit] = amount * scaleFactor
            }
            return result
        }

        suspend fun analyzeIngredient(context : Context, unit : Int, amount : Double, ingredient : Int): Map<Int, Double> {
            return analyzeIngredientAmounts(context, listOf(IngredientAmount(unit, ingredient, false, amount)), 1.0, 1.0)
        }
    }
}