package com.jaidev.seeaplayer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity

class History : AppCompatActivity() {
    private lateinit var dbHandler: MydbHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)
        supportActionBar?.hide()

        // Initialize dbHandler
        dbHandler = MydbHandler(this, null, null, 1)

        val sites: List<String> = dbHandler.databaseToString()
        if (sites.isNotEmpty()) {
            val myAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, sites)
            val myList = findViewById<ListView>(R.id.listViewHistory)
            myList.adapter = myAdapter
            // Set item click listener
            myList.setOnItemClickListener { parent, view, position, id ->
                val selectedUrl = sites[position]
                openUrlInBrowser(selectedUrl)
            }
        }
    }

    private fun openUrlInBrowser(url: String) {
        // Open the URL in a web browser
     val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }
}
