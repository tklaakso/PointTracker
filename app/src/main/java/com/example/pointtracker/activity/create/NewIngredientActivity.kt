package com.example.pointtracker.activity.create

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.pointtracker.R
import com.example.pointtracker.activity.view.IngredientActivity
import com.example.pointtracker.data.DatabaseClient
import com.example.pointtracker.data.PointDatabase
import com.example.pointtracker.data.entity.Conversion
import com.example.pointtracker.data.entity.Ingredient
import com.example.pointtracker.data.entity.Unit
import com.example.pointtracker.util.Nutritionix
import com.example.pointtracker.util.NutritionixFoodItem
import com.example.pointtracker.util.Util
import com.google.gson.Gson
import kotlinx.coroutines.launch
import org.chromium.net.CronetException
import org.chromium.net.UrlRequest
import org.chromium.net.UrlResponseInfo
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.channels.WritableByteChannel


class NewIngredientActivity : AppCompatActivity() {

    private var original : Ingredient? = null

    private var conversionBoxes = mutableMapOf<Int, EditText>()
    private var perAmountBox : EditText? = null
    private var perUnitBox : AutoCompleteTextView? = null

    private lateinit var ingredientNameAdapter : ArrayAdapter<NutritionixFoodItem>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_new_ingredient)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val importFromNutritionixBox = findViewById<CheckBox>(R.id.importFromNutritionixBox)
        val ingredientNameBox : AutoCompleteTextView = findViewById(R.id.ingredientNameBox)
        ingredientNameBox.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                if (!importFromNutritionixBox.isChecked || ingredientNameBox.isPerformingCompletion)
                    return
                populateNutritionixSearchResults()
            }
        })
        ingredientNameBox.setOnItemClickListener { parent, view, position, id ->
            if (!importFromNutritionixBox.isChecked)
                return@setOnItemClickListener
            val selected = ingredientNameAdapter.getItem(position)
            if (selected!!.id.isEmpty()) {
                Nutritionix.getNutritionalInfoFromFoodName(applicationContext, selected.name) { result: Map<*, *> ->
                    setFromNixData(result)
                }
            }
            else {
                Nutritionix.getNutritionalInfoFromNixItemId(applicationContext, selected.id) { result: Map<*, *> ->
                    setFromNixData(result)
                }
            }
        }
        ingredientNameAdapter = ArrayAdapter(this, androidx.appcompat.R.layout.select_dialog_item_material, listOf<NutritionixFoodItem>())
        ingredientNameBox.setAdapter(ingredientNameAdapter)
        val parentTextView : AutoCompleteTextView = findViewById(R.id.parentTextView)
        val conversionsLayout : LinearLayout = findViewById(R.id.conversionsLayout)
        lifecycleScope.launch {
            val ingredients = fetchIngredients()
            parentTextView.setAdapter(ArrayAdapter(this@NewIngredientActivity, androidx.appcompat.R.layout.select_dialog_item_material, ingredients.map { it.name }))
            val allUnits = fetchUnits()
            val units = fetchShownUnits()
            if (units.isNotEmpty()) {
                val layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                for (unit in units) {
                    val conversion = LinearLayout(this@NewIngredientActivity)
                    conversion.orientation = LinearLayout.HORIZONTAL
                    val prefix = TextView(this@NewIngredientActivity)
                    val amountBox = EditText(this@NewIngredientActivity)
                    amountBox.inputType = android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL or android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
                    val unitName = TextView(this@NewIngredientActivity)
                    prefix.layoutParams = layoutParams
                    amountBox.layoutParams = layoutParams
                    unitName.layoutParams = layoutParams
                    "Contains ".also { prefix.text = it }
                    unit.name.also { unitName.text = it }
                    prefix.gravity = android.view.Gravity.CENTER
                    unitName.gravity = android.view.Gravity.CENTER
                    conversion.addView(prefix)
                    conversion.addView(amountBox)
                    conversionBoxes[unit.id] = amountBox
                    conversion.addView(unitName)
                    conversionsLayout.addView(conversion)
                }
                val perUnitPrefix = TextView(this@NewIngredientActivity)
                "Per ".also { perUnitPrefix.text = it }
                perAmountBox = EditText(this@NewIngredientActivity)
                perAmountBox!!.inputType = android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL or android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
                perAmountBox!!.layoutParams = layoutParams
                perUnitBox = AutoCompleteTextView(this@NewIngredientActivity)
                perUnitBox!!.setAdapter(ArrayAdapter(this@NewIngredientActivity, androidx.appcompat.R.layout.select_dialog_item_material, allUnits.map { it.name }))
                perUnitPrefix.layoutParams = layoutParams
                perUnitBox!!.layoutParams = layoutParams
                val perUnit = LinearLayout(this@NewIngredientActivity)
                perUnit.orientation = LinearLayout.HORIZONTAL
                perUnitPrefix.gravity = android.view.Gravity.CENTER
                perUnit.addView(perUnitPrefix)
                perUnit.addView(perAmountBox)
                perUnit.addView(perUnitBox)
                conversionsLayout.addView(perUnit)
            }
            setFromExtras()
        }
    }

    private fun setFromNixData(data : Map<*, *>) {
        val food = (data["foods"] as List<*>)[0] as Map<String, *>
        lifecycleScope.launch {
            val unitNamesToIds = fetchShownUnits().associateBy({ it.name }, { it.id })
            if (unitNamesToIds.isEmpty())
                return@launch
            if (unitNamesToIds.containsKey("calorie") && food.containsKey("nf_calories")) {
                conversionBoxes[unitNamesToIds["calorie"]!!]!!.setText(food["nf_calories"].toString())
            }
            if (unitNamesToIds.containsKey("protein") && food.containsKey("nf_protein")) {
                conversionBoxes[unitNamesToIds["protein"]!!]!!.setText(food["nf_protein"].toString())
            }
            if (unitNamesToIds.containsKey("saturated fat") && food.containsKey("nf_saturated_fat")) {
                conversionBoxes[unitNamesToIds["saturated fat"]!!]!!.setText(food["nf_saturated_fat"].toString())
            }
            if (unitNamesToIds.containsKey("fat") && food.containsKey("nf_total_fat")) {
                conversionBoxes[unitNamesToIds["fat"]!!]!!.setText(food["nf_total_fat"].toString())
            }
            if (unitNamesToIds.containsKey("carbohydrate") && food.containsKey("nf_total_carbohydrate")) {
                conversionBoxes[unitNamesToIds["carbohydrate"]!!]!!.setText(food["nf_total_carbohydrate"].toString())
            }
            if (unitNamesToIds.containsKey("sodium") && food.containsKey("nf_sodium")) {
                conversionBoxes[unitNamesToIds["sodium"]!!]!!.setText(food["nf_sodium"].toString())
            }
            if (unitNamesToIds.containsKey("fiber") && food.containsKey("nf_dietary_fiber")) {
                conversionBoxes[unitNamesToIds["fiber"]!!]!!.setText(food["nf_dietary_fiber"].toString())
            }
            if (unitNamesToIds.containsKey("sugar") && food.containsKey("nf_sugars")) {
                conversionBoxes[unitNamesToIds["sugar"]!!]!!.setText(food["nf_sugars"].toString())
            }
            if (unitNamesToIds.containsKey("potassium") && food.containsKey("nf_potassium")) {
                conversionBoxes[unitNamesToIds["potassium"]!!]!!.setText(food["nf_potassium"].toString())
            }
            if (unitNamesToIds.containsKey("g") && food.containsKey("serving_weight_grams")) {
                conversionBoxes[unitNamesToIds["g"]!!]!!.setText(food["serving_weight_grams"].toString())
            }
            val servingUnit = food["serving_unit"] as String
            if (unitNamesToIds.containsKey(servingUnit))
                conversionBoxes[unitNamesToIds[servingUnit]!!]!!.setText("")
            perUnitBox!!.setText(servingUnit)
            perAmountBox!!.setText(food["serving_qty"].toString())
        }
    }

    private fun setFromExtras() {
        if (intent.extras == null)
            return
        lifecycleScope.launch {
            val db = DatabaseClient(applicationContext).getDB()
            val extras: Bundle = intent.extras!!
            if (extras.containsKey("id")) {
                original = db.ingredientDao().getById(extras.getInt("id"))
            }
            if (extras.containsKey("name")) {
                findViewById<AutoCompleteTextView>(R.id.ingredientNameBox).setText(extras.getString("name"))
            }
            if (extras.containsKey("parent")) {
                findViewById<AutoCompleteTextView>(R.id.parentTextView).setText(db.ingredientDao().getById(extras.getInt("parent"))!!.name)
            }
            setFromConversions()
        }
    }

    private fun setFromConversions() {
        if (original == null)
            return
        lifecycleScope.launch {
            val db = DatabaseClient(applicationContext).getDB()
            val perUnit = original!!.perUnit
            val perUnitAmount = original!!.perUnitAmount
            val ingredient = original!!.id
            if (perUnit == null || perUnitAmount == null)
                return@launch
            for ((id, amountBox) in conversionBoxes) {
                val conversion = db.conversionDao().getByParameters(ingredient, perUnit, id, false) ?: continue
                amountBox.setText(conversion.quantity2.toString())
            }
            perUnitBox!!.setText(db.unitDao().getById(perUnit)!!.name)
            perAmountBox!!.setText(perUnitAmount.toString())
        }
    }

    private fun populateNutritionixSearchResults() {
        Nutritionix.getSearchResults(this, findViewById<AutoCompleteTextView>(R.id.ingredientNameBox).text.toString()) { result: Map<*, *> ->
            val common = result["common"] as List<*>
            val branded = result["branded"] as List<*>
            val nutritionixFoodItemsCommon = common.map { NutritionixFoodItem((it as Map<String, *>).getValue("food_name") as String, "") }
            val nutritionixFoodItemsBranded = branded.map { NutritionixFoodItem((it as Map<String, *>).getValue("brand_name_item_name") as String, it.getValue("nix_item_id") as String) }
            val ingredientNameBox = findViewById<AutoCompleteTextView>(R.id.ingredientNameBox)
            runOnUiThread {
                ingredientNameAdapter.clear()
                ingredientNameAdapter.addAll(nutritionixFoodItemsCommon + nutritionixFoodItemsBranded)
                ingredientNameAdapter.notifyDataSetChanged()
                ingredientNameBox.showDropDown()
            }
        }
    }

    private suspend fun fetchIngredients() : List<Ingredient> {
        val db : PointDatabase = DatabaseClient(applicationContext).getDB()
        val ingredients : List<Ingredient> = db.ingredientDao().getAll()
        return ingredients
    }

    private suspend fun fetchUnits() : List<Unit> {
        val db : PointDatabase = DatabaseClient(applicationContext).getDB()
        val units : List<Unit> = db.unitDao().getAll()
        return units
    }

    private suspend fun fetchShownUnits() : List<Unit> {
        val db : PointDatabase = DatabaseClient(applicationContext).getDB()
        val units : List<Unit> = db.unitDao().getAllFlaggedForConversion()
        return units
    }

    private suspend fun validateInputs() : Boolean {
        val db : PointDatabase = DatabaseClient(applicationContext).getDB()
        val ingredientNameBox = findViewById<AutoCompleteTextView>(R.id.ingredientNameBox)
        val parentTextView = findViewById<AutoCompleteTextView>(R.id.parentTextView)
        try {
            if (ingredientNameBox.text.isEmpty())
                throw Throwable()
            if (parentTextView.text.isNotEmpty())
                db.ingredientDao().getByName(parentTextView.text.toString())!!
            if (perUnitBox != null && perUnitBox!!.text.isNotEmpty())
                db.unitDao().getByName(perUnitBox!!.text.toString())!!
            if (perAmountBox != null && perAmountBox!!.text.isNotEmpty())
                perAmountBox!!.text.toString().toDouble()
            for ((_, amountBox) in conversionBoxes) {
                if (amountBox.text.isEmpty())
                    continue
                amountBox.text.toString().toDouble()
                if (!(perUnitBox != null && perAmountBox != null && perUnitBox!!.text.isNotEmpty() && perAmountBox!!.text.isNotEmpty()))
                    throw Throwable()
            }
            return true
        }
        catch (e : Throwable) {
            return false
        }
    }

    fun onConfirm(view : View) {
        val ingredientNameBox = findViewById<AutoCompleteTextView>(R.id.ingredientNameBox)
        val parentTextView = findViewById<AutoCompleteTextView>(R.id.parentTextView)
        lifecycleScope.launch {
            if (!validateInputs()) {
                Util.buildErrorDialog(this@NewIngredientActivity, "One or more inputs are invalid")
                return@launch
            }
            if ((original == null || original!!.name != ingredientNameBox.text.toString()) && Util.checkIfRecipeIngredientNameInUse(applicationContext, ingredientNameBox.text.toString())) {
                Util.buildErrorDialog(this@NewIngredientActivity, "A recipe or ingredient with that name already exists")
                return@launch
            }
            val db = DatabaseClient(applicationContext).getDB()
            val ingredientDao = db.ingredientDao()
            val unitDao = db.unitDao()
            val parent : Ingredient? = if (parentTextView.text.isNotEmpty()) {
                ingredientDao.getByName(parentTextView.text.toString())
            } else {
                ingredientDao.getByName("all")
            }
            val perUnitText = perUnitBox!!.text.toString().ifEmpty { null }
            if (original != null) {
                ingredientDao.update(Ingredient(original!!.id, ingredientNameBox.text.toString(), parent!!.id, true, if (perUnitText != null) unitDao.getByName(perUnitText)!!.id else null, perAmountBox!!.text.toString().ifEmpty { null }?.toDouble()))
            }
            else{
                ingredientDao.insert(Ingredient(0, ingredientNameBox.text.toString(), parent!!.id, true, if (perUnitText != null) unitDao.getByName(perUnitText)!!.id else null, perAmountBox!!.text.toString().ifEmpty { null }?.toDouble()))
            }
            val ingredient = ingredientDao.getByName(ingredientNameBox.text.toString())
            if (perUnitBox != null && perUnitBox!!.text.isNotEmpty() && perAmountBox != null && perAmountBox!!.text.isNotEmpty()) {
                val perUnit = unitDao.getByName(perUnitBox!!.text.toString())
                val perAmount = perAmountBox!!.text.toString().toDouble()
                for ((id, amountBox) in conversionBoxes) {
                    if (amountBox.text.isEmpty())
                        continue
                    val amount = amountBox.text.toString().toDouble()
                    val conversion = Conversion(0, ingredient!!.id, perUnit!!.id, id, perAmount, amount, false)
                    db.conversionDao().replaceByParameters(conversion)
                }
            }
            val intent = Intent(this@NewIngredientActivity, IngredientActivity::class.java)
            startActivity(intent)
        }
    }
}