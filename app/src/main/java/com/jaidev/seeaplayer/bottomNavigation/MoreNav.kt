
package com.jaidev.seeaplayer.bottomNavigation

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.jaidev.seeaplayer.LogSignIn.signin
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.databinding.FragmentMoreNavBinding

class moreNav : Fragment() {
    private lateinit var binding : FragmentMoreNavBinding
    private lateinit var relativeLayout: RelativeLayout
    private var checkedItem: Int = 0
    private var selected: String = ""
    private val CHECKED_ITEM = "checked_item"
    lateinit var mAdView: AdView

    // Define your variable here
    companion object{
        lateinit var auth : FirebaseAuth
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.fragment_more_nav, container, false)
        binding = FragmentMoreNavBinding.bind(view)
        (activity as AppCompatActivity).supportActionBar?.title = "Settings"
        MobileAds.initialize(requireContext()) {}
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
    // Internet is connected, navigate to the desired destination
    it.findNavController().navigate(R.id.action_moreNav_to_moreSettingNav)
}
        binding.subscribePlans.setOnClickListener {
            if (checkConnection(requireContext())) {
                // Internet is connected, navigate to the desired destination
                it.findNavController().navigate(R.id.action_moreNav_to_seeAOne)
            } else {
                // Internet is not connected, show a toast message
                Toast.makeText(requireContext(), "No Internet Connection \uD83C\uDF10", Toast.LENGTH_SHORT).show()
            }
        }

        binding.appThemelayout.setOnClickListener {
            showDialog()

        }
        setActionBarGradient()
        relativeLayout = binding.relativeLayoutMore

        // Set the background color of SwipeRefreshLayout based on app theme
        setRelativeLayoutBackgroundColor()

binding.userDetails.setOnClickListener {
    if (auth.currentUser == null) {
        startActivity(Intent(requireContext(), signin::class.java))
        requireActivity().finish()
    }else{
        // Do nothing
    }
}

        return view
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
            relativeLayout.setBackgroundColor(resources.getColor(R.color.dark_cool_blue))
            activity?.window?.navigationBarColor = ContextCompat.getColor(requireContext(), R.color.dark_cool_blue)

        } else {
            // Light mode is enabled, set background color to white
            relativeLayout.setBackgroundColor(resources.getColor(android.R.color.white))
            activity?.window?.navigationBarColor = ContextCompat.getColor(requireContext(), android.R.color.white)
            activity?.window?.decorView?.systemUiVisibility = activity?.window?.decorView?.systemUiVisibility?.or(
                View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            )!!
        }
    }


    private fun updateData(): String {

        return "User : ${auth.currentUser?.displayName}"

    }


    fun showDialog() {
        val themes = resources.getStringArray(R.array.theme)
        val builder = MaterialAlertDialogBuilder(requireContext())
        builder.setTitle("Select Theme")
        builder.setSingleChoiceItems(
            R.array.theme,
            getCheckedItem()
        ) { dialogInterface: DialogInterface, i: Int ->
            selected = themes[i]
            checkedItem = i
        }

        builder.setPositiveButton("OK") { dialogInterface: DialogInterface, i: Int ->
            if (selected == null) {
                selected = themes[i]
                checkedItem = i
            }

            when (selected) {
                "System Default" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                "Dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                "Light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

            }
            setCheckedItem(checkedItem)
        }

        builder.setNegativeButton("Cancel") { dialogInterface: DialogInterface, i: Int ->
            dialogInterface.dismiss()
        }

        val dialog = builder.create()
        dialog.show()
    }
    private fun getCheckedItem(): Int {
        return requireContext().getSharedPreferences("YourSharedPreferencesName", Context.MODE_PRIVATE)
            .getInt(CHECKED_ITEM, checkedItem)
    }

    private fun setCheckedItem(i: Int) {
        requireContext().getSharedPreferences("YourSharedPreferencesName", Context.MODE_PRIVATE)
            .edit()
            .putInt(CHECKED_ITEM, i)
            .apply()
    }

    private fun setActionBarGradient() {
        // Check the current night mode
        val nightMode = AppCompatDelegate.getDefaultNightMode()
        if (nightMode == AppCompatDelegate.MODE_NIGHT_NO) {
            // Light mode is applied
            (activity as AppCompatActivity).supportActionBar?.apply {
                setBackgroundDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.background_actionbar_light
                    )
                )
            }
        } else if (nightMode == AppCompatDelegate.MODE_NIGHT_YES) {
            // Dark mode is applied
            (activity as AppCompatActivity).supportActionBar?.apply {
                setBackgroundDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.background_actionbar
                    )
                )
            }
        } else {
            // System Default mode is applied
            val isSystemDefaultDarkMode = when (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) {
                android.content.res.Configuration.UI_MODE_NIGHT_YES -> true
                else -> false
            }
            // Set the ActionBar color based on the System Default mode
            if (isSystemDefaultDarkMode) {
                // System Default mode is dark
                (activity as AppCompatActivity).supportActionBar?.apply {
                    setBackgroundDrawable(
                        ContextCompat.getDrawable(
                            requireContext(),
                            R.drawable.background_actionbar
                        )
                    )
                }
            } else {
                // System Default mode is light
                (activity as AppCompatActivity).supportActionBar?.apply {
                    setBackgroundDrawable(
                        ContextCompat.getDrawable(
                            requireContext(),
                            R.drawable.background_actionbar_light
                        )
                    )
                }
            }
        }
    }

    private fun showSignOutDialog() {
        // Check if the user is registered (logged in)
        if (auth.currentUser == null) {
            // Show dialog for not registered user
            val notRegisteredBuilder = MaterialAlertDialogBuilder(requireContext())
            notRegisteredBuilder.setTitle("Not registered!!")
            notRegisteredBuilder.setMessage("You are not registered in the app. You do not need to sign out.")
            notRegisteredBuilder.setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            notRegisteredBuilder.show()
        } else {
            // Show sign out confirmation dialog for registered user
            val signOutBuilder = MaterialAlertDialogBuilder(requireContext())
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


    override fun onResume() {
        super.onResume()
        MobileAds.initialize(requireContext()) {}
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
        setActionBarGradient()

    }

}
