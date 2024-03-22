package com.jaidev.seeaplayer

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.jaidev.seeaplayer.databinding.ActivitySeeAeduBinding

class SeeAEdu : AppCompatActivity() {
    private lateinit var binding: ActivitySeeAeduBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySeeAeduBinding.inflate(layoutInflater)
        supportActionBar?.hide()
        setContentView(binding.root)


val items = listOf(Model(R.drawable.seea_monthly),Model(R.drawable.seea_quarterly)
    ,Model(R.drawable.seea_annualy),Model(R.drawable.seea_biennial))

        val adapter = MyDotAdapter(items)
        binding.viewPager.adapter = adapter
        createDotIndictor(items.size)

        binding.viewPager.registerOnPageChangeCallback(object :ViewPager2.OnPageChangeCallback(){

            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateDotIndector(position)

            }

            })
        binding.SeeAoneActivity.setOnClickListener {
            startActivity(Intent(this, SeeAOne::class.java))
            finish()
        }

        binding.subscribeBtn.setOnClickListener {
            startActivity(Intent(this, SeeaEduProcess::class.java))
        }
    }

    private fun createDotIndictor(count : Int){
        for(i in 0 until count){
            val dot = ImageView(this)
            dot.setImageResource(R.drawable.dot_selector)
            binding.dotIndictor.addView(dot)
        }
    }

    private fun updateDotIndector(position : Int){
        for(i in 0 until binding.dotIndictor.childCount){
            val dot = binding.dotIndictor.getChildAt(i) as ImageView
            dot.isSelected = i == position
        }
    }

}
