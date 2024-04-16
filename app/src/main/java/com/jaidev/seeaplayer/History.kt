package com.jaidev.seeaplayer

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.jaidev.seeaplayer.browseFregment.BrowseFragment

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
            val reversedSites = sites.reversed() // Reverse the list (oldest first)
            val myAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1,  reversedSites)
            val myList = findViewById<ListView>(R.id.listViewHistory)
            myList.adapter = myAdapter
            // Set item click listener
            myList.setOnItemClickListener { parent, view, position, id ->
                val selectedUrl = sites[position]
                showLoadingToast()
                openUrlInBrowser(selectedUrl)
            }
        }
    }

    private fun openUrlInBrowser(query: String) {
        val browserFragment = BrowseFragment(urlNew = query)
        changeTab("Brave", browserFragment)
    }

    private fun showLoadingToast() {
        Toast.makeText(this, "Provided link is loading. You can go back.", Toast.LENGTH_LONG).show()
    }
}
