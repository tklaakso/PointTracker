package com.example.pointtracker.activity.create

import android.app.Activity
import android.app.Dialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.pointtracker.R
import com.example.pointtracker.data.DatabaseClient
import com.example.pointtracker.data.PointDatabase
import com.example.pointtracker.data.entity.FoodItem
import com.example.pointtracker.data.entity.Ingredient
import com.example.pointtracker.data.entity.Recipe
import com.example.pointtracker.data.entity.Unit
import com.example.pointtracker.util.Util
import kotlinx.coroutines.launch

class FoodItemFragment(private val newRecipeActivity: NewRecipeActivity, private val foodItem: FoodItem?) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val parentActivity = requireActivity()
            val builder = AlertDialog.Builder(it)
            val inflater = parentActivity.layoutInflater
            val view = inflater.inflate(R.layout.dialog_new_fooditem, null)
            builder.setView(view)
            val unitSearch : AutoCompleteTextView = view.findViewById(R.id.unitSearchBox)
            val ingredientSearch : AutoCompleteTextView = view.findViewById(R.id.ingredientSearchBox)
            val quantityBox = view.findViewById<EditText>(R.id.quantityBox)
            lifecycleScope.launch {
                val units = fetchUnits(parentActivity)
                val ingredients = fetchIngredients(parentActivity)
                unitSearch.setAdapter(ArrayAdapter(parentActivity, androidx.appcompat.R.layout.select_dialog_item_material, units))
                ingredientSearch.setAdapter(ArrayAdapter(parentActivity, androidx.appcompat.R.layout.select_dialog_item_material, ingredients))
                if (foodItem != null) {
                    val db : PointDatabase = DatabaseClient(parentActivity.applicationContext).getDB()
                    unitSearch.setText(db.unitDao().getById(foodItem.unit)!!.name)
                    ingredientSearch.setText(if (!foodItem.isRecipe) db.ingredientDao().getById(foodItem.ingredient!!)!!.name else db.recipeDao().getById(foodItem.recipe!!)!!.name)
                    quantityBox.setText(foodItem.quantity.toString())
                }
            }
            builder.setMessage("Edit Food Item")
                .setPositiveButton("Confirm"
                ) { _, _ ->
                    parentActivity.lifecycleScope.launch {
                        if (!validateInputs(parentActivity, unitSearch, ingredientSearch)) {
                            Util.buildErrorDialog(parentActivity, "One or more inputs are invalid")
                            return@launch
                        }
                        val db : PointDatabase = DatabaseClient(parentActivity.applicationContext).getDB()
                        val unit = unitSearch.text.toString()
                        val ingredient = ingredientSearch.text.toString()
                        val quantity = quantityBox.text.toString()
                        var isRecipe = false
                        if (db.ingredientDao().getByName(ingredient) == null)
                            isRecipe = true
                        val newFoodItem = FoodItem(
                            0,
                            quantity.toDouble(),
                            db.unitDao().getByName(unit)!!.id,
                            0,
                            isRecipe,
                            if (isRecipe) db.recipeDao().getByName(ingredient)!!.id else null,
                            if (!isRecipe) db.ingredientDao()
                                .getByName(ingredient)!!.id else null
                        )
                        if (foodItem != null) {
                            newRecipeActivity.deleteFoodItem(foodItem)
                        }
                        newRecipeActivity.onConfirmFoodItem(newFoodItem)
                        dismiss()
                    }
                }
                .setNegativeButton("Cancel"
                ) { _, _ ->
                    dismiss()
                }
            val dialog = builder.create()
            return dialog
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    private suspend fun validateInputs(parentActivity : Activity, unitSearch : AutoCompleteTextView, ingredientSearch : AutoCompleteTextView) : Boolean {
        val db : PointDatabase = DatabaseClient(parentActivity.applicationContext).getDB()
        try {
            db.unitDao().getByName(unitSearch.text.toString())!!
            if (!(db.ingredientDao().getByName(ingredientSearch.text.toString()) != null || db.recipeDao().getByName(ingredientSearch.text.toString()) != null))
                throw Throwable()
            return true
        } catch (e : Throwable) {
            return false
        }
    }

    private suspend fun fetchUnits(parentActivity: Activity) : List<String> {
        val db : PointDatabase = DatabaseClient(parentActivity.applicationContext).getDB()
        val units : List<Unit> = db.unitDao().getAll()
        return units.map { it.name }
    }

    private suspend fun fetchIngredients(parentActivity: Activity) : List<String> {
        val db : PointDatabase = DatabaseClient(parentActivity.applicationContext).getDB()
        val ingredients : List<Ingredient> = db.ingredientDao().getAll()
        val recipes : List<Recipe> = db.recipeDao().getAll()
        return ingredients.map { it.name } + recipes.map { it.name }
    }
}