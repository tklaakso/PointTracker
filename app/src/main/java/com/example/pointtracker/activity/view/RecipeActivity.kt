package com.example.pointtracker.activity.view

import android.content.Intent
import android.widget.LinearLayout
import androidx.lifecycle.lifecycleScope
import com.example.pointtracker.activity.MainActivity
import com.example.pointtracker.activity.create.NewRecipeActivity
import com.example.pointtracker.data.DatabaseClient
import com.example.pointtracker.data.PointDatabase
import com.example.pointtracker.util.Util
import com.example.pointtracker.views.DeletableCardView
import kotlinx.coroutines.launch

class RecipeActivity : InteractiveListViewActivity("Recipes", MainActivity::class.java, NewRecipeActivity::class.java) {
    override fun populateItems(layout: LinearLayout) {
        val activity = this
        lifecycleScope.launch {
            val db: PointDatabase = DatabaseClient(applicationContext).getDB()
            val recipes = db.recipeDao().getAll()
            val viewsToAdd = mutableListOf<DeletableCardView>()
            for (recipe in recipes) {
                if (!itemFilter(recipe.name))
                    continue
                val cardView = DeletableCardView(activity)
                cardView.setCardText(recipe.name)
                cardView.setOnClickListener {
                    val intent = Intent(activity, NewRecipeActivity::class.java)
                    intent.putExtra("id", recipe.id)
                    intent.putExtra("name", recipe.name)
                    intent.putExtra("scale", recipe.scale)
                    intent.putExtra("portions", recipe.portions)
                    if (recipe.finalWeightAmount != null)
                        intent.putExtra("finalWeightAmount", recipe.finalWeightAmount)
                    if (recipe.finalWeightUnit != null)
                        intent.putExtra("finalWeightUnit", recipe.finalWeightUnit)
                    startActivity(intent)
                }
                cardView.setOnDeleteListener {
                    Util.buildWarningDialog(this@RecipeActivity, "Are you sure you want to delete this item?") { _, _ ->
                        lifecycleScope.launch {
                            db.recipeDao().delete(recipe)
                            populateItems(layout)
                        }
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