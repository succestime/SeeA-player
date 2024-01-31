package com.jaidev.seeaplayer

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
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
import android.widget.ImageView
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
import com.jaidev.seeaplayer.databinding.RenameFieldBinding
import com.jaidev.seeaplayer.databinding.VideoMoreFeaturesBinding
import java.io.File


class
MusicAdapter(
    private val context: Context,
    private var musicList: ArrayList<Music>,
    private var playlistDetails: Boolean = false,
    private val isMusic: Boolean = false,
    val selectionActivity: Boolean = false
)
    : RecyclerView.Adapter<MusicAdapter.MyAdapter>() {

    private  var newPosition = 0
    private lateinit var dialogRF: AlertDialog
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
    @SuppressLint("NotifyDataSetChanged")
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
                val customDialogRF =
                    LayoutInflater.from(context).inflate(R.layout.rename_field, holder.root, false)
                val bindingRF = RenameFieldBinding.bind(customDialogRF)
                val dialogRF = MaterialAlertDialogBuilder(context).setView(customDialogRF)
                    .setCancelable(false)
                    .setPositiveButton("Rename") { self, _ ->
                        val currentFile = File(musicList[position].path)
                        val newName = bindingRF.renameField.text
                        if (newName != null && currentFile.exists() && newName.toString()
                                .isNotEmpty()
                        ) {
                            val newFile = File(
                                currentFile.parentFile,
                                newName.toString() + "." + currentFile.extension
                            )
                            if (currentFile.renameTo(newFile)) {
                                MediaScannerConnection.scanFile(
                                    context, arrayOf(newFile.toString()),
                                    arrayOf("audio/*"), null
                                )
                                when {
                                    MainActivity.search -> {
                                        MainActivity.musicListSearch[position].title =
                                            newName.toString()
                                        MainActivity.musicListSearch[position].path = newFile.path
                                        MainActivity.musicListSearch[position].artUri =
                                            Uri.fromFile(newFile)

                                        MainActivity.dataChanged = true
                                        notifyItemChanged(position)
                                    }

                                    else -> {
                                        MainActivity.MusicListMA[position].title =
                                            newName.toString()
                                        MainActivity.MusicListMA[position].path = newFile.path
                                        MainActivity.MusicListMA[position].artUri =
                                            Uri.fromFile(newFile)
                                        Glide.with(context)
                                            .asBitmap()
                                            .load(musicList[position].artUri)
                                            .apply(RequestOptions().placeholder(R.drawable.speaker).centerCrop())
                                            .into(holder.image)
                                        MainActivity.dataChanged = true
                                        notifyItemChanged(position)
                                    }

                                }
                            } else {
                                Toast.makeText(context, "Permission Denied", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }

                        self.dismiss()
                    }
                    .setNegativeButton("Cancel") { self, _ ->
                        self.dismiss()
                    }
                    .create()
                dialogRF.show()
                bindingRF.renameField.text = SpannableStringBuilder(musicList[position].title)
                dialogRF.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
                    .setBackgroundColor(Color.BLACK)
                dialogRF.getButton(android.app.AlertDialog.BUTTON_NEGATIVE)
                    .setBackgroundColor(Color.BLACK)

            }
            bindingMf.deleteBtn.setOnClickListener {
                requestPermissionR()
                dialog.dismiss()
                val alertDialogBuilder = AlertDialog.Builder(context)
                val layoutInflater = LayoutInflater.from(context)
                val view = layoutInflater.inflate(R.layout.delete_alertdialog, null)


                val videoNameDelete = view.findViewById<TextView>(R.id.videoNameDelete)
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

                videoNameDelete.text = musicList[position].title

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


