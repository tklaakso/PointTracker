package com.example.pointtracker.activity.view

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.SearchView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.pointtracker.R

abstract class InteractiveListViewActivity(private val title : String, private val returnActivity : Class<*>, private val newActivity : Class<*>) : AppCompatActivity(), SearchView.OnQueryTextListener {

    private var searchString : String = ""
    protected val itemFilter : (String) -> Boolean = { it.contains(searchString, ignoreCase = true) }

    constructor() : this("", InteractiveListViewActivity::class.java, InteractiveListViewActivity::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_interactive_list_view)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = title
        onBackPressedDispatcher.addCallback(this@InteractiveListViewActivity, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val intent = Intent(this@InteractiveListViewActivity, returnActivity)
                startActivity(intent)
                finish()
            }
        })
        val searchBox = findViewById<SearchView>(R.id.searchBox)
        searchBox.setOnQueryTextListener(this)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val layout: LinearLayout = findViewById(R.id.itemsList)
        populateItems(layout)
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        searchString = newText ?: ""
        populateItems(findViewById(R.id.itemsList))
        return true
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        return true
    }

    abstract fun populateItems(layout: LinearLayout)

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun newItemButtonClicked(view: View) {
        val intent = Intent(this, newActivity)
        startActivity(intent)
    }
}