package com.example.pointtracker.util

import android.content.Context
import android.widget.LinearLayout
import android.widget.TextView
import com.example.pointtracker.R
import com.example.pointtracker.data.DatabaseClient
import java.util.Calendar

class TrackerUtil {
    companion object {
        fun getDayStartTime() : Long {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = System.currentTimeMillis()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            return calendar.timeInMillis
        }

        fun getPriorDaysOfWeek() : List<Long> {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = getDayStartTime()
            val daysOfWeek = mutableListOf<Long>()
            for (i in 0..6) {
                daysOfWeek.add(calendar.timeInMillis)
                if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY)
                    break
                calendar.add(Calendar.DAY_OF_YEAR, -1)
            }
            return daysOfWeek.reversed()
        }

        suspend fun getConsumptionAnalysis(context : Context, excludeTrackerItems : List<Int> = listOf()) : Triple<Pair<Map<Int, Double>, Map<Int, Double>>, Pair<Map<Int, Double>, Map<Int, Double>>, Map<Int, Int>> {
            RecipeAnalyzer.invalidateCache()
            val db = DatabaseClient(context).getDB()
            val limits = db.limitDao().getAll()
            val dailyLimits = limits.filter { it.daily }
            val weeklyLimits = limits.filter { !it.daily }
            val weeklyConsumption = weeklyLimits.associateBy({ it.unit }, { it.amount }).toMutableMap()
            val weeklyConsumptionOriginal = weeklyConsumption.toMap()
            var dailyConsumption: Map<Int, Double>? = null
            var dailyConsumptionOriginal: Map<Int, Double>? = null
            val missingConversions: MutableMap<Int, Int> = mutableMapOf()
            for (day in getPriorDaysOfWeek()) {
                val trackerItems = db.trackerItemDao().getByDate(day).filter { !excludeTrackerItems.contains(it.id) }
                val unitsMap = RecipeAnalyzer.analyzeIngredientAmounts(
                    context,
                    trackerItems.map { IngredientAmount(it.unit, (if (it.isRecipe) it.recipe else it.ingredient)!!, it.isRecipe, it.amount - (it.finalAmount ?: 0.0)) },
                    1.0,
                    1.0
                )
                dailyConsumption = dailyLimits.associateBy({ it.unit }, { it.amount }).toMutableMap()
                dailyConsumptionOriginal = dailyConsumption.toMap()
                for ((unit, amount) in unitsMap) {
                    if (amount >= 0) {
                        if (unit in dailyConsumption && dailyConsumption[unit]!! >= amount) {
                            dailyConsumption[unit] = dailyConsumption[unit]!! - amount
                        }
                        else {
                            var toConsume = amount
                            if (unit in dailyConsumption) {
                                toConsume -= dailyConsumption[unit]!!
                                dailyConsumption[unit] = 0.0
                            }
                            if (unit in weeklyConsumption) {
                                weeklyConsumption[unit] = weeklyConsumption[unit]!! - toConsume
                            }
                        }
                    }
                    else {
                        missingConversions[unit] = -amount.toInt() - 1
                    }
                }
            }
            return Triple(Pair(dailyConsumptionOriginal!!, dailyConsumption!!), Pair(weeklyConsumptionOriginal, weeklyConsumption), missingConversions)
        }
    }
}