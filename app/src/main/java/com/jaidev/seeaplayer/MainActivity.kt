

package com.jaidev.seeaplayer


import android.Manifest
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.text.format.DateUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.jaidev.seeaplayer.Services.FolderDetectionService
import com.jaidev.seeaplayer.allAdapters.VideoAdapter
import com.jaidev.seeaplayer.bottomNavigation.downloadNav
import com.jaidev.seeaplayer.bottomNavigation.homeNav
import com.jaidev.seeaplayer.browserActivity.LinkTubeActivity
import com.jaidev.seeaplayer.dataClass.Folder
import com.jaidev.seeaplayer.dataClass.Music
import com.jaidev.seeaplayer.dataClass.RecantMusic
import com.jaidev.seeaplayer.dataClass.RecantVideo
import com.jaidev.seeaplayer.dataClass.VideoData
import com.jaidev.seeaplayer.dataClass.exitApplication
import com.jaidev.seeaplayer.databinding.ActivityMainBinding
import com.jaidev.seeaplayer.musicActivity.PlayerMusicActivity
import java.io.File

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding
    private lateinit var currentFragment: Fragment
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var adapter: VideoAdapter
    private  var runnable : Runnable? = null
    private lateinit var drawerLayout: DrawerLayout
    private var mInterstitialAd: InterstitialAd? = null
    private var doubleBackToExitPressedOnce = false
    private var adLoaded = false // Flag to track if the ad is loaded

    private lateinit var linkTubeActivityResultLauncher: ActivityResultLauncher<Intent>


    companion object {

        private const val PREFS_NAME = "speed_preferences"

        var videoRecantList = ArrayList<RecantVideo>()
        var musicRecantList = ArrayList<RecantMusic>()
        lateinit var videoList: ArrayList<VideoData>
        lateinit var MusicListMA: ArrayList<Music>
        lateinit var musicListSearch: ArrayList<Music>
        var search: Boolean = false
        lateinit var searchList: ArrayList<VideoData>
        lateinit var folderList: ArrayList<Folder>
        var dataChanged: Boolean = false
        var adapterChanged: Boolean = false
        var sortValue: Int = 0
        val sortList = arrayOf(
            MediaStore.Video.Media.DATE_ADDED + " DESC",
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.TITLE + " DESC",
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.SIZE + " DESC"
        )
        val sortMusicList = arrayOf(
            MediaStore.Audio.Media.DATE_ADDED + " DESC",
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.TITLE + " DESC",
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.SIZE + " DESC"
        )
        fun isInternetAvailable(context: Context): Boolean {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork ?: return false
            val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
            return activeNetwork.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    @SuppressLint("SuspiciousIndentation", "RestrictedApi", "CutPasteId",
        "UnspecifiedRegisterReceiverFlag"
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedPreferences = this.getSharedPreferences("themes", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.apply()

        // Clear the saved speed when the app starts
        val sharedPreferencesMusic = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPreferencesMusic.edit()) {
            clear()
            apply()
        }
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        loadAd()

        // Register BroadcastReceiver
        val filter = IntentFilter("com.yourapp.LINK_TUBE_OPENED")
        registerReceiver(linkTubeOpenedReceiver, filter)

        // Register for activity result
        linkTubeActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            // Hide progress bar when returning from LinkTubeActivity
            binding.mainActivityProgressbar.visibility = View.GONE
            setFragment(homeNav())
            binding.bottomNav.selectedItemId = R.id.home // Manually set the selected item
        }
        bottomNav()
        funRequestRuntimePermission()
        setBottomLayoutBackgroundColor()
        setActionBarGradient()
        // Check internet connectivity and show/hide the "Subscribe" TextView
        checkInternetConnection()

        toggle = ActionBarDrawerToggle(this, binding.root, R.string.open, R.string.close)
        binding.root.addDrawerListener(toggle)
        toggle.syncState()

//        setupActionBar()
//        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNav)
//        bottomNavigationView.itemIconTintList = null // This line ensures that the icon will use its actual color

        drawerLayout = binding.drawerLayoutMA

        setDrawerLayoutBackgroundColor()

        if (shouldStartService()) {
            startMediaScanService()
        }


    }

    private val linkTubeOpenedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // Hide the progress bar when LinkTubeActivity is opened
            binding.mainActivityProgressbar.visibility = View.GONE
        }
    }


    private fun bottomNav() {
        binding.bottomNav.setOnItemSelectedListener {
            try {
                when (it.itemId) {
                    R.id.home -> {
                        setFragment(homeNav())
                    }
                    R.id.music -> {
                        setFragment(musicNav())
                    }
                    R.id.download -> {
                        setFragment(downloadNav())
                    }
                    R.id.linkTube -> {
                        val intent = Intent(this@MainActivity, LinkTubeActivity::class.java)
                        linkTubeActivityResultLauncher.launch(intent)
                            binding.mainActivityProgressbar.visibility = View.VISIBLE
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error coming", Toast.LENGTH_SHORT).show()
            }
            true
        }
    }




    @RequiresApi(Build.VERSION_CODES.R)
    private fun funRequestRuntimePermission(){
        if (requestRuntimePermission()) {
            folderList = ArrayList()
            videoList = getAllVideos()
            MusicListMA = getAllAudio()
            setFragment(homeNav())

            runnable = Runnable {
                if(dataChanged){
                    dataChanged = false
                    adapterChanged = true
                }
                Handler(Looper.getMainLooper()).postDelayed(runnable!!, 200)
            }
            Handler(Looper.getMainLooper()).postDelayed(runnable!!, 0)
        } else {
            folderList = ArrayList()
            videoList = ArrayList()
            MusicListMA = getAllAudio()
            setFragment(homeNav())
        }
    }


    private fun checkInternetConnection() {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        val isInternetConnected = networkInfo != null && networkInfo.isConnected

        val subscribeTextView = supportActionBar?.customView?.findViewById<LinearLayout>(R.id.connectivityCardView)
        if (isInternetConnected) {
            subscribeTextView?.visibility = View.VISIBLE
        } else {
            subscribeTextView?.visibility = View.GONE
        }
    }


    @SuppressLint("ResourceType")
    private fun setDrawerLayoutBackgroundColor() {

        val isDarkMode = when (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) {
            android.content.res.Configuration.UI_MODE_NIGHT_YES -> true
            else -> false
        }

        if (isDarkMode) {
            // Dark mode is enabled, set background color to #012030
            drawerLayout.setBackgroundColor(resources.getColor(R.color.dark_cool_blue))
        } else {
            // Light mode is enabled, set background color to white
            drawerLayout.setBackgroundColor(resources.getColor(android.R.color.white))
        }
    }

    @SuppressLint("ResourceType")
    private fun setBottomLayoutBackgroundColor() {

        val isDarkMode = when (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) {
            android.content.res.Configuration.UI_MODE_NIGHT_YES -> true
            else -> false
        }

        if (isDarkMode) {
            // Dark mode is enabled, set background color to #012030
            binding.bottomNav.setBackgroundColor(resources.getColor(R.color.light_bottom_navBar))
            window.navigationBarColor = ContextCompat.getColor(this, R.color.light_bottom_navBar)
        } else {
            // Light mode is enabled, set background color to white
            binding.bottomNav.setBackgroundColor(resources.getColor(android.R.color.white))
            window.navigationBarColor = ContextCompat.getColor(this, R.color.white)
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR


        }
    }
    private fun shouldStartService(): Boolean {
        val lastScanTime = getLastScanTime()
        val currentTime = System.currentTimeMillis()
        val scanInterval = 24 * 60 * 60 * 1000 // 24 hours in milliseconds

        // Check if the last scan time is not set or if the interval has passed since the last scan
        return lastScanTime == null || currentTime - lastScanTime >= scanInterval
    }

    private fun getLastScanTime(): Long {
        // Retrieve the last scan time from SharedPreferences
        val sharedPreferences = getSharedPreferences("MyPreferences", Context.MODE_PRIVATE)
        return sharedPreferences.getLong("lastScanTime", 0)
    }

    private fun startMediaScanService() {
        val serviceIntent = Intent(this, FolderDetectionService::class.java)
        startService(serviceIntent)
    }




    private fun setFragment(fragment: Fragment) {
        currentFragment = fragment
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.frameLayout, fragment)
        transaction.commit()
    }



    private fun requestRuntimePermission(): Boolean {
        //android 13 permission request
        //android 13 permission request
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_MEDIA_VIDEO
                )
                != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_MEDIA_AUDIO
                )
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.READ_MEDIA_VIDEO,
                        Manifest.permission.READ_MEDIA_AUDIO ,
                        READ_EXTERNAL_STORAGE,
                        Manifest.permission.POST_NOTIFICATIONS ,
                    ),
                    13
                )
                return false
            }
            return true
        }

        //requesting storage permission for only devices less than api 28
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(this, arrayOf(WRITE_EXTERNAL_STORAGE ,  READ_EXTERNAL_STORAGE,), 13)
                return false
            }
            return true
        } else {
            //read external storage permission for devices higher than android 10 i.e. api 29
            if (ActivityCompat.checkSelfPermission(
                    this,
                    READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(this, arrayOf(READ_EXTERNAL_STORAGE), 14)
                return false
            }
        }
        return true
    }


    @RequiresApi(Build.VERSION_CODES.R)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 13) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
                folderList = ArrayList()
                videoList = getAllVideos()
                MusicListMA = getAllAudio()
                setFragment(homeNav())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    requestPermissionR()
                }
            } else Snackbar.make(binding.root, "Storage Permission Needed!!", 5000)
                .setAction("OK") {
                    ActivityCompat.requestPermissions(this, arrayOf(WRITE_EXTERNAL_STORAGE), 13)

                }
                .show()
