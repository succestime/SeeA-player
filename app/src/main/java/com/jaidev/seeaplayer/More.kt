
package com.jaidev.seeaplayer

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Bundle
import android.view.View
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.jaidev.seeaplayer.LogSignIn.signin
import com.jaidev.seeaplayer.Settings.MoreSettingNav
import com.jaidev.seeaplayer.Subscription.SeeAOne
import com.jaidev.seeaplayer.dataClass.ThemeHelper
import com.jaidev.seeaplayer.databinding.ActivityMoreBinding

class More : AppCompatActivity() {

    private lateinit var binding: ActivityMoreBinding
    private lateinit var relativeLayout: RelativeLayout
    lateinit var mAdView: AdView
    companion object{
        lateinit var auth : FirebaseAuth
    }


    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyThemeChange()
        binding = ActivityMoreBinding.inflate(layoutInflater)
        setContentView(binding.root)
        registerReceiver(themeChangeReceiver, IntentFilter("THEME_CHANGEDED"))
        supportActionBar?.title = "Settings"
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Enable the Up button
        MobileAds.initialize(this) {}
        mAdView = binding.bannerAds
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)
        mAdView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                binding.adsLayout.visibility = View.VISIBLE
            }
        }
        // Initially set the adsLayout visibility to GONE until the ad is loaded
        binding.adsLayout.visibility = View.GONE


      auth = FirebaseAuth.getInstance()


        binding.signOut.setOnClickListener {
            showSignOutDialog()
        }

        binding.Settingslayout.setOnClickListener {
            startActivity(Intent(this, MoreSettingNav::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_right)
        }
        binding.subscribePlans.setOnClickListener {
            if (checkConnection(this)) {
                startActivity(Intent(this, SeeAOne::class.java))
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_right)
            } else {
                // Internet is not connected, show a toast message
                Toast.makeText(this, "No Internet Connection \uD83C\uDF10", Toast.LENGTH_SHORT).show()
            }
        }

        binding.appThemelayout.setOnClickListener {
            startActivity(Intent(this, ThemeActivity::class.java))
           overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_right)
        }
//        setActionBarGradient()
        relativeLayout = binding.relativeLayoutMore

        // Set the background color of SwipeRefreshLayout based on app theme
        setRelativeLayoutBackgroundColor()

        binding.userDetails.setOnClickListener {
            if (checkConnection(this)) {
                // Internet is connected
                if (auth.currentUser == null) {
                    // User is not authenticated, navigate to sign-in screen
                    startActivity(Intent(this, signin::class.java))
                } else {
                    Toast.makeText(this, "You are already registered \uD83D\uDE0A", Toast.LENGTH_SHORT).show()

                }
            } else {
                // Internet is not connected, show a toast message
                Toast.makeText(this, "No Internet Connection \uD83C\uDF10", Toast.LENGTH_SHORT).show()
            }
        }

binding.adsRemove.setOnClickListener {
    binding.adsLayout.visibility = View.GONE
}
    }
    private val themeChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            applyThemeChange()
            recreate()
        }
    }
    private fun applyThemeChange() {
        val theme = ThemeHelper.getSavedTheme(this)
        ThemeHelper.applyTheme(this, theme)
    }

    private fun checkConnection(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val networkInfo: NetworkInfo? = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }
    private fun setRelativeLayoutBackgroundColor() {
        val isDarkMode = when (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) {
            android.content.res.Configuration.UI_MODE_NIGHT_YES -> true
            else -> false
        }

        if (isDarkMode) {
            // Dark mode is enabled, set background color to #012030
          window?.navigationBarColor = ContextCompat.getColor(this, R.color.dark_cool_blue)

        } else {
            // Light mode is enabled, set background color to white
        window?.navigationBarColor = ContextCompat.getColor(this, android.R.color.white)
            window?.decorView?.systemUiVisibility = window?.decorView?.systemUiVisibility?.or(
                View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            )!!
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    private fun updateData(): String {

        return "User : ${auth.currentUser?.displayName}"

    }
    private fun showSignOutDialog() {
        // Check if the user is registered (logged in)
        if (auth.currentUser == null) {
            // Show dialog for not registered user
            val notRegisteredBuilder = MaterialAlertDialogBuilder(this)
            notRegisteredBuilder.setTitle("Not registered!!")
            notRegisteredBuilder.setMessage("You are not registered in the app. You do not need to sign out.")
            notRegisteredBuilder.setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            notRegisteredBuilder.show()
        } else {
            // Show sign out confirmation dialog for registered user
            val signOutBuilder = MaterialAlertDialogBuilder(this)
            signOutBuilder.setTitle("Want to sign out?")
            signOutBuilder.setMessage("Do you want to Sign Out from the app? If yes, click on the Sign Out button.")
            signOutBuilder.setPositiveButton("Sign Out") { _, _ ->
               auth.signOut()
                binding.userDetails.text = updateData()
            }
            signOutBuilder.setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            signOutBuilder.show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(themeChangeReceiver)

    }

    override fun onResume() {
        super.onResume()

        MobileAds.initialize(this) {}
        mAdView = binding.bannerAds
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)
        mAdView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                binding.adsLayout.visibility = View.VISIBLE
            }
        }
        // Initially set the adsLayout visibility to GONE until the ad is loaded
        binding.adsLayout.visibility = View.GONE
        binding.userDetails.text = updateData()

    }
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.from_left, R.anim.to_right)
    }
}
