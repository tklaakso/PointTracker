package com.example.pointtracker.util

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog
import com.example.pointtracker.data.DatabaseClient
import java.nio.ByteBuffer

class Util {
    companion object {
        fun buildErrorDialog(activity: Activity, message : String) {
            AlertDialog.Builder(activity)
                .setTitle("Error")
                .setMessage(message)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }

        fun buildWarningDialog(activity: Activity, message : String, listener : DialogInterface.OnClickListener) {
            AlertDialog.Builder(activity)
                .setTitle("Warning")
                .setMessage(message)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setPositiveButton(android.R.string.ok, listener)
                .setNegativeButton("Cancel", null)
                .show()
        }

        suspend fun checkIfRecipeIngredientNameInUse(context : Context, name : String) : Boolean {
            val db = DatabaseClient(context).getDB()
            return db.ingredientDao().getByName(name) != null || db.recipeDao().getByName(name) != null
        }

        suspend fun checkIfRecipeNameInUse(context : Context, name : String) : Boolean {
            val db = DatabaseClient(context).getDB()
            return db.recipeDao().getByName(name) != null
        }

        suspend fun checkIfIngredientNameInUse(context : Context, name : String) : Boolean {
            val db = DatabaseClient(context).getDB()
            return db.ingredientDao().getByName(name) != null
        }

        suspend fun checkIfUnitNameInUse(context : Context, name : String) : Boolean {
            val db = DatabaseClient(context).getDB()
            return db.unitDao().getByName(name) != null
        }

        suspend fun checkIfLimitAlreadyExists(context : Context, unit : Int, daily : Boolean) : Boolean {
            val db = DatabaseClient(context).getDB()
            return db.limitDao().getByParameters(unit, daily) != null
        }

        suspend fun checkIfConversionAlreadyExists(context : Context, ingredient : Int, unit1 : Int, unit2 : Int, includeInCalculation : Boolean) : Boolean {
            val db = DatabaseClient(context).getDB()
            return db.conversionDao().getByParameters(ingredient, unit1, unit2, includeInCalculation) != null
        }
    }
}