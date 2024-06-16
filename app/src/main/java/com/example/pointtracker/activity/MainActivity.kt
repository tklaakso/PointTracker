package com.example.pointtracker.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.pointtracker.activity.view.IngredientActivity
import com.example.pointtracker.R
import com.example.pointtracker.activity.view.ConversionActivity
import com.example.pointtracker.activity.view.LimitActivity
import com.example.pointtracker.activity.view.RecipeActivity
import com.example.pointtracker.activity.view.SettingsActivity
import com.example.pointtracker.activity.view.TrackerActivity
import com.example.pointtracker.activity.view.UnitActivity
import com.example.pointtracker.data.DatabaseClient
import com.example.pointtracker.data.DatabaseContainer
import com.example.pointtracker.data.entity.Ingredient
import com.example.pointtracker.data.entity.Unit
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var startForResult: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        startForResult = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            if (it.resultCode == RESULT_OK) {
                val uri = it.data?.data
                if (uri != null) {
                    val contentResolver = applicationContext.contentResolver
                    contentResolver.openInputStream(uri).use {
                        inputStream ->
                        val json = inputStream?.bufferedReader().use { res ->
                            res!!.readText()
                        }
                        lifecycleScope.launch {
                            Gson().fromJson(json, DatabaseContainer::class.java).setContext(applicationContext).writeToDB()
                        }
                    }
                }
            }
        }
        lifecycleScope.launch {
            val db = DatabaseClient(applicationContext).getDB()
            val allIngredient = db.ingredientDao().getByName("all")
            if (allIngredient == null) {
                db.ingredientDao().insert(Ingredient(0, "all", null, false, null, null))
            }
            val wholeUnit = db.unitDao().getByName("whole")
            val portionUnit = db.unitDao().getByName("portion")
            if (wholeUnit == null) {
                db.unitDao().insert(Unit(0, "whole",
                    showInAnalysis = false,
                    promptConversion = false,
                    deletable = false
                ))
            }
            if (portionUnit == null) {
                db.unitDao().insert(Unit(0, "portion",
                    showInAnalysis = false,
                    promptConversion = false,
                    deletable = false
                ))
            }
        }
    }

    fun recipesButtonClicked(view: View) {
        val intent = Intent(this, RecipeActivity::class.java)
        startActivity(intent)
    }

    fun ingredientsButtonClicked(view: View) {
        val intent = Intent(this, IngredientActivity::class.java)
        startActivity(intent)
    }

    fun conversionsButtonClicked(view: View) {
        val intent = Intent(this, ConversionActivity::class.java)
        startActivity(intent)
    }

    fun unitsButtonClicked(view: View) {
        val intent = Intent(this, UnitActivity::class.java)
        startActivity(intent)
    }

    fun limitsButtonClicked(view: View) {
        val intent = Intent(this, LimitActivity::class.java)
        startActivity(intent)
    }

    fun trackerButtonClicked(view: View) {
        val intent = Intent(this, TrackerActivity::class.java)
        startActivity(intent)
    }

    fun importButtonClicked(view: View) {
        val chooseFile = Intent(Intent.ACTION_OPEN_DOCUMENT)
        chooseFile.type = "application/json"
        startForResult.launch(chooseFile)
    }

    fun exportButtonClicked(view: View) {
        lifecycleScope.launch {
            val json = Gson().toJson(DatabaseContainer(applicationContext).populateFromDB())
            val exportFile = File(applicationContext.filesDir, "export.json")
            exportFile.writeText(json)
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "application/json"
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(applicationContext, "com.example.pointtracker.fileprovider", exportFile))
            startActivity(Intent.createChooser(intent, "Share"))
        }
    }

    fun settingsButtonClicked(view: View) {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }
}