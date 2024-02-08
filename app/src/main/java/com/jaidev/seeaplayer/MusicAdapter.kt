package com.jaidev.seeaplayer

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.text.SpannableStringBuilder
import android.text.format.DateUtils
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.core.text.bold
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jaidev.seeaplayer.dataClass.Music
import com.jaidev.seeaplayer.databinding.DetailsViewBinding
import com.jaidev.seeaplayer.databinding.MusicViewBinding
import com.jaidev.seeaplayer.databinding.VideoMoreFeaturesBinding
import java.io.File


class MusicAdapter(
    private val context: Context,
    private var musicList: ArrayList<Music>,
    private var playlistDetails: Boolean = false,
    private val isMusic: Boolean = false,
    val selectionActivity: Boolean = false
)
    : RecyclerView.Adapter<MusicAdapter.MyAdapter>() {

    private  var newPosition = 0
    private lateinit var dialogRF: AlertDialog
    private lateinit var sharedPreferences: SharedPreferences
    companion object {
        private const val REQUEST_CODE_PERMISSION = 1001
        private const val PREF_NAME = "music_titles"
    }
    init {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        // Load saved music titles
        loadMusicTitles()
    }
    interface MusicDeleteListener {
        fun onMusicDeleted()
    }

    private var musicDeleteListener: MusicDeleteListener? = null

    fun setVideoDeleteListener(listener: MusicDeleteListener) {
        musicDeleteListener = listener
    }

    class MyAdapter(binding: MusicViewBinding) : RecyclerView.ViewHolder(binding.root) {
        val title = binding.songName
        val album = binding.songAlbum
        val image = binding.musicViewImage
        val root = binding.root
        val more = binding.MoreChoose2
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyAdapter {
        return MyAdapter(MusicViewBinding.inflate(LayoutInflater.from(context), parent, false))
    }

    @RequiresApi(Build.VERSION_CODES.R)
    @SuppressLint("NotifyDataSetChanged", "MissingInflatedId")
    override fun onBindViewHolder(holder: MyAdapter, @SuppressLint("RecyclerView") position: Int) {
        holder.title.text = musicList[position].title
        holder.album.text = musicList[position].album
        Glide.with(context)
            .asBitmap()
            .load(musicList[position].artUri)
            .apply(RequestOptions().placeholder(R.drawable.speaker).centerCrop())
            .into(holder.image)

        holder.more.setOnClickListener {
            newPosition = position
            val customDialog = LayoutInflater.from(context)
                .inflate(R.layout.video_more_features, holder.root, false)
            val bindingMf = VideoMoreFeaturesBinding.bind(customDialog)
            val dialog = MaterialAlertDialogBuilder(context).setView(customDialog)
                .create()
            dialog.show()

            bindingMf.shareBtn.setOnClickListener {

                dialog.dismiss()
                val shareIntent = Intent()
                shareIntent.action = Intent.ACTION_SEND
                shareIntent.type = "audio/*"
                shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(musicList[position].path))
                startActivity(
                    context,
                    Intent.createChooser(shareIntent, "Sharing Music File!!"),
                    null
                )


            }
            bindingMf.infoBtn.setOnClickListener {
                dialog.dismiss()
                val customDialogIf = LayoutInflater.from(context)
                    .inflate(R.layout.details_view, holder.root, false)
                val bindingIf = DetailsViewBinding.bind(customDialogIf)
                val dialogIf = MaterialAlertDialogBuilder(context).setView(customDialogIf)
                    .setCancelable(false)
                    .setPositiveButton("OK") { self, _ ->


                        self.dismiss()
                    }
                    .create()
                dialogIf.show()
                val infoText = SpannableStringBuilder().bold { append("DETAILS\n\nName : ") }
                    .append(musicList[position].title)
                    .bold { append("\n\nDuration : ") }
                    .append(DateUtils.formatElapsedTime(musicList[position].duration / 1000))
                    .bold { append("\n\nFile Size : ") }.append(
                        Formatter.formatShortFileSize(
                            context,
                            musicList[position].size.toLong()
                        )
                    )
                    .bold { append("\n\nLocation : ") }.append(musicList[position].path)
                bindingIf.detailTV.text = infoText
            }

            bindingMf.renameBtn.setOnClickListener {
                dialog.dismiss()
                requestPermissionR()
                // Get the current music title as default text
                val defaultTitle = musicList[position].title
                // Show the rename dialog with the current music title as default text
                showRenameDialog(position, defaultTitle)
            }

            bindingMf.deleteBtn.setOnClickListener {
                requestPermissionR()
                dialog.dismiss()
                val alertDialogBuilder = AlertDialog.Builder(context)
                val layoutInflater = LayoutInflater.from(context)
                val view = layoutInflater.inflate(R.layout.delete_alertdialog, null)


                val musicNameDelete = view.findViewById<TextView>(R.id.videmusicNameDelete)
                val deleteText = view.findViewById<TextView>(R.id.deleteText)
                val cancelText = view.findViewById<TextView>(R.id.cancelText)
                val iconImageView = view.findViewById<ImageView>(R.id.videoImage)
                // Set the delete text color to red
                deleteText.setTextColor(ContextCompat.getColor(context, R.color.red))

                // Set the cancel text color to black
                cancelText.setTextColor(ContextCompat.getColor(context, R.color.black))

                // Load video image into iconImageView using Glide
                Glide.with(context)
                    .asBitmap()
                    .load(musicList[position].artUri)
                    .apply(RequestOptions().placeholder(R.mipmap.ic_logo_o).centerCrop())
                    .into(iconImageView)

                musicNameDelete.text = musicList[position].title

                alertDialogBuilder.setView(view)

                val alertDialog = alertDialogBuilder.create()
                deleteText.setOnClickListener {
                    val file = File(musicList[position].path)
                    if (file.exists() && file.delete()) {
                        MediaScannerConnection.scanFile(context, arrayOf(file.path), null, null)
                        when {
                            MainActivity.search -> {
                                MainActivity.dataChanged = true
                                musicList.removeAt(position)
                                notifyDataSetChanged()
                            }

                            else -> {
                                MainActivity.dataChanged = true
                                MainActivity.MusicListMA.removeAt(position)
                                notifyDataSetChanged()
                                musicDeleteListener?.onMusicDeleted()                            }

                        }
                    } else {
                        Toast.makeText(context, "Permission Denied!!", Toast.LENGTH_SHORT).show()
                    }
                    alertDialog.dismiss()
                }

                cancelText.setOnClickListener {
                    // Handle cancel action here
                    alertDialog.dismiss()
                }
                alertDialog.show()
            }


        }


            when {
            playlistDetails -> {
                holder.root.setOnClickListener {
                    sendIntent(ref = "PlaylistDetailsAdapter", pos = position)
                }
            }
            selectionActivity ->{
                holder.root.setOnClickListener {
                    if(addSong(musicList[position]))
                        holder.root.setBackgroundColor(ContextCompat.getColor(context, R.color.cool_green))
                    else
                        holder.root.setBackgroundColor(ContextCompat.getColor(context, R.color.white))

                }
            }
            else -> {
                holder.root.setOnClickListener {
                    when {
                        MainActivity.search -> sendIntent(ref = "MusicAdapterSearch", pos = position)

                       musicList[position].id == PlayerMusicActivity.nowMusicPlayingId ->
                            sendIntent(ref = "NowPlaying", pos = PlayerMusicActivity.songPosition)

                        else -> sendIntent(ref = "MusicAdapter", pos = position)
                    }
                }
            }
        }

    }

    override fun getItemCount(): Int {
            return musicList.size
        }
    private fun renameMusic(position: Int, newName: String) {
        val oldMusic = musicList[position]
        val newMusic = oldMusic.copy(title = newName)
        musicList[position] = newMusic
        notifyItemChanged(position)
        // Save updated music title to SharedPreferences
        saveMusicTitle(position, newName)
        // Get the current music title as default text
        val defaultTitle = musicList[position].title
        // Show the rename dialog with the current music title as default text
        showRenameDialog(position, defaultTitle)

    }


    private fun showRenameDialog(position: Int, defaultTitle: String) {
        val dialogBuilder = AlertDialog.Builder(context)

        // Set up the layout for the dialog
        val view = LayoutInflater.from(context).inflate(R.layout.rename_field, null)
        val editText = view.findViewById<EditText>(R.id.renameField)
        editText.setText(defaultTitle) // Set default text as current music title

        dialogBuilder.setView(view)
            .setTitle("Rename Music")
            .setMessage("Enter new name for the music:")
            .setCancelable(false)
            .setPositiveButton("Rename") { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty()) {
                    renameMusic(position, newName)
                    // Dismiss the dialog after performing the rename action
                    dialogRF.dismiss()
                } else {
                    Toast.makeText(context, "Name can't be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel") { self, _ ->
                self.dismiss()
            }
        dialogRF = dialogBuilder.create()
        dialogRF.show()

        val positiveButton = dialogRF.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
        val negativeButton = dialogRF.getButton(android.app.AlertDialog.BUTTON_NEGATIVE)
        positiveButton.setBackgroundColor(Color.BLACK)
        negativeButton.setBackgroundColor(Color.BLACK)

        // Set margins between the buttons
        val layoutParams = positiveButton.layoutParams as LinearLayout.LayoutParams
        layoutParams.setMargins(0, 0, 50, 0) // Add margin to the right of the positive button
        positiveButton.layoutParams = layoutParams

        val negativeLayoutParams = negativeButton.layoutParams as LinearLayout.LayoutParams
        negativeLayoutParams.setMargins(0, 0, 100, 0) // Add margin to the left of the negative button
        negativeButton.layoutParams = negativeLayoutParams
    }


    private fun saveMusicTitle(position: Int, newName: String) {
        val editor = sharedPreferences.edit()
        editor.putString(position.toString(), newName)
        editor.apply()
    }

    private fun loadMusicTitles() {
        for (i in 0 until musicList.size) {
            val savedTitle = sharedPreferences.getString(i.toString(), null)
            savedTitle?.let {
                musicList[i] = musicList[i].copy(title = it)
            }
        }
    }



    private fun sendIntent(ref: String, pos: Int){
        val intent = Intent(context, PlayerMusicActivity::class.java)
        intent.putExtra("index", pos)
        intent.putExtra("class", ref)
        startActivity(context, intent, null)
    }
    @SuppressLint("NotifyDataSetChanged")
    fun updateMusicList(searchList : ArrayList<Music>){
        musicList = ArrayList()
        musicList.addAll(searchList)
        notifyDataSetChanged()
    }





    private fun addSong(song: Music): Boolean{
        PlaylistActivity.musicPlaylist.ref[PlaylistDetails.currentPlaylistPos].playlist.forEachIndexed { index, music ->
            if(song.id == music.id){
                PlaylistActivity.musicPlaylist.ref[PlaylistDetails.currentPlaylistPos].playlist.removeAt(index)
                return false
            }
        }
        PlaylistActivity.musicPlaylist.ref[PlaylistDetails.currentPlaylistPos].playlist.add(song)
        return true
    }
    @SuppressLint("NotifyDataSetChanged")
    fun refreshPlaylist(){
       musicList = ArrayList()
        musicList = PlaylistActivity.musicPlaylist.ref[PlaylistDetails.currentPlaylistPos].playlist
        notifyDataSetChanged()
    }

    //for requesting android 11 or higher storage permission
    private fun requestPermissionR() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.addCategory("android.intent.category.DEFAULT")
                intent.data = Uri.parse("package:${context.applicationContext.packageName}")
                ContextCompat.startActivity(context, intent, null)
            }
        }
    }



}

