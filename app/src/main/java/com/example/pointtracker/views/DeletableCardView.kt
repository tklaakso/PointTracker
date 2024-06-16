package com.example.pointtracker.views

import android.content.Context
import android.view.LayoutInflater
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.example.pointtracker.R
import com.google.android.material.floatingactionbutton.FloatingActionButton

class DeletableCardView(context: Context) : CardView(context) {

    private var cardText : TextView? = null
    private var deleteButton : FloatingActionButton? = null

    init {
        initLayout()
    }

    private fun initLayout() {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.deletable_card_view, this, true)
        cardText = findViewById(R.id.cardText)
        deleteButton = findViewById(R.id.deleteButton)
    }

    fun setCardText(text: String) {
        cardText?.text = text
    }

    fun setOnDeleteListener(listener: OnClickListener) {
        deleteButton?.setOnClickListener(listener)
    }
}