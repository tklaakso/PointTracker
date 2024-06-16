package com.example.pointtracker.activity.view

import android.content.Intent
import android.widget.LinearLayout
import androidx.lifecycle.lifecycleScope
import com.example.pointtracker.activity.MainActivity
import com.example.pointtracker.activity.create.NewLimitActivity
import com.example.pointtracker.activity.create.NewUnitActivity
import com.example.pointtracker.data.DatabaseClient
import com.example.pointtracker.data.PointDatabase
import com.example.pointtracker.util.Util
import com.example.pointtracker.views.DeletableCardView
import kotlinx.coroutines.launch

class LimitActivity : InteractiveListViewActivity("Limits", MainActivity::class.java, NewLimitActivity::class.java) {
    override fun populateItems(layout: LinearLayout) {
        val activity = this
        lifecycleScope.launch {
            val db: PointDatabase = DatabaseClient(applicationContext).getDB()
            val limits = db.limitDao().getAll()
            layout.removeAllViews()
            for (limit in limits) {
                val unitName = db.unitDao().getById(limit.unit)!!.name
                val timePeriod = if (limit.daily) "daily" else "weekly"
                val limitText = "${limit.amount} $unitName $timePeriod"
                if (!itemFilter(limitText))
                    continue
                val cardView = DeletableCardView(activity)
                limitText.also { cardView.setCardText(it) }
                cardView.setOnClickListener {
                    val intent = Intent(activity, NewLimitActivity::class.java)
                    intent.putExtra("id", limit.id)
                    intent.putExtra("amount", limit.amount)
                    intent.putExtra("unit", limit.unit)
                    intent.putExtra("daily", limit.daily)
                    startActivity(intent)
                }
                cardView.setOnDeleteListener {
                    Util.buildWarningDialog(this@LimitActivity, "Are you sure you want to delete this item?") { _, _ ->
                        lifecycleScope.launch {
                            db.limitDao().delete(limit)
                            populateItems(layout)
                        }
                    }
                }
                layout.addView(cardView)
            }
        }
    }
}