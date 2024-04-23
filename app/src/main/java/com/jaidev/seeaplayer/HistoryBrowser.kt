package com.jaidev.seeaplayer

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewStub
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.jaidev.seeaplayer.allAdapters.HistoryAdapter
import com.jaidev.seeaplayer.browseFregment.BrowseFragment
import com.jaidev.seeaplayer.dataClass.HistoryItem
import com.jaidev.seeaplayer.dataClass.HistoryManager
import com.jaidev.seeaplayer.databinding.ActivityHistoryBrowserBinding

class HistoryBrowser : AppCompatActivity() , HistoryAdapter.ItemClickListener  {
    private var isEditTextVisible = false
    private lateinit var binding: ActivityHistoryBrowserBinding
    private lateinit var fileListAdapter: HistoryAdapter
    private lateinit var emptyStateLayout: ViewStub // Add reference to emptyStateLayout

    companion object{
        val historyItems: MutableList<HistoryItem> = mutableListOf()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBrowserBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        val historyList = HistoryManager.getHistoryList(this).toMutableList()
        fileListAdapter = HistoryAdapter(historyList , this )
        emptyStateLayout = findViewById(R.id.emptyStateLayout)
        binding.recyclerFileView.layoutManager = LinearLayoutManager(this)
        binding.recyclerFileView.adapter = fileListAdapter
        // Show empty state layout based on history list size
       updateEmptyStateVisibility()

        binding.imageButtonSearch.setOnClickListener {
            // Show editTextSearch
            binding.editTextSearch.visibility = View.VISIBLE
            binding.imageButtonSearch.visibility = View.GONE
            binding.editTextSearch.text?.clear()

            // Set focus to editTextSearch
            binding.editTextSearch.requestFocus()

            // Show the keyboard
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding.editTextSearch, InputMethodManager.SHOW_IMPLICIT)

            isEditTextVisible = true
        }



        binding.ClearData.setOnClickListener {
            showClearDataConfirmationDialog()
        }
        binding.editTextSearch.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                // Handle touch events on EditText
                handleEditTextTouch(v, event)
            }
            false
        }
        binding.editTextSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Not used in this case
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Not used in this case
            }

            override fun afterTextChanged(s: Editable?) {
                s?.let {
                    val searchText = s.toString().trim()
                    fileListAdapter.filterItems(searchText)
                }
            }
        })
    }

    private fun handleEditTextTouch(v: View, event: MotionEvent) {
        val bounds: Rect = binding.editTextSearch.compoundDrawablesRelative[2].bounds
        val drawableStart = binding.editTextSearch.compoundDrawablesRelative[0] // Index 0 for drawable start
        val bound = drawableStart?.bounds

        if (bound != null) {
            val touchArea = Rect(
                v.paddingLeft,
                v.paddingTop,
                v.paddingLeft + bounds.width(),
                v.height - v.paddingBottom
            )

            if (touchArea.contains(event.x.toInt(), event.y.toInt())) {
                // Hide editTextSearch
                hideEditText()
                fileListAdapter.filterItems("") // Empty string filters and shows all items

                return
            }
        }

        val touchArea = Rect(
            v.width - bounds.width() - v.paddingEnd,
            v.paddingTop,
            v.width - v.paddingEnd,
            v.height - v.paddingBottom
        )

        if (touchArea.contains(event.x.toInt(), event.y.toInt())) {
            // Clear the text in the EditText
            binding.editTextSearch.text?.clear()
            fileListAdapter.filterItems("") // Empty string filters and shows all items

        }
    }


    private fun hideEditText() {
        binding.editTextSearch.visibility = View.GONE
        binding.imageButtonSearch.visibility = View.VISIBLE
        // Hide keyboard
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.editTextSearch.windowToken, 0)
        isEditTextVisible = false


    }
    private fun updateEmptyStateVisibility() {
        if (fileListAdapter.itemCount == 0) {
            emptyStateLayout.visibility = View.VISIBLE
        } else {
            emptyStateLayout.visibility = View.GONE
        }
    }
    override fun onItemClick(historyItem: HistoryItem) {
        // Handle item click here
        val query = historyItem.url
        openUrlInBrowser(query)
        showLoadingToast()
    }
    private fun openUrlInBrowser(query: String) {
        val browserFragment = BrowseFragment(urlNew = query)
        changeTab("Brave", browserFragment)
    }

    private fun showLoadingToast() {
        Toast.makeText(this, "Provided link is loading. You can go back.", Toast.LENGTH_LONG).show()
    }


    private fun showClearDataConfirmationDialog() {
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.apply {
            setMessage("Are you sure you want to clear all the browsing data?\nIf you clear the browsing data, it will not be recoverable.")
            setPositiveButton("Clear Data") { dialog, _ ->
                clearBrowsingData()
                dialog.dismiss()
            }
            setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
        }

        // Create the AlertDialog
        val alertDialog = alertDialogBuilder.create()

        // Set text color for buttons
        alertDialog.setOnShowListener {
            val buttonPositive = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val buttonNegative = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)

            // Set cool blue color for positive button ("Clear Data")
            buttonPositive.setTextColor(ContextCompat.getColor(this@HistoryBrowser, R.color.cool_blue))

            // Set cool blue color for negative button ("Cancel")
            buttonNegative.setTextColor(ContextCompat.getColor(this@HistoryBrowser, R.color.cool_blue))
        }

        // Show the AlertDialog
        alertDialog.show()
    }


    @SuppressLint("NotifyDataSetChanged")
    private fun clearBrowsingData() {
        // Clear browsing history data
        HistoryManager.clearHistory(this)

        // Fetch updated history list
        val updatedHistoryList = HistoryManager.getHistoryList(this).toMutableList()

        // Log the size of the updated history list for debugging
        Log.d("HistoryBrowser", "Updated history list size: ${updatedHistoryList.size}")

        // Update historyItems with the updated list
        historyItems.clear()
        historyItems.addAll(updatedHistoryList)

        // Log the size of historyItems after update for debugging
        Log.d("HistoryBrowser", "Updated historyItems size: ${historyItems.size}")

        // Notify the adapter that the dataset has changed
        fileListAdapter.notifyDataSetChanged()

        // Show a toast message to indicate successful data clearance
        Toast.makeText(this, "Browsing data cleared successfully", Toast.LENGTH_SHORT).show()
    }

//    private fun updateEmptyStateVisibility() {
//        if (historyItems.isEmpty()) {
//            binding.emptyStateLayout.visibility = View.VISIBLE
//        } else {
//            binding.emptyStateLayout.visibility = View.GONE
//        }
//    }

}