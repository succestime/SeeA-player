package com.jaidev.seeaplayer.Subscription

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.allAdapters.Model
import com.jaidev.seeaplayer.allAdapters.MyDotAdapter
import com.jaidev.seeaplayer.databinding.ActivitySeeAeduBinding
import me.relex.circleindicator.CircleIndicator3
import java.util.Timer
import java.util.TimerTask

class SeeAEdu : AppCompatActivity() {
    private lateinit var binding: ActivitySeeAeduBinding
    private lateinit var timer: Timer
    private lateinit var timerTask: TimerTask

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySeeAeduBinding.inflate(layoutInflater)
        supportActionBar?.hide()
        setContentView(binding.root)

        val items = listOf(
            Model(R.drawable.seea_edu_monthly),
            Model(R.drawable.seea_quarterly),
            Model(R.drawable.seea_half_yearly),
            Model(R.drawable.seea_annual),
            Model(R.drawable.seea_biennial)
        )

        val adapter = MyDotAdapter(items)
        binding.viewPager.adapter = adapter

        // Set circle indicator
        val indicator: CircleIndicator3 = findViewById(R.id.dotIndictor)
        indicator.setViewPager(binding.viewPager)

//        // Set listener for page change callback
//        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
//            override fun onPageSelected(position: Int) {
//                super.onPageSelected(position)
////                updateDotIndector(position)
//            }
//        })

        //Automatically scroll ViewPager2
        startAutoScroll()

        binding.SeeAoneActivity.setOnClickListener {
            startActivity(Intent(this, SeeAOne::class.java))
            finish()
        }

        binding.subscribeBtn.setOnClickListener {
            if (checkConnection(this)) {
                startActivity(Intent(this, SeeaEduProcess::class.java))
            } else {
                // Internet is not connected, show a toast message
                Toast.makeText(this, "No Internet Connection \uD83C\uDF10", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun checkConnection(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo: NetworkInfo? = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

//    private fun createDotIndictor(count: Int) {
//        for (i in 0 until count) {
//            val dot = ImageView(this)
//            dot.setImageResource(R.drawable.dot_selector)
//            binding.dotIndictor.addView(dot)
//        }
//    }

    private fun startAutoScroll() {
        timer = Timer()
        timerTask = object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    val newPosition = (binding.viewPager.currentItem + 1) % binding.viewPager.adapter?.itemCount!!
                    binding.viewPager.setCurrentItem(newPosition, true)
                }
            }
        }
        timer.schedule(timerTask, 4000, 4000) // Delay 2 seconds, repeat every 2 seconds
    }

    override fun onDestroy() {
        super.onDestroy()
        timer.cancel()
    }
}
