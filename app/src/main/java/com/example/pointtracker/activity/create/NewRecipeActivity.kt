package com.example.pointtracker.activity.create

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.pointtracker.R
import com.example.pointtracker.activity.view.RecipeActivity
import com.example.pointtracker.data.DatabaseClient
import com.example.pointtracker.data.PointDatabase
import com.example.pointtracker.data.entity.Conversion
import com.example.pointtracker.data.entity.FoodItem
import com.example.pointtracker.data.entity.Ingredient
import com.example.pointtracker.data.entity.Recipe
import com.example.pointtracker.data.entity.Unit
import com.example.pointtracker.util.RecipeAnalyzer
import com.example.pointtracker.util.Util
import com.example.pointtracker.views.DeletableCardView
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class NewRecipeActivity : AppCompatActivity() {

    private var foodItems = mutableListOf<FoodItem>()
    private val deletedFoodItems = mutableListOf<FoodItem>()
    private val foodItemMapping = mutableMapOf<FoodItem, DeletableCardView>()

    private var original : Recipe? = null

    private var scale = 1.0
    private var portions = 1.0

    private val analysisMutex : Mutex = Mutex()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_new_recipe)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val scaleBox = findViewById<EditText>(R.id.scaleBox)
        val portionsBox = findViewById<EditText>(R.id.portionsBox)
        scaleBox.setText(scale.toString())
        portionsBox.setText(portions.toString())
        setFromExtras()
        scaleBox.addTextChangedListener(object : TextWatcher{
            override fun afterTextChanged(s: Editable?) {
                try {
                    scale = scaleBox.text.toString().toDouble()
                }
                catch (e : NumberFormatException) {
                    return
                }
                if (scale < 0) {
                    scale = 1.0
                    scaleBox.setText(scale.toString())
                }
                lifecycleScope.launch {
                    refreshAnalysis()
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        })
        portionsBox.addTextChangedListener(object : TextWatcher{
            override fun afterTextChanged(s: Editable?) {
                try {
                    portions = portionsBox.text.toString().toDouble()
                }
                catch (e : NumberFormatException) {
                    return
                }
                if (portions <= 0) {
                    portions = 1.0
                    portionsBox.setText(portions.toString())
                }
                lifecycleScope.launch {
                    refreshAnalysis()
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        })
        val finalWeightUnitBox = findViewById<AutoCompleteTextView>(R.id.finalWeightUnit)
        lifecycleScope.launch {
            val units = fetchUnits()
            val adapter = ArrayAdapter(this@NewRecipeActivity, androidx.appcompat.R.layout.select_dialog_item_material, units)
            finalWeightUnitBox.setAdapter(adapter)
        }
    }

    private suspend fun fetchUnits() : List<String> {
        val db : PointDatabase = DatabaseClient(applicationContext).getDB()
        val units : List<Unit> = db.unitDao().getAll()
        return units.map { it.name }
    }

    private fun setFromExtras() {
        if (intent.extras == null)
            return
        lifecycleScope.launch {
            val db = DatabaseClient(applicationContext).getDB()
            val extras: Bundle = intent.extras!!
            if (extras.containsKey("id")) {
                original = db.recipeDao().getById(extras.getInt("id"))
            }
            if (extras.containsKey("scale")) {
                scale = extras.getDouble("scale")
                findViewById<EditText>(R.id.scaleBox).setText(scale.toString())
            }
            if (extras.containsKey("portions")) {
                portions = extras.getDouble("portions")
                findViewById<EditText>(R.id.portionsBox).setText(portions.toString())
            }
            if (extras.containsKey("name")) {
                findViewById<EditText>(R.id.recipeNameBox).setText(extras.getString("name"))
                fetchFoodItems(original!!.id)
            }
            if (extras.containsKey("finalWeightAmount")) {
                findViewById<EditText>(R.id.finalWeightAmount).setText(extras.getDouble("finalWeightAmount").toString())
            }
            if (extras.containsKey("finalWeightUnit")) {
                val unit = db.unitDao().getById(extras.getInt("finalWeightUnit"))!!
                findViewById<AutoCompleteTextView>(R.id.finalWeightUnit).setText(unit.name)
            }
        }
    }

    private fun fetchFoodItems(id : Int) {
        lifecycleScope.launch {
            val db = DatabaseClient(applicationContext).getDB()
            val foodItemDao = db.foodItemDao()
            foodItems = foodItemDao.getByRecipe(id).toMutableList()
            refreshFoodItems()
            refreshAnalysis()
        }
    }

    private suspend fun refreshFoodItems() {
        val db = DatabaseClient(applicationContext).getDB()
        val foodItemsList = findViewById<LinearLayout>(R.id.foodItemsList)
        foodItemsList.removeAllViews()
        for (foodItem in foodItems) {
            val unitName = db.unitDao().getById(foodItem.unit)!!.name
            val ingredientName = if (foodItem.isRecipe) db.recipeDao().getById(foodItem.recipe!!)!!.name else db.ingredientDao().getById(foodItem.ingredient!!)!!.name
            val cardView = DeletableCardView(this@NewRecipeActivity)
            foodItemMapping[foodItem] = cardView
            val cardText = "${foodItem.quantity} $unitName $ingredientName".also { cardView.setCardText(it) }
            cardText.also { cardView.setCardText(it) }
            cardView.setOnClickListener {
                val fragment = FoodItemFragment(this@NewRecipeActivity, foodItem)
                fragment.show(supportFragmentManager, "FoodItemFragment")
            }
            cardView.setOnDeleteListener {
                Util.buildWarningDialog(this@NewRecipeActivity, "Are you sure you want to delete this item?") { _, _ ->
                    lifecycleScope.launch {
                        deleteFoodItem(foodItem)
                        refreshAnalysis()
                    }
                }
            }
            foodItemsList.addView(cardView)
        }
    }

    private suspend fun refreshAnalysis() {
        analysisMutex.lock()
        val analysisList = findViewById<LinearLayout>(R.id.analysisList)
        val unitsMap = RecipeAnalyzer.analyzeFoodItems(applicationContext, scale, portions, foodItems)
        val db = DatabaseClient(applicationContext).getDB()
        val viewsToAdd = mutableListOf<TextView>()
        for ((unit, amount) in unitsMap) {
            val unitObj = db.unitDao().getById(unit)!!
            val unitText = unitObj.name
            if (!unitObj.showInAnalysis)
                continue
            if (amount >= 0) {
                val textView = TextView(this@NewRecipeActivity)
                "$unitText: ${String.format("%.2f", amount)}".also { textView.text = it }
                viewsToAdd.add(textView)
            } else {
                val ingredientId = -amount.toInt() - 1
                val ingredient = db.ingredientDao().getById(ingredientId)!!
                val ingredientText = ingredient.name
                val textView = TextView(this@NewRecipeActivity)
                "$unitText: Missing conversion for $ingredientText".also {
                    textView.text = it
                }
                viewsToAdd.add(textView)
            }
        }
        runOnUiThread {
            analysisList.removeAllViews()
            for (view in viewsToAdd) {
                analysisList.addView(view)
            }
            analysisMutex.unlock()
        }
    }

    fun deleteFoodItem(foodItem: FoodItem) {
        if (foodItem.id != 0)
            deletedFoodItems.add(foodItem)
        val foodItemsList = findViewById<LinearLayout>(R.id.foodItemsList)
        if (foodItems.contains(foodItem))
            foodItems.remove(foodItem)
        if (foodItemMapping.containsKey(foodItem)) {
            val cardView = foodItemMapping[foodItem]!!
            runOnUiThread {
                foodItemsList.removeView(cardView)
            }
            foodItemMapping.remove(foodItem)
        }
    }

    fun onAddFoodItem(view: View) {
        val fragment = FoodItemFragment(this, null)
        fragment.show(supportFragmentManager, "FoodItemFragment")
    }

    fun onConfirmFoodItem(foodItem: FoodItem) {
        foodItems.add(foodItem)
        lifecycleScope.launch {
            val db = DatabaseClient(applicationContext).getDB()
            val foodItemsList = findViewById<LinearLayout>(R.id.foodItemsList)
            val unitName = db.unitDao().getById(foodItem.unit)!!.name
            val ingredientName = if (foodItem.isRecipe) db.recipeDao().getById(foodItem.recipe!!)!!.name else db.ingredientDao().getById(foodItem.ingredient!!)!!.name
            val cardView = DeletableCardView(this@NewRecipeActivity)
            foodItemMapping[foodItem] = cardView
            "${foodItem.quantity} $unitName $ingredientName".also { cardView.setCardText(it) }
            cardView.setOnClickListener {
                val fragment = FoodItemFragment(this@NewRecipeActivity, foodItem)
                fragment.show(supportFragmentManager, "FoodItemFragment")
            }
            cardView.setOnDeleteListener {
                Util.buildWarningDialog(this@NewRecipeActivity, "Are you sure you want to delete this item?") { _, _ ->
                    lifecycleScope.launch {
                        deleteFoodItem(foodItem)
                        refreshAnalysis()
                    }
                }
            }
            runOnUiThread {
                foodItemsList.addView(cardView)
            }
            refreshAnalysis()
        }
    }

    private suspend fun validateInputs() : Boolean {
        val db = DatabaseClient(applicationContext).getDB()
        val recipeNameBox = findViewById<EditText>(R.id.recipeNameBox)
        val finalWeightAmountBox = findViewById<EditText>(R.id.finalWeightAmount)
        val finalWeightUnitBox = findViewById<AutoCompleteTextView>(R.id.finalWeightUnit)
        try {
            if (recipeNameBox.text.isEmpty())
                throw Throwable()
            if (finalWeightAmountBox.text.isNotEmpty())
                finalWeightAmountBox.text.toString().toDouble()
            if (finalWeightUnitBox.text.isNotEmpty())
                db.unitDao().getByName(finalWeightUnitBox.text.toString())!!
            return true
        }
        catch (e : Throwable) {
            return false
        }
    }

    fun onConfirm(view: View) {
        val recipeNameBox = findViewById<EditText>(R.id.recipeNameBox)
        val finalWeightAmountBox = findViewById<EditText>(R.id.finalWeightAmount)
        val finalWeightUnitBox = findViewById<AutoCompleteTextView>(R.id.finalWeightUnit)
        var finalWeightAmount : Double? = null
        var finalWeightUnit : Int? = null
        lifecycleScope.launch {
            if (!validateInputs()) {
                Util.buildErrorDialog(this@NewRecipeActivity, "One or more inputs are invalid")
                return@launch
            }
            if ((original == null || original!!.name != recipeNameBox.text.toString()) && Util.checkIfRecipeIngredientNameInUse(applicationContext, recipeNameBox.text.toString())) {
                Util.buildErrorDialog(this@NewRecipeActivity, "A recipe or ingredient with that name already exists")
                return@launch
            }
            val db = DatabaseClient(applicationContext).getDB()
            val recipeDao = db.recipeDao()
            val unitDao = db.unitDao()
            if (finalWeightAmountBox.text.isNotEmpty()) {
                finalWeightAmount = finalWeightAmountBox.text.toString().toDouble()
            }
            if (finalWeightUnitBox.text.isNotEmpty()) {
                finalWeightUnit = unitDao.getByName(finalWeightUnitBox.text.toString())!!.id
            }
            if (original != null) {
                recipeDao.update(Recipe(original!!.id, recipeNameBox.text.toString(), scale, portions, finalWeightAmount, finalWeightUnit))
            }
            else {
                recipeDao.insert(Recipe(0, recipeNameBox.text.toString(), scale, portions, finalWeightAmount, finalWeightUnit))
            }
            val recipe = recipeDao.getByName(recipeNameBox.text.toString())
            for (deletedFoodItem in deletedFoodItems) {
                db.foodItemDao().delete(deletedFoodItem)
            }
            for (foodItem in foodItems) {
                foodItem.constituent = recipe!!.id
                db.foodItemDao().insert(foodItem)
            }
            val intent = Intent(this@NewRecipeActivity, RecipeActivity::class.java)
            startActivity(intent)
        }
    }
}