//                ActivityCompat.requestPermissions(this, arrayOf(WRITE_EXTERNAL_STORAGE),13)
        }

        //for read external storage permission
        if (requestCode == 14) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
                folderList = ArrayList()
                videoList = getAllVideos()
                MusicListMA = getAllAudio()
                setFragment(homeNav())

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    requestPermissionR()
                }
            } else Snackbar.make(binding.root, "Storage Permission Needed!!", 5000)
                .setAction("OK") {
                    ActivityCompat.requestPermissions(this, arrayOf(READ_EXTERNAL_STORAGE), 14)

                }
                .show()
        }
    }

    //for requesting android 11 or higher storage permission
    private fun requestPermissionR() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.addCategory("android.intent.category.DEFAULT")
                intent.data = Uri.parse("package:${this.applicationContext.packageName}")
                ContextCompat.startActivity(this, intent, null)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.sort_view, menu)

        val sortOrderMenuItem = menu.findItem(R.id.sortOrder)
        sortOrderMenuItem.setOnMenuItemClickListener { item ->
            // Handle the click event here
            when (item.itemId) {
                R.id.sortOrder -> {
                    val menuItems = arrayOf(
                        "Latest",
                        "Oldest",
                        "Name(A to Z)",
                        "Name(Z to A)",
                        "File Size(Smallest)",
                        "File Size(Largest)"
                    )
                    var value = sortValue
                    val dialog = MaterialAlertDialogBuilder(this)
                        .setTitle("Sort By")
                        .setPositiveButton("OK") { _, _ ->
                            val sortEditor = getSharedPreferences("Sorting", MODE_PRIVATE).edit()
                            sortEditor.putInt("sortValue", value)
                            sortEditor.apply()

                            //for restarting app
                            finish()
                            startActivity(intent)
                        }
                        .setSingleChoiceItems(menuItems, sortValue) { _, pos ->
                            value = pos
                        }
                        .create()
                    dialog.show()
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setBackgroundColor(Color.BLACK)
                    true
                }
                else -> false
            }
        }

        val profileMenuItem = menu.findItem(R.id.profile)
        profileMenuItem.setOnMenuItemClickListener { item ->
            // Handle the click event here
            when (item.itemId) {
                R.id.profile -> {
                    startActivity(Intent(this@MainActivity, More::class.java))
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_right)
                    if (!adLoaded) { // Check if the ad has already been loaded
                        loadAd()
                        adLoaded = true // Set the flag to true after loading the ad
                    }
                    mInterstitialAd?.show(this)

                    true

                }

                else -> false
            }
        }


        return true
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if (toggle.onOptionsItemSelected(item))
            return true

        // Handle other menu items if needed
        return super.onOptionsItemSelected(item)
    }

    // Function to refresh folder list
    @RequiresApi(Build.VERSION_CODES.R)
    fun refreshFolderList() {
        folderList.clear()
        videoList = getAllVideos() // Repopulate the videoList which also populates folderList
        MusicListMA = getAllAudio()// Repopulate the music list if needed
    }



    @SuppressLint("Range")
    fun getAllVideos(): ArrayList<VideoData> {
        val sortEditor = getSharedPreferences("Sorting", MODE_PRIVATE)
    sortValue = sortEditor.getInt("sortValue", 0)
        val tempList = ArrayList<VideoData>()
        val folderMap = mutableMapOf<String, String>()  // Map to hold folder ID and name association

        val projection = arrayOf(
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.BUCKET_ID
        )
        val cursor = this.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection, null, null,
            sortList[sortValue]
        )
        cursor?.use {
            if (it.moveToFirst()) {
                do {
                    val titleC = it.getString(it.getColumnIndex(MediaStore.Video.Media.TITLE)) ?: ""
                    val idC = it.getString(it.getColumnIndex(MediaStore.Video.Media._ID)) ?: ""
                    val folderC = it.getString(it.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)) ?: ""
                    val folderIdC = it.getString(it.getColumnIndex(MediaStore.Video.Media.BUCKET_ID)) ?: ""
                    val sizeC = it.getString(it.getColumnIndex(MediaStore.Video.Media.SIZE)) ?: ""
                    val pathC = it.getString(it.getColumnIndex(MediaStore.Video.Media.DATA)) ?: ""
                    val durationC = it.getString(it.getColumnIndex(MediaStore.Video.Media.DURATION))?.toLong() ?: 0L
                    val dateAddedMillis = it.getLong(it.getColumnIndex(MediaStore.Video.Media.DATE_ADDED))

                    try {
                        val file = File(pathC)
                        val artUriC = Uri.fromFile(file)
                        val currentTimestamp = System.currentTimeMillis()
                        val isNewVideo = currentTimestamp - dateAddedMillis <= DateUtils.DAY_IN_MILLIS

                        val video = VideoData(
                            title = titleC,
                            id = idC,
                            folderName = folderC,
                            duration = durationC,
                            size = sizeC,
                            path = pathC,
                            artUri = artUriC,
                            dateAdded = dateAddedMillis,
                            isNew = isNewVideo
                        )

                        if (file.exists()) tempList.add(video)

                        // Ensure folder is added to MainActivity.folderList
                        if (folderList.none { it.id == folderIdC }) {
                           folderList.add(Folder(id = folderIdC, folderName = folderMap[folderIdC] ?: folderC.ifEmpty { "Internal memory" } ))
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } while (it.moveToNext())
            }
        }
        return tempList
    }



    @SuppressLint("Recycle", "Range")
    @RequiresApi(Build.VERSION_CODES.R)
   fun getAllAudio(): ArrayList<Music>{
        val tempList = ArrayList<Music>()
        val selection = MediaStore.Audio.Media.IS_MUSIC +  " != 0"
        val projection = arrayOf(MediaStore.Audio.Media._ID,MediaStore.Audio.Media.TITLE,MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ARTIST,MediaStore.Audio.Media.DURATION,MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.ALBUM_ID ,     MediaStore.Audio.Media.SIZE,)
        val cursor = this.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, null,
            sortMusicList[sortValue]
        )

        if (cursor != null) {
            if (cursor.moveToNext()) {
                do {
                    val titleC = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE))?:"Unknown"
                    val idC = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media._ID))?:"Unknown"
                    val albumC = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM))?:"Unknown"
                    val artistC = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST))?:"Unknown"
                    val pathC = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA))
                    val durationC = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION))
                    val albumIdC = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)).toString()
                    val uri =  Uri.parse("content://media/external/audio/albumart")
                    val artUriC = Uri.withAppendedPath(uri, albumIdC).toString()
                    val sizeC = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.SIZE))
                    val music = Music(id = idC, title = titleC, album = albumC, artist = artistC, path = pathC, duration = durationC,
                        artUri = artUriC , size = sizeC)
                    val file = File(music.path)
                    if(file.exists())
                        tempList.add(music)
                }while (cursor.moveToNext())
            }
            cursor.close()
        }
        return tempList
    }



    override fun onDestroy() {
        super.onDestroy()
        if (!PlayerMusicActivity.isPlaying && PlayerMusicActivity.musicService != null) {
            exitApplication()
        }
        unregisterReceiver(linkTubeOpenedReceiver)

    }
    private fun setActionBarGradient() {
        // Check the current night mode
        val nightMode = AppCompatDelegate.getDefaultNightMode()
        if (nightMode == AppCompatDelegate.MODE_NIGHT_NO) {
            // Light mode is applied
            supportActionBar?.apply {
                setBackgroundDrawable(
                    ContextCompat.getDrawable(
                        this@MainActivity,
                        R.drawable.background_actionbar_light
                    )
                )
            }
        } else if (nightMode == AppCompatDelegate.MODE_NIGHT_YES) {
            // Dark mode is applied
            supportActionBar?.apply {
                setBackgroundDrawable(
                    ContextCompat.getDrawable(
                        this@MainActivity,
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
                supportActionBar?.apply {
                    setBackgroundDrawable(
                        ContextCompat.getDrawable(
                            this@MainActivity,
                            R.drawable.background_actionbar
                        )
                    )
                }
            } else {
                // System Default mode is light
                supportActionBar?.apply {
                    setBackgroundDrawable(
                        ContextCompat.getDrawable(
                            this@MainActivity,
                            R.drawable.background_actionbar_light
                        )
                    )
                }
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged", "SuspiciousIndentation")
    override fun onResume() {
        super.onResume()
        setActionBarGradient()
        if (!adLoaded) { // Check if the ad has already been loaded
            loadAd()
            adLoaded = true // Set the flag to true after loading the ad
        }
    }
    override fun onPause() {
        super.onPause()
        if (!adLoaded) { // Check if the ad has already been loaded
            loadAd()
            adLoaded = true // Set the flag to true after loading the ad
        }

    }
    fun loadAd() {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            this,
            "ca-app-pub-3504589383575544/9248821864",
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    mInterstitialAd = null
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    mInterstitialAd = interstitialAd
                }
            })
    }
    override fun onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed()
            return
        }

        this.doubleBackToExitPressedOnce = true
        Toast.makeText(this, "Tap again to exit", Toast.LENGTH_SHORT).show()

        Handler(Looper.getMainLooper()).postDelayed(Runnable { doubleBackToExitPressedOnce = false }, 2000)
    }


}