// Hi new Jaidev sharma i am old JaiDev Sharma i want to tell you that
// i am leaving more error for yoy but don't worry .How much you will solve the error
// you will learn about your app SeeA Player and that much you will be able
// get comfident  of you big Playn how you Will Present Your Idea but i want that preavious Tow or Three days
// i am getting for uncomfertable because of my body and also by error error but
// today i get some relafe By gitHub,  because i added my all SeeA Player code to gitHub
// firstly i want to you
// password fo my gitHub account "jai@8851035394"
// UserName of my gitHub account "succestime"

///////////////////////////////////////////
///////////////////////////////////////////////////

// now i want to tell you that i am proude of you because you did not hesitate or afferade by by error
// i know that by my toking error will not solve or
// i cnt't give you more confidense to go forward
// i want to you remember about aur carecters future how we are going to live
// like a King in that secret way and
// how you are going to make this india to akhand Bharate
// and i you made the complex plane to india to akhand Bharate also remember what you can do my money like cars, food etc.

///////////////////////////////////////////
///////////////////////////////////////////////////
// also remember about your love what you image about your love if
// you want your love and that newGeneration what your are doing  now and what you done in past ,
//   what you learn about life , tell storeys of your all problems.
//   if you want all think what you image read this think and go forward  and solve all errors

///////////////////////////////////////////
///////////////////////////////////////////////////
//////////////////////////////////////////////////// ///////////////////////////////////////////////
//  i get idea one time you can check this also ask to chatGpt that ///////////////////////////////////////
// give me code of delete functionality then also say to it that if ///////////////////////////////////////
// i want to also update the recycleview /////////////////////////////////////////
// "is any video deleted to remove also in other recycleView "  //////////////////////////////////////////
//also for Rename functionality//////////////////////////////////////////////////



//     now error thinks
// in video player Delete and Rename functionality is working good for android 11
// but in recantVideo rename and relete functinolaty is not updating

// in music Player Delete functionality is working good
// but rename functionality is not working good
// Note => Delete and Rename functionality update to working


// after doing these error you have to
// update delete and Rename functionality in both recantvideo and recantmusic fregment


// after doing this now you have to to add "chipe" click functionality on there respective fregment

// after this dicide what you want to do

// my now decesion it go to  profile and suscribe

// learn the hanuman bagvan ji and shive ji and do what you are doing

