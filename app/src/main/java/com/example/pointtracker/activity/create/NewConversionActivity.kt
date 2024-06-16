package com.example.pointtracker.activity.create

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.CheckBox
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.pointtracker.R
import com.example.pointtracker.activity.view.ConversionActivity
import com.example.pointtracker.data.DatabaseClient
import com.example.pointtracker.data.PointDatabase
import com.example.pointtracker.data.entity.Conversion
import com.example.pointtracker.data.entity.Ingredient
import com.example.pointtracker.data.entity.Unit
import com.example.pointtracker.util.Util
import kotlinx.coroutines.launch

class NewConversionActivity : AppCompatActivity() {

    private var original : Conversion? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_new_conversion)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val conversionIngredientBox = findViewById<AutoCompleteTextView>(R.id.conversionIngredientBox)
        val conversionUnit1Box = findViewById<AutoCompleteTextView>(R.id.conversionUnit1Box)
        val conversionUnit2Box = findViewById<AutoCompleteTextView>(R.id.conversionUnit2Box)
        lifecycleScope.launch {
            val units = fetchUnits()
            val ingredients = fetchIngredients()
            conversionIngredientBox.setAdapter(ArrayAdapter(this@NewConversionActivity, androidx.appcompat.R.layout.select_dialog_item_material, ingredients))
            conversionUnit1Box.setAdapter(ArrayAdapter(this@NewConversionActivity, androidx.appcompat.R.layout.select_dialog_item_material, units))
            conversionUnit2Box.setAdapter(ArrayAdapter(this@NewConversionActivity, androidx.appcompat.R.layout.select_dialog_item_material, units))
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
                original = db.conversionDao().getById(extras.getInt("id"))
            }
            if (extras.containsKey("ingredient")) {
                val ingredientName = db.ingredientDao().getById(extras.getInt("ingredient"))!!.name
                findViewById<EditText>(R.id.conversionIngredientBox).setText(ingredientName)
            }
            if (extras.containsKey("unit1")) {
                val unitName = db.unitDao().getById(extras.getInt("unit1"))!!.name
                findViewById<AutoCompleteTextView>(R.id.conversionUnit1Box).setText(
                    unitName
                )
            }
            if (extras.containsKey("unit2")) {
                val unitName = db.unitDao().getById(extras.getInt("unit2"))!!.name
                findViewById<AutoCompleteTextView>(R.id.conversionUnit2Box).setText(
                    unitName
                )
            }
            if (extras.containsKey("quantity1")) {
                findViewById<EditText>(R.id.conversionQuantity1Box).setText(
                    extras.getDouble("quantity1").toString()
                )
            }
            if (extras.containsKey("quantity2")) {
                findViewById<EditText>(R.id.conversionQuantity2Box).setText(
                    extras.getDouble("quantity2").toString()
                )
            }
            if (extras.containsKey("includeInCalculation")) {
                findViewById<CheckBox>(R.id.includeInCalculationBox).isChecked =
                    extras.getBoolean("includeInCalculation")
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
        return ingredients.map { it.name }
    }

    private suspend fun validateInputs() : Boolean {
        val db : PointDatabase = DatabaseClient(applicationContext).getDB()
        val conversionIngredientBox = findViewById<AutoCompleteTextView>(R.id.conversionIngredientBox)
        val conversionUnit1Box = findViewById<AutoCompleteTextView>(R.id.conversionUnit1Box)
        val conversionUnit2Box = findViewById<AutoCompleteTextView>(R.id.conversionUnit2Box)
        val conversionQuantity1Box = findViewById<EditText>(R.id.conversionQuantity1Box)
        val conversionQuantity2Box = findViewById<EditText>(R.id.conversionQuantity2Box)
        try {
            if (conversionIngredientBox.text.isNotEmpty())
                db.ingredientDao().getByName(conversionIngredientBox.text.toString())!!
            db.unitDao().getByName(conversionUnit1Box.text.toString())!!
            db.unitDao().getByName(conversionUnit2Box.text.toString())!!
            conversionQuantity1Box.text.toString().toDouble()
            conversionQuantity2Box.text.toString().toDouble()
            return true
        }
        catch (e : Throwable) {
            return false
        }
    }

    fun onConfirm(view: View) {
        lifecycleScope.launch {
            if (!validateInputs()) {
                Util.buildErrorDialog(this@NewConversionActivity, "One or more inputs are invalid")
                return@launch
            }
            val db : PointDatabase = DatabaseClient(applicationContext).getDB()
            val conversionIngredientBox = findViewById<AutoCompleteTextView>(R.id.conversionIngredientBox)
            val includeInCalculationBox = findViewById<CheckBox>(R.id.includeInCalculationBox)
            val conversionUnit1Box = findViewById<AutoCompleteTextView>(R.id.conversionUnit1Box)
            val conversionUnit2Box = findViewById<AutoCompleteTextView>(R.id.conversionUnit2Box)
            val ingredientName = conversionIngredientBox.text.toString().ifEmpty { "all" }
            val ingredient = db.ingredientDao().getByName(ingredientName)
            val unit1 = db.unitDao().getByName(conversionUnit1Box.text.toString())
            val unit2 = db.unitDao().getByName(conversionUnit2Box.text.toString())
            val includeInCalculation = includeInCalculationBox.isChecked
            if ((original == null || original!!.ingredient != ingredient!!.id || original!!.unit1 != unit1!!.id || original!!.unit2 != unit2!!.id || original!!.includeInCalculation != includeInCalculation) && Util.checkIfConversionAlreadyExists(applicationContext, ingredient!!.id, unit1!!.id, unit2!!.id, includeInCalculation)) {
                Util.buildErrorDialog(this@NewConversionActivity, "Conversion with these parameters already exists")
                return@launch
            }
            val conversionQuantity1Box = findViewById<EditText>(R.id.conversionQuantity1Box)
            val conversionQuantity2Box = findViewById<EditText>(R.id.conversionQuantity2Box)
            val quantity1 = conversionQuantity1Box.text.toString().toDouble()
            val quantity2 = conversionQuantity2Box.text.toString().toDouble()
            if (original != null) {
                db.conversionDao().update(Conversion(original!!.id, ingredient!!.id, unit1!!.id, unit2!!.id, quantity1, quantity2, includeInCalculationBox.isChecked))
            }
            else {
                db.conversionDao().insert(Conversion(0, ingredient!!.id, unit1!!.id, unit2!!.id, quantity1, quantity2, includeInCalculationBox.isChecked))
            }
            val intent = Intent(this@NewConversionActivity, ConversionActivity::class.java)
            startActivity(intent)
        }
    }
}