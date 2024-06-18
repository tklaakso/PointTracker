package com.example.pointtracker.activity.view

import android.content.Intent
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.example.pointtracker.activity.MainActivity
import com.example.pointtracker.activity.create.NewIngredientActivity
import com.example.pointtracker.data.DatabaseClient
import com.example.pointtracker.data.PointDatabase
import com.example.pointtracker.util.Util
import com.example.pointtracker.views.DeletableCardView
import kotlinx.coroutines.launch

class IngredientActivity : InteractiveListViewActivity("Ingredients", MainActivity::class.java, NewIngredientActivity::class.java) {
    override fun populateItems(layout: LinearLayout) {
        val activity = this
        lifecycleScope.launch {
            val db: PointDatabase = DatabaseClient(applicationContext).getDB()
            val ingredients = db.ingredientDao().getAll()
            val viewsToAdd = mutableListOf<DeletableCardView>()
            for (ingredient in ingredients) {
                if (!itemFilter(ingredient.name))
                    continue
                val cardView = DeletableCardView(activity)
                cardView.setCardText(ingredient.name)
                cardView.setOnClickListener {
                    if (ingredient.deletable) {
                        val intent = Intent(activity, NewIngredientActivity::class.java)
                        intent.putExtra("id", ingredient.id)
                        intent.putExtra("name", ingredient.name)
                        if (ingredient.parent != null)
                            intent.putExtra("parent", ingredient.parent)
                        startActivity(intent)
                    }
                    else {
                        Util.buildErrorDialog(activity, "You may not edit a preset ingredient")
                    }
                }
                cardView.setOnDeleteListener {
                    if (ingredient.deletable) {
                        Util.buildWarningDialog(this@IngredientActivity, "Are you sure you want to delete this item?") { _, _ ->
                            lifecycleScope.launch {
                                db.ingredientDao().delete(ingredient)
                                populateItems(layout)
                            }
                        }
                    }
                    else {
                        AlertDialog.Builder(activity)
                            .setTitle("Error")
                            .setMessage("You may not delete a preset ingredient")
                            .setIcon(android.R.drawable.ic_dialog_info)
                            .setPositiveButton(android.R.string.ok, null)
                            .show()
                    }
                }
                viewsToAdd.add(cardView)
            }
            runOnUiThread {
                layout.removeAllViews()
                for (view in viewsToAdd) {
                    layout.addView(view)
                }
            }
        }
    }
}