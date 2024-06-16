package com.example.pointtracker.activity.view

import android.content.Intent
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import androidx.lifecycle.lifecycleScope
import com.example.pointtracker.activity.MainActivity
import com.example.pointtracker.activity.create.NewConversionActivity
import com.example.pointtracker.data.DatabaseClient
import com.example.pointtracker.data.PointDatabase
import com.example.pointtracker.util.Util
import com.example.pointtracker.views.DeletableCardView
import kotlinx.coroutines.launch

class ConversionActivity : InteractiveListViewActivity("Conversions", MainActivity::class.java, NewConversionActivity::class.java) {
    override fun populateItems(layout: LinearLayout) {
        val activity = this
        lifecycleScope.launch {
            val db: PointDatabase = DatabaseClient(applicationContext).getDB()
            val conversions = db.conversionDao().getAll()
            layout.removeAllViews()
            for (conversion in conversions) {
                val unit1Name = db.unitDao().getById(conversion.unit1)!!.name
                val unit2Name = db.unitDao().getById(conversion.unit2)!!.name
                val ingredientName = db.ingredientDao().getById(conversion.ingredient)!!.name
                val displayText = "${conversion.quantity1} $unit1Name $ingredientName -> ${conversion.quantity2} $unit2Name $ingredientName"
                if (!itemFilter(displayText))
                    continue
                val cardView = DeletableCardView(activity)
                displayText.also { cardView.setCardText(it) }
                cardView.setOnClickListener {
                    val intent = Intent(activity, NewConversionActivity::class.java)
                    intent.putExtra("id", conversion.id)
                    intent.putExtra("ingredient", conversion.ingredient)
                    intent.putExtra("unit1", conversion.unit1)
                    intent.putExtra("unit2", conversion.unit2)
                    intent.putExtra("quantity1", conversion.quantity1)
                    intent.putExtra("quantity2", conversion.quantity2)
                    intent.putExtra("includeInCalculation", conversion.includeInCalculation)
                    startActivity(intent)
                }
                cardView.setOnDeleteListener {
                    Util.buildWarningDialog(this@ConversionActivity, "Are you sure you want to delete this item?") { _, _ ->
                        lifecycleScope.launch {
                            db.conversionDao().delete(conversion)
                            populateItems(layout)
                        }
                    }
                }
                layout.addView(cardView)
            }
        }
    }
}