package com.jaidev.seeaplayer.Subscription

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.databinding.ActivitySeeAoneBinding

class SeeAOne : AppCompatActivity() {
    private lateinit var binding: ActivitySeeAoneBinding
    private var selectedBox: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make the status bar transparent
        window.statusBarColor = Color.BLACK
        // Hide the action bar if you have one
        supportActionBar?.hide()
        binding = ActivitySeeAoneBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()
        binding.fakeCodeNumber.setOnClickListener {
            seea_one_sheet().show(supportFragmentManager, "newTaskTag")
        }

        val squareBox = binding.monthlyBox
        val quarterlyBox = binding.quarterlyBox
        val annualBox = binding.annualBox
        val biennialBox = binding.biennialBox

        // Setting touch listeners for the boxes
        setTouchListener(squareBox, binding.monthlySelect, binding.monthlyCircle)
        setTouchListener(quarterlyBox, binding.quarterlySelect, binding.quarterlyCircle)
        setTouchListener(annualBox, binding.annualSelect, binding.annualCircle)
        setTouchListener(biennialBox, binding.biennialSelect, binding.biennialCircle)

        // Select the quarterly box by default
        selectBox(quarterlyBox, binding.quarterlySelect, binding.quarterlyCircle)

        binding.SeeAeduActivity.setOnClickListener {
            startActivity(Intent(this, SeeAEdu::class.java))
            finish()
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun selectBox(box: View, selectIndicator: ImageView, circleIndicator: View?) {
        // Reset the background of the previously selected box (if any)
        selectedBox?.background = getDrawable(R.drawable.square_box_bg_selector)

        // Set the background of the clicked box to the selected state
        box.background = getDrawable(R.drawable.square_box_bg_selected)

        // Update the currently selected box
        selectedBox = box

        // Show all circles for unselected boxes
        binding.monthlyCircle?.visibility = ImageView.VISIBLE
        binding.quarterlyCircle?.visibility = ImageView.VISIBLE
        binding.annualCircle?.visibility = ImageView.VISIBLE
        binding.biennialCircle?.visibility = ImageView.VISIBLE

        // Hide all select indicators
        binding.monthlySelect.visibility = ImageView.INVISIBLE
        binding.quarterlySelect.visibility = ImageView.INVISIBLE
        binding.annualSelect.visibility = ImageView.INVISIBLE
        binding.biennialSelect.visibility = ImageView.INVISIBLE

        // Show the select indicator of the selected box
        selectIndicator.visibility = ImageView.VISIBLE
        // Hide the circle indicator of the selected box
        circleIndicator?.visibility = ImageView.INVISIBLE
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setTouchListener(box: View, selectIndicator: ImageView, circleIndicator: View?) {
        box.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                selectBox(box, selectIndicator, circleIndicator)
            }
            true
        }
    }
}
