package com.jaidev.seeaplayer

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.jaidev.seeaplayer.databinding.ActivitySeeAeduBinding

class SeeAEdu : AppCompatActivity() {
    private lateinit var binding: ActivitySeeAeduBinding
    private var selectedBox: LinearLayout? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setTheme(More.themesList[More.themeIndex])
        binding = ActivitySeeAeduBinding.inflate(layoutInflater)
        supportActionBar?.hide()
        setContentView(binding.root)

        val squareBox = binding.monthlyBox
        val quarterlyBox = binding.quarterlyBox
        val annualBox = binding.annualBox



        squareBox.setOnClickListener { selectBox(squareBox, binding.monthlySelect) }
        quarterlyBox.setOnClickListener { selectBox(quarterlyBox, binding.quarterlySelect) }
        annualBox.setOnClickListener { selectBox(annualBox, binding.annualSelect) }

        quarterlyBox.performClick()


        binding.SeeAoneActivity.setOnClickListener {
            startActivity(Intent(this, SeeAOne::class.java))
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun selectBox(box: LinearLayout, selectIndicator: ImageView) {
        // Reset the background of the previously selected box (if any)
        selectedBox?.background = getDrawable(R.drawable.square_box_bg_selector)

        // Set the background of the clicked box to the selected state
        box.background = getDrawable(R.drawable.square_box_bg_selected)

        // Update the currently selected box
        selectedBox = box

        binding.monthlySelect.visibility = ImageView.GONE
        binding.quarterlySelect.visibility = ImageView.GONE
        binding.annualSelect.visibility = ImageView.GONE

        // Show indicator of the selected box
        selectIndicator.visibility = ImageView.VISIBLE
    }
}
