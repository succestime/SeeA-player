
package com.jaidev.seeaplayer

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.text.SpannableStringBuilder
import android.text.format.DateUtils
import android.text.format.Formatter
import android.view.ActionMode
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
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

    private var newPosition = 0
    private lateinit var dialogRF: AlertDialog
    private lateinit var sharedPreferences: SharedPreferences

    // Tracks selected items
    private val selectedItems = HashSet<Int>()
    private var actionMode: ActionMode? = null

    companion object {
        private const val PREF_NAME = "music_titles"
    }
    private val layoutInflater: LayoutInflater = LayoutInflater.from(context)
    init {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        // Load saved music titles
        loadMusicTitles()
    }

    interface MusicDeleteListener {
        fun onMusicDeleted()
    }

    private var musicDeleteListener: MusicDeleteListener? = null

    fun setMusicDeleteListener(listener: MusicDeleteListener) {
        musicDeleteListener = listener
    }


    class MyAdapter(binding: MusicViewBinding) : RecyclerView.ViewHolder(binding.root) {
        val title = binding.songName
        val album = binding.songAlbum
        val image = binding.musicViewImage
        val root = binding.root
        val more = binding.MoreChoose2
        val button = binding.multiIcon
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
            .apply(RequestOptions().placeholder(R.drawable.music_speaker_three).centerCrop())
            .into(holder.image)

        holder.root.setOnLongClickListener {
            toggleSelection(position)
            true
        }

        holder.root.setOnClickListener {
            if (actionMode != null) {
                // If action mode is active, toggle selection as usual
                toggleSelection(position)
            } else {

                when {
                    MainActivity.search -> sendIntent(
                        ref = "MusicAdapterSearch",
                        pos = position
                    )

                    musicList[position].id == PlayerMusicActivity.nowMusicPlayingId ->
                        sendIntent(
                            ref = "NowPlaying",
                            pos = PlayerMusicActivity.songPosition
                        )

                    else -> sendIntent(ref = "MusicAdapter", pos = position)

                }
            }



        }

        when {
            playlistDetails -> {
                holder.root.setOnClickListener {
                    sendIntent(ref = "PlaylistDetailsAdapter", pos = position)
                }
            }

            selectionActivity -> {
                holder.root.setOnClickListener {
                    if (addSong(musicList[position]))
                        holder.root.setBackgroundColor(
                            ContextCompat.getColor(
                                context,
                                R.color.cool_green
                            )
                        )
                    else
                        holder.root.setBackgroundColor(
                            ContextCompat.getColor(
                                context,
                                R.color.white
                            )
                        )

                }
            }


        }
        // Show/hide multi-select icon based on selection
        holder.button.visibility = if (selectedItems.contains(position)) View.VISIBLE else View.GONE

        holder.more.setOnClickListener {
            newPosition = position
            // Inflate the custom dialog layout
            val customDialog = LayoutInflater.from(context).inflate(R.layout.video_more_features, holder.root, false)
            val bindingMf = VideoMoreFeaturesBinding.bind(customDialog)
            // Create the dialog
            val dialogBuilder = MaterialAlertDialogBuilder(context)
                .setView(customDialog)
            val dialog = dialogBuilder.create()

            // Show the dialog
            dialog.show()

            // Get the window attributes of the dialog
            val window = dialog.window
            window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT) // Set dialog width and height
            window?.setGravity(Gravity.BOTTOM) // Set dialog gravity to bottom

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
                // Get the current music title as default text
                val defaultTitle = musicList[position].title
                // Show the rename dialog with the current music title as default text
                showRenameDialog(position, defaultTitle)
            }

            bindingMf.deleteBtn.setOnClickListener {
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
                                musicDeleteListener?.onMusicDeleted()
                            }

                            isMusic -> {
                                MainActivity.dataChanged = true
                                MainActivity.MusicListMA.removeAt(position)
                                notifyDataSetChanged()
                                musicDeleteListener?.onMusicDeleted()
                            }


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
    }

    override fun getItemCount(): Int {
        return musicList.size
    }

    // Toggle selection for multi-select
    // Toggle selection for multi-select
    @SuppressLint("NotifyDataSetChanged")
    private fun toggleSelection(position: Int) {
        if (selectedItems.contains(position)) {
            selectedItems.remove(position)
        } else {
            selectedItems.add(position)
        }

        // Start or finish action mode based on selection
        if (selectedItems.isEmpty()) {
            actionMode?.finish()
        } else {
            startActionMode()
        }

        // Update selected items
        notifyDataSetChanged()
        actionMode?.invalidate()
    }

    // Start action mode for multi-select
    private fun startActionMode() {
        if (actionMode == null) {
            actionMode = (context as AppCompatActivity).startActionMode(actionModeCallback)
        }
        actionMode?.title = "${selectedItems.size} selected"
    }

    // Action mode callback
    private val actionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            // Inflate action mode menu
            mode?.menuInflater?.inflate(R.menu.multiple_select_menu, menu)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
