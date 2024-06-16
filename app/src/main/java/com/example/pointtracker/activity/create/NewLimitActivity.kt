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
import com.example.pointtracker.activity.view.IngredientActivity
import com.example.pointtracker.activity.view.LimitActivity
import com.example.pointtracker.data.DatabaseClient
import com.example.pointtracker.data.PointDatabase
import com.example.pointtracker.data.entity.Ingredient
import com.example.pointtracker.data.entity.Limit
import com.example.pointtracker.data.entity.Unit
import com.example.pointtracker.util.Util
import kotlinx.coroutines.launch

class NewLimitActivity : AppCompatActivity() {

    private var original : Limit? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_new_limit)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val limitUnitBox = findViewById<AutoCompleteTextView>(R.id.limitUnitBox)
        lifecycleScope.launch {
            val units = fetchUnits()
            limitUnitBox.setAdapter(ArrayAdapter(this@NewLimitActivity, androidx.appcompat.R.layout.select_dialog_item_material, units))
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
                original = db.limitDao().getById(extras.getInt("id"))
            }
            if (extras.containsKey("unit")) {
                findViewById<AutoCompleteTextView>(R.id.limitUnitBox).setText(db.unitDao().getById(extras.getInt("unit"))!!.name)
            }
            if (extras.containsKey("amount")) {
                findViewById<EditText>(R.id.limitAmountBox).setText(extras.getDouble("amount").toString())
            }
            if (extras.containsKey("daily")) {
                if (extras.getBoolean("daily")) {
                    findViewById<RadioGroup>(R.id.timespanRadioGroup).check(R.id.dailyRadioButton)
                }
                else {
                    findViewById<RadioGroup>(R.id.timespanRadioGroup).check(R.id.weeklyRadioButton)
                }
            }
        }
    }

    private suspend fun fetchUnits() : List<String> {
        val db : PointDatabase = DatabaseClient(applicationContext).getDB()
        val units : List<Unit> = db.unitDao().getAll()
        return units.map { it.name }
    }

    private suspend fun validateInputs() : Boolean {
        val db = DatabaseClient(applicationContext).getDB()
        val limitUnitBox = findViewById<AutoCompleteTextView>(R.id.limitUnitBox)
        val limitAmountBox = findViewById<EditText>(R.id.limitAmountBox)
        try {
            db.unitDao().getByName(limitUnitBox.text.toString())!!
            limitAmountBox.text.toString().toDouble()
            return true
        }
        catch (e : Throwable) {
            return false
        }
    }

    fun onConfirm(view : View) {
        val limitUnitBox = findViewById<AutoCompleteTextView>(R.id.limitUnitBox)
        val limitAmountBox = findViewById<EditText>(R.id.limitAmountBox)
        val timespanRadioGroup = findViewById<RadioGroup>(R.id.timespanRadioGroup)
        lifecycleScope.launch {
            if (!validateInputs()) {
                Util.buildErrorDialog(this@NewLimitActivity, "One or more inputs are invalid")
                return@launch
            }
            val db = DatabaseClient(applicationContext).getDB()
            if ((original == null || original!!.unit != db.unitDao().getByName(limitUnitBox.text.toString())!!.id || original!!.daily != (timespanRadioGroup.checkedRadioButtonId == R.id.dailyRadioButton)) && Util.checkIfLimitAlreadyExists(this@NewLimitActivity, db.unitDao().getByName(limitUnitBox.text.toString())!!.id, timespanRadioGroup.checkedRadioButtonId == R.id.dailyRadioButton)) {
                Util.buildErrorDialog(this@NewLimitActivity, "A limit for this unit and timespan already exists")
                return@launch
            }
            val limitDao = db.limitDao()
            val limitUnit = db.unitDao().getByName(limitUnitBox.text.toString())
            if (original != null) {
                limitDao.update(Limit(original!!.id, limitUnit!!.id, limitAmountBox.text.toString().toDouble(), timespanRadioGroup.checkedRadioButtonId == R.id.dailyRadioButton))
            }
            else{
                limitDao.insert(Limit(0, limitUnit!!.id, limitAmountBox.text.toString().toDouble(), timespanRadioGroup.checkedRadioButtonId == R.id.dailyRadioButton))
            }
            val intent = Intent(this@NewLimitActivity, LimitActivity::class.java)
            startActivity(intent)
        }
    }
}