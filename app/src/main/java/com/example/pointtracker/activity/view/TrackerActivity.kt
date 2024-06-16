package com.example.pointtracker.activity.view

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.example.pointtracker.R
import com.example.pointtracker.activity.MainActivity
import com.example.pointtracker.activity.create.NewTrackerItemActivity
import com.example.pointtracker.data.DatabaseClient
import com.example.pointtracker.data.PointDatabase
import com.example.pointtracker.util.IngredientAmount
import com.example.pointtracker.util.RecipeAnalyzer
import com.example.pointtracker.util.TrackerUtil
import com.example.pointtracker.util.Util
import com.example.pointtracker.views.DeletableCardView
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Calendar

class TrackerActivity : InteractiveListViewActivity("Daily Tracker", MainActivity::class.java, NewTrackerItemActivity::class.java) {

    private val analysisMutex = Mutex()
    private var initialized = false

    override fun populateItems(layout: LinearLayout) {
        val activity = this
        lifecycleScope.launch {
            val db: PointDatabase = DatabaseClient(applicationContext).getDB()
            val trackerItems = db.trackerItemDao().getAll()
            val viewsToAdd = mutableListOf<DeletableCardView>()
            for (trackerItem in trackerItems) {
                if (trackerItem.date != TrackerUtil.getDayStartTime())
                    continue
                val unitName = db.unitDao().getById(trackerItem.unit)!!.name
                val ingredientName = if (trackerItem.isRecipe) db.recipeDao().getById(trackerItem.recipe!!)!!.name else db.ingredientDao().getById(trackerItem.ingredient!!)!!.name
                val trackerItemText = "${trackerItem.amount} $unitName $ingredientName"
                if (!itemFilter(trackerItemText))
                    continue
                val cardView = DeletableCardView(activity)
                trackerItemText.also { cardView.setCardText(it) }
                cardView.setOnClickListener {
                    val intent = Intent(activity, NewTrackerItemActivity::class.java)
                    intent.putExtra("id", trackerItem.id)
                    intent.putExtra("amount", trackerItem.amount)
                    intent.putExtra("unit", trackerItem.unit)
                    intent.putExtra("isRecipe", trackerItem.isRecipe)
                    intent.putExtra("recipe", trackerItem.recipe)
                    intent.putExtra("ingredient", trackerItem.ingredient)
                    intent.putExtra("date", trackerItem.date)
                    startActivity(intent)
                }
                cardView.setOnDeleteListener {
                    Util.buildWarningDialog(this@TrackerActivity, "Are you sure you want to delete this item?") { _, _ ->
                        lifecycleScope.launch {
                            db.trackerItemDao().delete(trackerItem)
                            populateItems(layout)
                        }
                    }
                }
                viewsToAdd.add(cardView)
            }
            runOnUiThread {
                layout.removeAllViews()
                for (view in viewsToAdd)
                    layout.addView(view)
            }
            if (!initialized) {
                initLayout()
                initialized = true
            }
            refreshAnalysis()
        }
    }

    private suspend fun refreshAnalysis() {
        analysisMutex.lock()
        val db: PointDatabase = DatabaseClient(applicationContext).getDB()
        val result = TrackerUtil.getConsumptionAnalysis(applicationContext)
        val dailyConsumptionOriginal = result.first.first
        val dailyConsumption = result.first.second
        val weeklyConsumptionOriginal = result.second.first
        val weeklyConsumption = result.second.second
        val missingConversions = result.third
        val consumedList = findViewById<LinearLayout>(R.id.consumedList)
        val remainingList = findViewById<LinearLayout>(R.id.remainingList)
        val consumedViewsToAdd = mutableListOf<TextView>()
        val remainingViewsToAdd = mutableListOf<TextView>()
        for ((unit, amount) in dailyConsumptionOriginal) {
            val unitText = db.unitDao().getById(unit)!!.name
            val consumedTextView = TextView(this@TrackerActivity)
            val remainingTextView = TextView(this@TrackerActivity)
            if (missingConversions.containsKey(unit)) {
                val missingConversion = db.ingredientDao().getById(missingConversions[unit]!!)!!
                "Error: Missing conversion for ${missingConversion.name} to $unitText".also { consumedTextView.text = it }
                "Error: Missing conversion for ${missingConversion.name} to $unitText".also { remainingTextView.text = it }
            }
            else {
                "${
                    String.format(
                        "%.2f",
                        amount - dailyConsumption[unit]!!
                    )
                } $unitText today".also { consumedTextView.text = it }
                "${
                    String.format(
                        "%.2f",
                        dailyConsumption[unit]!!
                    )
                } $unitText today".also {
                    remainingTextView.text = it
                }
            }
            consumedViewsToAdd.add(consumedTextView)
            remainingViewsToAdd.add(remainingTextView)
        }
        for ((unit, amount) in weeklyConsumptionOriginal) {
            val unitText = db.unitDao().getById(unit)!!.name
            val consumedTextView = TextView(this@TrackerActivity)
            val remainingTextView = TextView(this@TrackerActivity)
            if (missingConversions.containsKey(unit)) {
                val missingConversion = db.ingredientDao().getById(missingConversions[unit]!!)!!
                "Error: Missing conversion for ${missingConversion.name} to $unitText".also { consumedTextView.text = it }
                "Error: Missing conversion for ${missingConversion.name} to $unitText".also { remainingTextView.text = it }
            }
            else {
                "${
                    String.format(
                        "%.2f",
                        amount - weeklyConsumption[unit]!!
                    )
                } $unitText this week".also { consumedTextView.text = it }
                "${
                    String.format(
                        "%.2f",
                        weeklyConsumption[unit]!!
                    )
                } $unitText this week".also {
                    remainingTextView.text = it
                }
            }
            consumedViewsToAdd.add(consumedTextView)
            remainingViewsToAdd.add(remainingTextView)
        }
        runOnUiThread {
            consumedList.removeAllViews()
            remainingList.removeAllViews()
            for (view in consumedViewsToAdd)
                consumedList.addView(view)
            for (view in remainingViewsToAdd)
                remainingList.addView(view)
            analysisMutex.unlock()
        }
    }

    private fun initLayout() {
        val contentPane = findViewById<FrameLayout>(R.id.contentPane)
        val inflater = contentPane.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.tracker_content_pane, contentPane, true)
    }
}