//            // Hide the menu_rename item if more than one item is selected
//            val renameItem = menu?.findItem(R.id.menu_rename)
//            renameItem?.isVisible = selectedItems.size == 1

            return true
        }

        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            // Handle action mode menu items
            val actionMode = mode
            when (item?.itemId) {
                R.id.shareMultiBrowser -> {
                    deleteSelectedVideos(actionMode, newPosition)

                    return true
                }

                R.id.deleteMultiBrowser -> {
// Call the showRenameDialog method here
                    if (selectedItems.size == 1) {
                        val selectedPosition = selectedItems.first()
                        val defaultName = musicList[selectedPosition].title
                        showRenameDialog(selectedPosition, defaultName)
                    } else {
                        Toast.makeText(
                            context,
                            "Please select only one video to rename",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return true
                }
                // Add more action mode items as needed
            }
            return false
        }

        @SuppressLint("NotifyDataSetChanged")
        override fun onDestroyActionMode(mode: ActionMode?) {
            // Clear selection and action mode
            selectedItems.clear()
            actionMode = null
            notifyDataSetChanged()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun deleteSelectedVideos(actionMode: ActionMode?, @SuppressLint("RecyclerView") position: Int) {
        val selectedPositions = ArrayList(selectedItems)
        val deleteDialogBuilder = AlertDialog.Builder(context)
        val deleteView = layoutInflater.inflate(R.layout.delete_alertdialog, null)

        val deleteText = deleteView.findViewById<TextView>(R.id.deleteText)
        val cancelText = deleteView.findViewById<TextView>(R.id.cancelText)
        val iconImageView = deleteView.findViewById<ImageView>(R.id.videoImage)
        val videoNameDelete = deleteView.findViewById<TextView>(R.id.videmusicNameDelete)

        // Set the delete text color to red
        deleteText.setTextColor(ContextCompat.getColor(context, R.color.red))

        // Set the cancel text color to black
        cancelText.setTextColor(ContextCompat.getColor(context, R.color.black))

        // Load video image into iconImageView using Glide
        Glide.with(context)
            .asBitmap()
            .load(musicList[selectedPositions.first()].artUri) // Assuming only one item is selected
            .apply(RequestOptions().placeholder(R.mipmap.ic_logo_o).centerCrop())
            .into(iconImageView)

        // Set the video name
        videoNameDelete.text = musicList[selectedPositions.first()].title // Assuming only one item is selected

        deleteDialogBuilder.setView(deleteView)

        val deleteDialog = deleteDialogBuilder.create()

        deleteText.setOnClickListener {
            for (position in selectedPositions) {
                val file = File(musicList[position].path)
                if (file.exists() && file.delete()) {
                    MediaScannerConnection.scanFile(context, arrayOf(file.path), null, null)
                    when {
                        MainActivity.search -> {
                            MainActivity.dataChanged = true
                            musicList.removeAt(position)
                            notifyDataSetChanged()
                            musicDeleteListener?.onMusicDeleted()
                        }

                        isMusic -> {
                            MainActivity.dataChanged = true
                            MainActivity.MusicListMA.removeAt(position)
                            notifyDataSetChanged()
                            musicDeleteListener?.onMusicDeleted()
                        }

                    }
                } else {
                    Toast.makeText(context, "Permission Denied!!", Toast.LENGTH_SHORT).show()
                }
            }
            deleteDialog.dismiss()
            actionMode?.finish()
            // You might want to call updateTotalVideoCount() here to refresh the total video count
        }

        cancelText.setOnClickListener {
            // Handle cancel action here
            deleteDialog.dismiss()
        }
        deleteDialog.show()
    }







    private fun renameMusic(position: Int, newName: String) {
        val music = musicList[position]
        music.title = newName
        notifyItemChanged(position)
        saveMusicTitle(music.id, newName)
        val defaultTitle = music.title
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

    }


    private fun saveMusicTitle(uniqueIdentifier: String, newName: String) {
        val editor = sharedPreferences.edit()
        editor.putString(uniqueIdentifier, newName)
        editor.apply()
    }


    private fun loadMusicTitles() {
        for (music in musicList) {
            val savedTitle = sharedPreferences.getString(music.id, null)
            savedTitle?.let {
                music.title = it
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




}
