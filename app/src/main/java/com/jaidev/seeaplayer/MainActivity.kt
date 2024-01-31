package com.jaidev.seeaplayer


import android.Manifest
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.switchmaterial.SwitchMaterial
import com.jaidev.seeaplayer.bottomNavigation.downloadNav
import com.jaidev.seeaplayer.bottomNavigation.homeNav
import com.jaidev.seeaplayer.bottomNavigation.moreNav
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

    @SuppressLint("SuspiciousIndentation")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.coolBlueNav)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        toggle = ActionBarDrawerToggle(this, binding.root, R.string.open, R.string.close)
        binding.root.addDrawerListener(toggle)
        toggle.syncState()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (requestRuntimePermission()) {
            folderList = ArrayList()
            videoList = getAllVideos()
            setFragment(homeNav())
            MusicListMA = getAllAudios()


        } else {
            folderList = ArrayList()
            videoList = ArrayList()
            setFragment(homeNav())
          MusicListMA = getAllAudios()
        }

        FavoritesManager.loadFavorites(this)

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
                        setFragment(moreNav())
                    }

                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error coming", Toast.LENGTH_SHORT).show()
            }
            return@setOnItemSelectedListener true
        }

        binding.navView.setNavigationItemSelectedListener {

            when (it.itemId) {
                R.id.feedbackNav -> Toast.makeText(baseContext, "feedback", Toast.LENGTH_SHORT)
                    .show()

                R.id.settingsNav -> Toast.makeText(baseContext, "feedback2", Toast.LENGTH_SHORT)
                    .show()

                R.id.themesNav -> {

                    val currentThemeMode = AppCompatDelegate.getDefaultNightMode()

                    // Inflate the custom layout for the dialog
                    val dialogView = layoutInflater.inflate(R.layout.theme_dialoge, null)
                    val switchTheme = dialogView.findViewById<SwitchMaterial>(R.id.switchTheme)

                    // Set the switch state based on the current theme mode
                    switchTheme.isChecked = currentThemeMode == AppCompatDelegate.MODE_NIGHT_YES

                    // Set up the switch change listener
                    switchTheme.setOnCheckedChangeListener { _, isChecked ->
                        // Dynamically change the theme mode based on switch state
                        val newThemeMode =
                            if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
                            else AppCompatDelegate.MODE_NIGHT_NO

                        // Set the new theme mode
                        AppCompatDelegate.setDefaultNightMode(newThemeMode)

                        // Restart the activity to apply the new theme
                        finish()
                        startActivity(intent)
                    }

                    // Create a custom dialog
                    val dialog = MaterialAlertDialogBuilder(this)
                        .setTitle("Select Theme")
                        .setPositiveButton("Apply") { _, _ ->
                            // Apply the selected theme when the "Apply" button is clicked
                            // (Handled by the setOnCheckedChangeListener)
                        }
                        .setNegativeButton("Cancel") { dialog, _ ->
                            // Dismiss the dialog when the "Cancel" button is clicked
                            dialog.dismiss()
                        }
                        .setView(dialogView)
                        .create()

                    // Show the custom dialog
                    dialog.show()

                    // Set custom dialog button colors
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.RED)
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.GREEN)


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
                    arrayOf(Manifest.permission.READ_MEDIA_VIDEO),
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
                        try {
                            val file = File(pathC)
                            val artUriC = Uri.fromFile(file)
                            val video = VideoData(
                                title = titleC,
                                id = idC,
                                folderName = folderC,
                                duration = durationC,
                                size = sizeC,
                                path = pathC,
                                artUri = artUriC
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



    override fun onDestroy() {
        super.onDestroy()
        if (!PlayerMusicActivity.isPlaying && PlayerMusicActivity.musicService != null) {
            exitApplication()
        }
        FavoritesManager.saveFavorites(this)
    }

//    override fun onResume() {
//        super.onResume()
//
//        // Save favorites when the app resumes
//        FavoritesManager.saveFavorites(this)
//    }

}





