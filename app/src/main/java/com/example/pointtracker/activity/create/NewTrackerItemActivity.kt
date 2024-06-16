package com.example.pointtracker.activity.create

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.RadioGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.pointtracker.R
import com.example.pointtracker.activity.view.TrackerActivity
import com.example.pointtracker.data.DatabaseClient
import com.example.pointtracker.data.PointDatabase
import com.example.pointtracker.data.entity.Ingredient
import com.example.pointtracker.data.entity.Limit
import com.example.pointtracker.data.entity.Recipe
import com.example.pointtracker.data.entity.TrackerItem
import com.example.pointtracker.data.entity.Unit
import com.example.pointtracker.util.IngredientAmount
import com.example.pointtracker.util.RecipeAnalyzer
import com.example.pointtracker.util.TrackerUtil
import com.example.pointtracker.util.Util
import kotlinx.coroutines.launch
import java.util.Calendar

class NewTrackerItemActivity : AppCompatActivity() {

    private var original : TrackerItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_new_tracker_item)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val trackerItemUnitBox = findViewById<AutoCompleteTextView>(R.id.trackerItemUnitBox)
        val trackerItemIngredientBox = findViewById<AutoCompleteTextView>(R.id.trackerItemIngredientBox)
        lifecycleScope.launch {
            val units = fetchUnits()
            val ingredients = fetchIngredients()
            trackerItemUnitBox.setAdapter(ArrayAdapter(this@NewTrackerItemActivity, androidx.appcompat.R.layout.select_dialog_item_material, units))
            trackerItemIngredientBox.setAdapter(ArrayAdapter(this@NewTrackerItemActivity, androidx.appcompat.R.layout.select_dialog_item_material, ingredients))
        }
        setFromExtras()
    }

    private fun setFromExtras() {
        if (intent.extras == null)
            return
        lifecycleScope.launch {
            val db = DatabaseClient(applicationContext).getDB()
            val extras: Bundle = intent.extras!!
            if (extras.containsKey("id")) {
                original = db.trackerItemDao().getById(extras.getInt("id"))
            }
            if (extras.containsKey("unit")) {
                findViewById<AutoCompleteTextView>(R.id.trackerItemUnitBox).setText(db.unitDao().getById(extras.getInt("unit"))!!.name)
            }
            if (extras.containsKey("amount")) {
                findViewById<EditText>(R.id.trackerItemAmountBox).setText(extras.getDouble("amount").toString())
            }
            if (extras.containsKey("ingredient")) {
                if (extras.containsKey("isRecipe") && extras.getBoolean("isRecipe")) {
                    findViewById<AutoCompleteTextView>(R.id.trackerItemIngredientBox).setText(db.recipeDao().getById(extras.getInt("recipe"))!!.name)
                }
                else {
                    findViewById<AutoCompleteTextView>(R.id.trackerItemIngredientBox).setText(db.ingredientDao().getById(extras.getInt("ingredient"))!!.name)
                }
            }
        }
    }

    private suspend fun fetchUnits() : List<String> {
        val db : PointDatabase = DatabaseClient(applicationContext).getDB()
        val units : List<Unit> = db.unitDao().getAll()
        return units.map { it.name }
    }

    private suspend fun fetchIngredients() : List<String> {
        val db : PointDatabase = DatabaseClient(applicationContext).getDB()
        val ingredients : List<Ingredient> = db.ingredientDao().getAll()
        val recipes : List<Recipe> = db.recipeDao().getAll()
        return ingredients.map { it.name } + recipes.map { it.name }
    }

    private fun getDayStartTime() : Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = System.currentTimeMillis()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun maximizeAmount(daily : Boolean) {
        val unitBox = findViewById<AutoCompleteTextView>(R.id.trackerItemUnitBox)
        val amountBox = findViewById<EditText>(R.id.trackerItemAmountBox)
        val ingredientBox = findViewById<AutoCompleteTextView>(R.id.trackerItemIngredientBox)
        if (ingredientBox.text.isEmpty())
            return
        if (unitBox.text.isEmpty())
            return
        lifecycleScope.launch {
            val db = DatabaseClient(applicationContext).getDB()
            val unit = db.unitDao().getByName(unitBox.text.toString())
            val ingredientText = ingredientBox.text.toString()
            var ingredientAnalysis : Map<Int, Double>? = null
            if (db.ingredientDao().getByName(ingredientText) == null) {
                val recipe = db.recipeDao().getByName(ingredientText)
                ingredientAnalysis = RecipeAnalyzer.analyzeRecipe(applicationContext, IngredientAmount(unit!!.id, recipe!!.id, true, 1.0), recipe)
            }
            else {
                val ingredient = db.ingredientDao().getByName(ingredientText)
                ingredientAnalysis = RecipeAnalyzer.analyzeIngredient(applicationContext, unit!!.id, 1.0, ingredient!!.id)
            }
            val result = TrackerUtil.getConsumptionAnalysis(applicationContext, excludeTrackerItems = if (original != null) listOf(original!!.id) else listOf())
            val dailyConsumption = result.first.second
            val weeklyConsumption = result.second.second
            var maxAmountDaily = Double.POSITIVE_INFINITY
            var maxAmountWeekly = Double.POSITIVE_INFINITY
            for ((unitId, amount) in dailyConsumption) {
                if (amount < 0)
                    continue
                if (ingredientAnalysis.containsKey(unitId) && ingredientAnalysis[unitId]!! >= 0) {
                    maxAmountDaily = maxAmountDaily.coerceAtMost((amount / ingredientAnalysis[unitId]!!) * ingredientAnalysis[unit.id]!!)
                }
            }
            if (daily) {
                maxAmountWeekly = 0.0
            }
            else {
                for ((unitId, amount) in weeklyConsumption) {
                    if (amount < 0)
                        continue
                    if (ingredientAnalysis.containsKey(unitId) && ingredientAnalysis[unitId]!! >= 0) {
                        maxAmountWeekly = maxAmountWeekly.coerceAtMost((amount / ingredientAnalysis[unitId]!!) * ingredientAnalysis[unit.id]!!)
                    }
                }
            }
            val maxAmount = maxAmountDaily + maxAmountWeekly
            if (maxAmount != Double.POSITIVE_INFINITY)
                amountBox.setText(String.format("%.2f", maxAmount))
        }
    }

    fun onMaxDailyButtonClick(view : View) {
        maximizeAmount(true)
    }

    fun onMaxTotalButtonClick(view : View) {
        maximizeAmount(false)
    }

    private suspend fun validateInputs() : Boolean {
        val db = DatabaseClient(applicationContext).getDB()
        val amountBox = findViewById<EditText>(R.id.trackerItemAmountBox)
        val unitBox = findViewById<AutoCompleteTextView>(R.id.trackerItemUnitBox)
        val ingredientBox = findViewById<AutoCompleteTextView>(R.id.trackerItemIngredientBox)
        try {
            amountBox.text.toString().toDouble()
            db.unitDao().getByName(unitBox.text.toString())!!
            if (!(db.ingredientDao().getByName(ingredientBox.text.toString()) != null || db.recipeDao().getByName(ingredientBox.text.toString()) != null))
                throw Throwable()
            return true
        }
        catch (e : Throwable) {
            return false
        }
    }

    fun onConfirm(view : View) {
        val trackerItemUnitBox = findViewById<AutoCompleteTextView>(R.id.trackerItemUnitBox)
        val trackerItemAmountBox = findViewById<EditText>(R.id.trackerItemAmountBox)
        val trackerItemIngredientBox = findViewById<AutoCompleteTextView>(R.id.trackerItemIngredientBox)
        lifecycleScope.launch {
            if (!validateInputs()) {
                Util.buildErrorDialog(this@NewTrackerItemActivity, "One or more inputs are invalid")
                return@launch
            }
            val db = DatabaseClient(applicationContext).getDB()
            val trackerItemDao = db.trackerItemDao()
            val trackerItemUnit = db.unitDao().getByName(trackerItemUnitBox.text.toString())
            val ingredientText = trackerItemIngredientBox.text.toString()
            val ingredientId : Int?
            var isRecipe = false
            if (db.ingredientDao().getByName(ingredientText) == null) {
                isRecipe = true
                ingredientId = db.recipeDao().getByName(ingredientText)!!.id
            }
            else {
                ingredientId = db.ingredientDao().getByName(ingredientText)!!.id
            }
            if (original != null) {
                trackerItemDao.update(TrackerItem(original!!.id, trackerItemUnit!!.id, trackerItemAmountBox.text.toString().toDouble(), isRecipe, if (isRecipe) ingredientId else null, if (isRecipe) null else ingredientId, getDayStartTime()))
            }
            else{
                trackerItemDao.insert(TrackerItem(0, trackerItemUnit!!.id, trackerItemAmountBox.text.toString().toDouble(), isRecipe, if (isRecipe) ingredientId else null, if (isRecipe) null else ingredientId, getDayStartTime()))
            }
            val intent = Intent(this@NewTrackerItemActivity, TrackerActivity::class.java)
            startActivity(intent)
        }
    }
}