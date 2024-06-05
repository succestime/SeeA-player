package com.jaidev.seeaplayer.Subscription

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.databinding.ActivitySeeaEduProcessBinding

class SeeaEduProcess : AppCompatActivity() {
    private lateinit var binding: ActivitySeeaEduProcessBinding
    private var isTimeSelected = false
    private var isTimeHintRed = false
    private lateinit var seeaeduprocess: RelativeLayout
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        binding = ActivitySeeaEduProcessBinding.inflate(layoutInflater)
        supportActionBar?.hide()
        setContentView(binding.root)
        initializeBinding()

        seeaeduprocess = binding.seeAEduProcess
        setSwipeRefreshBackgroundColor()
    }

    private fun initializeBinding() {
        val items = listOf("Months", "Years")
        val itemsNull = listOf("Null")
        val itemsMonth = listOf("Month (1)", "Months (Quarterly)", "Months (Half - Yearly)")
        val itemsYear = listOf("Annual", "Biennial")
        val itemsQuantity = listOf("1", "2", "3", "4", "5", "6", "7", "8",
            "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20")

        val adapter = ArrayAdapter(this, R.layout.list_item_process, items)
        val adapterMonth = ArrayAdapter(this, R.layout.list_item_process, itemsMonth)
        val adapterYear = ArrayAdapter(this, R.layout.list_item_process, itemsYear)
        val adapterQuantity = ArrayAdapter(this, R.layout.list_item_process, itemsQuantity)
        val adapterNull = NonSelectableArrayAdapter(this, R.layout.list_item_process, itemsNull)

        binding.autoComplete.setAdapter(adapter)
        binding.timeComplete.setAdapter(adapterNull)
        binding.quantityComplete.setAdapter(adapterQuantity)
        // Default adapter

        binding.autoComplete.onItemClickListener = AdapterView.OnItemClickListener { _, _, i, _ ->
            if (isTimeSelected) {
                highlightBox(binding.timeComplete)
                setHintColor(binding.timeComplete, Color.RED)
                isTimeHintRed = true
                Toast.makeText(this, "Select again in the Membership days menu", Toast.LENGTH_LONG).show()
            }
            isTimeSelected = false // Reset the flag as we are changing the main selection

            val timeAdapter = when (items[i]) {
                "Months" -> adapterMonth
                "Years" -> adapterYear
                else -> adapterMonth // Default to monthly if nothing else
            }
            binding.timeComplete.setAdapter(timeAdapter)
        }

        binding.payButton.setOnClickListener {
            val autoText = binding.autoComplete.text.toString()
            val timeText = binding.timeComplete.text.toString()
            val quantityText = binding.quantityComplete.text.toString()

            if (autoText.isEmpty()) {
                highlightBox(binding.autoComplete)
                setHintColor(binding.autoComplete, Color.RED)
                Toast.makeText(this, "Select the Item", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            } else {
                resetBox(binding.autoComplete)
            }

            if (timeText.isEmpty() || !isTimeSelectionValid(autoText, timeText)) {
                highlightBox(binding.timeComplete)
                setHintColor(binding.timeComplete, Color.RED)
                isTimeHintRed = true
                Toast.makeText(this, "Select the correct Time", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            } else {
                resetBox(binding.timeComplete)
                isTimeHintRed = false
            }

            if (quantityText.isEmpty()) {
                highlightBox(binding.quantityComplete)
                setHintColor(binding.quantityComplete, Color.RED)
                Toast.makeText(this, "Select the Quantity", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            } else {
                resetBox(binding.quantityComplete)
            }


        }

        binding.timeComplete.onItemClickListener = AdapterView.OnItemClickListener { _, _, i, _ ->
            if (binding.autoComplete.text.toString().isEmpty()) {
                binding.timeComplete.setAdapter(adapterNull)
                Toast.makeText(this, "Please select an item first", Toast.LENGTH_SHORT).show()
            } else {
                isTimeSelected = true // Set the flag when a time item is selected
                val selectedItem = binding.autoComplete.text.toString()
                val itemSelected = when (selectedItem) {
                    "Months" -> itemsMonth[i]
                    "Years" -> itemsYear[i]
                    else -> "" // Handle default case if needed
                }
                if (itemSelected.isNotEmpty()) {
                    resetBox(binding.timeComplete)
                    isTimeHintRed = false
                }
            }
        }

        binding.quantityComplete.onItemClickListener = AdapterView.OnItemClickListener { adapterView, _, i, _ ->
            val itemSelected = adapterView.getItemAtPosition(i)
        }
    }

//    private fun isTimeSelectionValid(autoText: String, timeText: String): Boolean {
//
//    }



    private fun setSwipeRefreshBackgroundColor() {
        val isDarkMode = when (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) {
            android.content.res.Configuration.UI_MODE_NIGHT_YES -> true
            else -> false
        }

        if (isDarkMode) {
            // Dark mode is enabled, set background color to #012030
            seeaeduprocess.setBackgroundColor(resources.getColor(R.color.dark_cool_blue))
            window.navigationBarColor = ContextCompat.getColor(this, R.color.dark_cool_blue)

        } else {
            // Light mode is enabled, set background color to white
            seeaeduprocess.setBackgroundColor(resources.getColor(android.R.color.white))
            window.navigationBarColor = ContextCompat.getColor(this, R.color.white)
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

        }
    }
    private fun isTimeSelectionValid(autoText: String, timeText: String): Boolean {
        return when (autoText) {
            "Months" -> timeText.startsWith("Month")
            "Years" -> timeText.startsWith("Annual") || timeText.startsWith("Biennial")
            else -> false
        }
    }

//    private fun calculateAmount(time: String, quantity: Int): String {
//        val amount = when (time) {
//            "Month (1)" -> 25 * quantity
//            "Months (Quarterly)" -> 75 * quantity
//            "Months (Half - Yearly)" -> 149 * quantity
//            "Annual" -> 299 * quantity
//            "Biennial" -> 599 * quantity
//            else -> 0 // Default case
//        }
//        return "₹ $amount"
//    }

    private fun highlightBox(autoCompleteTextView: AutoCompleteTextView) {
        val shape = ShapeDrawable(RectShape())
        shape.paint.style = Paint.Style.STROKE
        shape.paint.color = Color.RED
        shape.paint.strokeWidth = 4.toFloat()
        autoCompleteTextView.background = shape
    }


//    private fun resetBox(autoCompleteTextView: AutoCompleteTextView) {
//
//    }
    private fun resetBox(autoCompleteTextView: AutoCompleteTextView) {
        autoCompleteTextView.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
        autoCompleteTextView.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
        if (isTimeHintRed) {
            setHintColor(autoCompleteTextView, Color.RED)
        } else {
            setHintColor(autoCompleteTextView, Color.GRAY) // Reset to default hint color
        }
    }

    private fun setHintColor(autoCompleteTextView: AutoCompleteTextView, color: Int) {
        val colorStateList = ColorStateList.valueOf(color)
        autoCompleteTextView.setHintTextColor(colorStateList)
    }

    private inner class NonSelectableArrayAdapter(context: Context, resource: Int, objects: List<String>) :
        ArrayAdapter<String>(context, resource, objects) {

        override fun isEnabled(position: Int): Boolean {
            // Make all items selectable except the first one ("Null")
            return position != 0
        }
    }

}