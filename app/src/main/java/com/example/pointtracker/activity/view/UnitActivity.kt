package com.example.pointtracker.activity.view

import android.content.Intent
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.example.pointtracker.activity.MainActivity
import com.example.pointtracker.activity.create.NewRecipeActivity
import com.example.pointtracker.activity.create.NewUnitActivity
import com.example.pointtracker.data.DatabaseClient
import com.example.pointtracker.data.PointDatabase
import com.example.pointtracker.util.Util
import com.example.pointtracker.views.DeletableCardView
import kotlinx.coroutines.launch

class UnitActivity : InteractiveListViewActivity("Units", MainActivity::class.java, NewUnitActivity::class.java) {
    override fun populateItems(layout: LinearLayout) {
        val activity = this
        lifecycleScope.launch {
            val db: PointDatabase = DatabaseClient(applicationContext).getDB()
            val units = db.unitDao().getAll()
            layout.removeAllViews()
            for (unit in units) {
                if (!itemFilter(unit.name))
                    continue
                val cardView = DeletableCardView(activity)
                cardView.setCardText(unit.name)
                cardView.setOnClickListener {
                    if (unit.deletable) {
                        val intent = Intent(activity, NewUnitActivity::class.java)
                        intent.putExtra("id", unit.id)
                        intent.putExtra("name", unit.name)
                        intent.putExtra("showInAnalysis", unit.showInAnalysis)
                        intent.putExtra("promptConversion", unit.promptConversion)
                        startActivity(intent)
                    }
                    else {
                        AlertDialog.Builder(activity)
                            .setTitle("Error")
                            .setMessage("You may not edit a preset unit")
                            .setIcon(android.R.drawable.ic_dialog_info)
                            .setPositiveButton(android.R.string.ok, null)
                            .show()
                    }
                }
                cardView.setOnDeleteListener {
                    if (unit.deletable) {
                        Util.buildWarningDialog(this@UnitActivity, "Are you sure you want to delete this item?") { _, _ ->
                            lifecycleScope.launch {
                                db.unitDao().delete(unit)
                                populateItems(layout)
                            }
                        }
                    }
                    else {
                        AlertDialog.Builder(activity)
                            .setTitle("Error")
                            .setMessage("You may not delete a preset unit")
                            .setIcon(android.R.drawable.ic_dialog_info)
                            .setPositiveButton(android.R.string.ok, null)
                            .show()
                    }
                }
                layout.addView(cardView)
            }
        }
    }
}