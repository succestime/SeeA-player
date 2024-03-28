
package com.jaidev.seeaplayer


import android.Manifest
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.format.DateUtils
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
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
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.jaidev.seeaplayer.bottomNavigation.downloadNav
import com.jaidev.seeaplayer.bottomNavigation.homeNav
import com.jaidev.seeaplayer.dataClass.Folder
import com.jaidev.seeaplayer.dataClass.Music
import com.jaidev.seeaplayer.dataClass.RecantMusic
import com.jaidev.seeaplayer.dataClass.RecantVideo
import com.jaidev.seeaplayer.dataClass.VideoData
import com.jaidev.seeaplayer.dataClass.exitApplication
import com.jaidev.seeaplayer.databinding.ActivityMainBinding
import java.io.File

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    private lateinit var currentFragment: Fragment
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var adapter: VideoAdapter
    private  var runnable : Runnable? = null
    private lateinit var drawerLayout: DrawerLayout
    private var checkedItem: Int = 0
    private var selected: String = ""
    private val CHECKED_ITEM = "checked_item"

    private var mInterstitialAd: InterstitialAd? = null

    companion object {
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
    }

    @SuppressLint("SuspiciousIndentation", "RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedPreferences = this.getSharedPreferences("themes", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.apply()

        when (getCheckedItem()) {
            0 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            1 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            2 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

        toggle = ActionBarDrawerToggle(this, binding.root, R.string.open, R.string.close)
        binding.root.addDrawerListener(toggle)
        toggle.syncState()


        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNavigationView.itemIconTintList = null // This line ensures that the icon will use its actual color



        supportActionBar?.apply {

            setBackgroundDrawable(ContextCompat.getDrawable(this@MainActivity, R.drawable.background_actionbar))
        }

        setActionBarGradient()
        drawerLayout = binding.drawerLayoutMA
        // Set the background color of SwipeRefreshLayout based on app theme
        setDrawerLayoutBackgroundColor()
        // Set the title for the action bar
        supportActionBar?.title = "SeeA Player"

        if (requestRuntimePermission()) {
            folderList = ArrayList()
            videoList = getAllVideos()
            setFragment(homeNav())
            MusicListMA = getAllAudios()

//            FavoritesManager.loadFavorites(this@MainActivity)

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
            setFragment(homeNav())
            MusicListMA = getAllAudios()
        }
// Check if the service needs to be started
        if (shouldStartService()) {
            startMediaScanService()
        }



        binding.drawerLayoutMA.setOnClickListener {
            loadAd()
            mInterstitialAd?.show(this)
        }



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
                        startActivity(intent)
                    }

                    R.id.more -> {
                        val intent = Intent(this@MainActivity, More::class.java)
                        startActivity(intent)
                    }

                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error coming", Toast.LENGTH_SHORT).show()
            }
            return@setOnItemSelectedListener true
        }

        binding.navView.setNavigationItemSelectedListener {

            when (it.itemId) {
                R.id.settingsNav -> {
                    val intent = Intent(this@MainActivity, More::class.java)
                    startActivity(intent) }
                R.id.themesNav -> {
                    val themes = resources.getStringArray(R.array.theme)
                    val builder = MaterialAlertDialogBuilder(this)
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

                R.id.sortOrderNav -> {
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
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setBackgroundColor(Color.RED)
                }

                R.id.exitNav -> {
                    val builder = MaterialAlertDialogBuilder(this)
                    builder.setTitle("Exit")
                        .setMessage("Do you want to close app?")
                        .setPositiveButton("Yes") { _, _ ->
                            exitApplication()
                        }
                        .setNegativeButton("No") { dialog, _ ->
                            dialog.dismiss()
                        }
                    val customDialog = builder.create()
                    customDialog.show()

                    customDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.RED)
                    customDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.GREEN)
                }
            }
            true
        }


    }

    private fun getCheckedItem(): Int {
        return this.getSharedPreferences("YourSharedPreferencesName", Context.MODE_PRIVATE)
            .getInt(CHECKED_ITEM, checkedItem)
    }
    private fun setCheckedItem(i: Int) {
        this.getSharedPreferences("YourSharedPreferencesName", Context.MODE_PRIVATE)
            .edit()
            .putInt(CHECKED_ITEM, i)
            .apply()
    }


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
    private fun shouldStartService(): Boolean {
        val lastScanTime = getLastScanTime()
        val currentTime = System.currentTimeMillis()
        val scanInterval = 24 * 60 * 60 * 1000 // 24 hours in milliseconds

        // Check if the last scan time is not set or if the interval has passed since the last scan
        return lastScanTime == null || currentTime - lastScanTime >= scanInterval
    }

    private fun getLastScanTime(): Long? {
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
        transaction.disallowAddToBackStack()
        transaction.commit()
    }


    private fun requestRuntimePermission(): Boolean {
        //android 13 permission request
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                  Manifest.permission.READ_MEDIA_VIDEO
                )
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_MEDIA_VIDEO , Manifest.permission.READ_MEDIA_AUDIO),
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
                ActivityCompat.requestPermissions(this, arrayOf(WRITE_EXTERNAL_STORAGE), 13)
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
                setFragment(homeNav())
                MusicListMA = getAllAudios()
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
                setFragment(homeNav())
                MusicListMA = getAllAudios()
            } else Snackbar.make(binding.root, "Storage Permission Needed!!", 5000)
                .setAction("OK") {
                    ActivityCompat.requestPermissions(this, arrayOf(READ_EXTERNAL_STORAGE), 14)
                }
                .show()
        }
    }



    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.sort_view, menu)
        // Find the item you want to hide
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
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setBackgroundColor(Color.RED)
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


    @SuppressLint("Range")
    fun getAllVideos(): ArrayList<VideoData> {
        val sortEditor = getSharedPreferences("Sorting", MODE_PRIVATE)
        sortValue = sortEditor.getInt("sortValue", 0)

        val tempList = ArrayList<VideoData>()
        val tempFolderList = ArrayList<String>()
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
        if (cursor != null)
            if (cursor.moveToFirst()) {
                do {
                    try {
                        val titleC =
                            cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.TITLE))
                        val idC =
                            cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media._ID))
                        val folderC =
                            cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME))
                        val folderIdC =
                            cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_ID))
                        val sizeC =
                            cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.SIZE))
                        val pathC =
                            cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATA))
                        val durationC =
                            cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DURATION))
                                .toLong()
                        val dateAddedMillis = cursor.getLong(cursor.getColumnIndex(MediaStore.Video.Media.DATE_ADDED))

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
                                artUri = artUriC, dateAdded = dateAddedMillis, isNew =isNewVideo
                            )


                            if (file.exists()) tempList.add(video)

                        } catch (_: Exception) {
                        }
                        // for adding folders and watching that not duplicate folder should add
                        if (!tempFolderList.contains(folderC)) {
                            tempFolderList.add(folderC)
                            folderList.add(Folder(id = folderIdC, folderName = folderC))
                        }

                    } catch (e: Exception) {
                        Log.e("VideoLoadError", "Error loading video: ${e.message}", e)
                    }
                } while (cursor.moveToNext())
                cursor.close()
            }
        // Remove folders with 0 or null videos
        folderList.removeAll { folder ->
            tempList.none { video -> video.folderName == folder.folderName }
        }
        return tempList
    }

    @SuppressLint("Range", "SuspiciousIndentation")
    fun getAllAudios(): ArrayList<Music> {
        val sortMusicEditor = getSharedPreferences("Sorting", MODE_PRIVATE)
        sortValue = sortMusicEditor.getInt("sortValue", 0)

        val tempList = ArrayList<Music>()
        val selection = MediaStore.Audio.Media.IS_MUSIC + " != 0"
        val projection = arrayOf(
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATA,
        )
        val cursor = this.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, null,
            sortMusicList[sortValue]
        )

        if (cursor != null) {
            if (cursor.moveToNext()) {
                do {
                    val titleMC =
                        cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE))
                    val idMC =
                        cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media._ID))
                    val pathMC =
                        cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA))
                    val artistMC =
                        cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST))
                    val durationMC =
                        cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION))
                            .toLong()
                    val sizeMC =
                        cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.SIZE))
                    val albumMC =
                        cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM))
                    val albumIdMC =
                        cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID))
                            .toString()

                    try {
                        val file = File(pathMC)
                        val albumArtUri =
                            Uri.parse("content://media/external/audio/albumart/$albumIdMC")
                        val music = Music(
                            title = titleMC,
                            id = idMC,
                            duration = durationMC,
                            path = pathMC,
                            artUri = albumArtUri,
                            artist = artistMC,
                            album = albumMC,
                            albumId = albumIdMC,
                            size = sizeMC
                        )


                        if (file.exists()) {
                            tempList.add(music)
                        }

                    } catch (_: Exception) {
                        Toast.makeText(this, "Songs did not load", Toast.LENGTH_SHORT).show()
                    }
                } while (cursor.moveToNext())
                cursor.close()
            }
        }

        return tempList
    }
    fun reloadVideos() {
        videoList = getAllVideos() // Implement this method to get all videos
        adapter.updateList(videoList)
    }



    override fun onDestroy() {
        super.onDestroy()
        if (!PlayerMusicActivity.isPlaying && PlayerMusicActivity.musicService != null) {
            exitApplication()
        }

    }
    private fun setActionBarGradient() {
        // Check if light mode is applied
        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_NO) {
            // Set gradient background for action bar
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
    @SuppressLint("NotifyDataSetChanged", "SuspiciousIndentation")
    override fun onResume() {
        super.onResume()

    }

    fun loadAd() {
        val adRequest = AdRequest.Builder().build()


        InterstitialAd.load(
            this,
            "ca-app-pub-4270893888625106/2835817173",
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

}
