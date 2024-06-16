package com.example.pointtracker.activity.create

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AutoCompleteTextView
import android.widget.CheckBox
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.pointtracker.R
import com.example.pointtracker.activity.view.UnitActivity
import com.example.pointtracker.data.DatabaseClient
import com.example.pointtracker.data.entity.Ingredient
import com.example.pointtracker.data.entity.Unit
import com.example.pointtracker.util.Util
import kotlinx.coroutines.launch

class NewUnitActivity : AppCompatActivity() {

    private var original: Unit? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_new_unit)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
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
                original = db.unitDao().getById(extras.getInt("id"))
            }
            if (extras.containsKey("name")) {
                findViewById<EditText>(R.id.unitNameBox).setText(extras.getString("name"))
            }
            if (extras.containsKey("showInAnalysis")) {
                findViewById<CheckBox>(R.id.showInAnalysisBox).isChecked = extras.getBoolean("showInAnalysis")
            }
            if (extras.containsKey("promptConversion")) {
                findViewById<CheckBox>(R.id.promptConversionBox).isChecked = extras.getBoolean("promptConversion")
            }
        }
    }

    private suspend fun validateInputs() : Boolean {
        return findViewById<EditText>(R.id.unitNameBox).text.toString().isNotEmpty()
    }

    fun onConfirm(view: View) {
        val unitNameBox = findViewById<EditText>(R.id.unitNameBox)
        val showInAnalysisBox = findViewById<CheckBox>(R.id.showInAnalysisBox)
        val promptConversionBox = findViewById<CheckBox>(R.id.promptConversionBox)
        lifecycleScope.launch {
            if (!validateInputs()) {
                Util.buildErrorDialog(this@NewUnitActivity, "One or more inputs are invalid")
                return@launch
            }
            if ((original == null || original!!.name != unitNameBox.text.toString()) && Util.checkIfUnitNameInUse(applicationContext, unitNameBox.text.toString())) {
                Util.buildErrorDialog(this@NewUnitActivity, "A unit with that name already exists")
                return@launch
            }
            val db = DatabaseClient(applicationContext).getDB()
            val unitDao = db.unitDao()
            if (original != null) {
                unitDao.update(Unit(original!!.id, unitNameBox.text.toString(), showInAnalysisBox.isChecked, promptConversionBox.isChecked, true))
            }
            else {
                unitDao.insert(Unit(0, unitNameBox.text.toString(), showInAnalysisBox.isChecked, promptConversionBox.isChecked, true))
            }
            val intent = Intent(this@NewUnitActivity, UnitActivity::class.java)
            startActivity(intent)
        }
    }
}