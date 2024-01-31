package com.jaidev.seeaplayer

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.text.SpannableStringBuilder
import android.text.format.DateUtils
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.core.text.bold
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jaidev.seeaplayer.dataClass.Music
import com.jaidev.seeaplayer.databinding.DetailsViewBinding
import com.jaidev.seeaplayer.databinding.MusicMoreFeatureBinding
import com.jaidev.seeaplayer.databinding.MusicViewBinding

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
                .inflate(R.layout.music_more_feature, holder.root, false)
            val bindingMf = MusicMoreFeatureBinding.bind(customDialog)
            val dialog = MaterialAlertDialogBuilder(context).setView(customDialog)
                .create()
            dialog.show()

            bindingMf.shareBtn2.setOnClickListener {

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
            bindingMf.infoBtn2.setOnClickListener {
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
//
//            bindingMf.renameMusicBtn.setOnClickListener {
//                dialog.dismiss()
//                requestWriteR()
//                notifyDataSetChanged()
//
//            }
//            bindingMf.deleteMusicBtn.setOnClickListener {
//                dialog.dismiss()
//                requestDeleteR(position = position)
//                notifyDataSetChanged()
//            }

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
//
//    @SuppressLint("ObsoleteSdkInt")
//    @RequiresApi(Build.VERSION_CODES.R)
//    private fun requestDeleteR(position: Int) {
//        // list of videos to delete
//        val uriList: List<Uri> = listOf(
//            Uri.withAppendedPath(
//                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
//                musicList[position].id
//            )
//        )
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//
//            // requesting for delete permission
//            val pi = MediaStore.createDeleteRequest(context.contentResolver, uriList)
//            (context as Activity).startIntentSenderForResult(
//                pi.intentSender, 125, null,
//                0, 0, 0, null
//            )
//
//        } else {
//            // for devices less than android 11
//            val file = File(musicList[position].path)
//            val builder = MaterialAlertDialogBuilder(context)
//            builder.setTitle("Delete Video ?")
//                .setTitle("Deleting Video ? ")
//                .setMessage(musicList[position].title)
//                .setPositiveButton("Yes") { self, _ ->
//                    if (file.exists() && file.delete()) {
//                        MediaScannerConnection.scanFile(context, arrayOf(file.path), null, null)
//                        updateDeleteUI(position = position)
//                    }
//                    self.dismiss()
//                }
//                .setNegativeButton("No"){self, _ -> self.dismiss() }
//            val delDialog = builder.create()
//            delDialog.show()
//            delDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.RED)
//            delDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.BLUE)
//
//        }
//
//    }
//    @SuppressLint("NotifyDataSetChanged")
//    private fun updateDeleteUI(position: Int) {
//        Log.d("MusicAdapter", "Deleting item at position $position")
//
//        when {
//            isMusic -> {
//                MainActivity.MusicListMA.removeAt(position)
//                notifyItemRemoved(position)
//                notifyDataSetChanged()
//                notifyItemRangeChanged(position, itemCount)
//                MainActivity.dataChanged = true
//            }
//            else -> {
//                MainActivity.MusicListMA.removeAt(position)
//                notifyItemRemoved(position)
//                notifyItemRangeChanged(position, itemCount)
//                notifyDataSetChanged()
//            }
//        }
//
//        // Log the size of the dataset after deletion
//        Log.d("MusicAdapter", "Dataset size after deletion: ${musicList.size}")
//    }


//    @SuppressLint("ObsoleteSdkInt")
//    @RequiresApi(Build.VERSION_CODES.R)
//    private fun requestWriteR(){
//        // files to modify
//        val uriList : List<Uri> =listOf(Uri.withAppendedPath(
//            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
//            , musicList[newPosition].id))
//
//        // requesting file write permission for specific files
//        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
//            val pi = MediaStore.createWriteRequest(context.contentResolver, uriList)
//            (context as Fragment).startIntentSenderForResult(pi.intentSender , 126 , null ,
//                0 ,0,0,null)
//        }else renameFunction(newPosition)
//
//    }
//
//    @SuppressLint("SuspiciousIndentation", "ObsoleteSdkInt")
//    private  fun renameFunction(position: Int) {
//        val customDialogRF = LayoutInflater.from(context).inflate(
//            R.layout.rename_field,
//            (context as Activity).findViewById(R.id.drawerLayoutMA),
//            false
//        )
//        val bindingRf = RenameFieldBinding.bind(customDialogRF)
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            dialogRF = MaterialAlertDialogBuilder(context).setView(customDialogRF)
//                .setCancelable(false)
//                .setPositiveButton("Rename") { self, _ ->
//                    val currentFile = File(musicList[position].path)
//                    val newName = bindingRf.renameField.text
//                    if (newName != null && currentFile.exists() && newName.toString()
//                            .isNotEmpty()
//                    ) {
//                        val newFile = File(
//                            currentFile.parentFile,
//                            newName.toString() + "." + currentFile.extension
//                        )
//                        val fromUri = Uri.withAppendedPath(
//                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, musicList[position].id
//                        )
//
//                        ContentValues().also {
//                            it.put(MediaStore.Files.FileColumns.IS_PENDING, 1)
//                            context.contentResolver.update(fromUri, it, null, null)
//                            it.clear()
//
//                            // updating file details
//                            it.put(MediaStore.Files.FileColumns.DISPLAY_NAME, newName.toString())
//                            it.put(MediaStore.Files.FileColumns.IS_PENDING, 0)
//                            context.contentResolver.update(fromUri, it, null, null)
//
//                        }
//                        updateRenameUI(position, newName = newName.toString(), newFile = newFile)
//                    }
//                    self.dismiss()
//                }
//                .setNegativeButton("Cancel") { self, _ ->
//                    self.dismiss()
//                }
//                .create()
//        } else {
//            dialogRF = MaterialAlertDialogBuilder(context).setView(customDialogRF)
//                .setCancelable(false)
//                .setPositiveButton("Rename") { self, _ ->
//                    val currentFile = File(musicList[position].path)
//                    val newName = bindingRf.renameField.text
//                    if (newName != null && currentFile.exists() && newName.toString().isNotEmpty()) {
//                        val newFile = File(currentFile.parentFile, newName.toString() + "." + currentFile.extension)
//                        if(currentFile.renameTo(newFile)){
//                            MediaScannerConnection.scanFile(context, arrayOf(newFile.toString()) , arrayOf("audio/*"), null)
//                            updateRenameUI(position = position , newName= newName.toString(), newFile = newFile)
//                        }
//                    }
//                    self.dismiss()
//                }
//                .setNegativeButton("Cancel"){self, _ ->
//                    self.dismiss()
//                }
//                .create()
//        }
//
//        bindingRf.renameField.text = SpannableStringBuilder(musicList[newPosition].title)
//        dialogRF.show()
//        dialogRF.getButton(AlertDialog.BUTTON_POSITIVE).setBackgroundColor(
//            MaterialColors.getColor(context, com.bumptech.glide.R.attr.theme , Color.BLACK))
//        dialogRF.getButton(AlertDialog.BUTTON_NEGATIVE).setBackgroundColor(
//            MaterialColors.getColor(context, com.bumptech.glide.R.attr.theme , Color.BLACK))
//    }

//    private fun updateRenameUI(position: Int, newName:String, newFile: File){
//        when{
//            isMusic -> {
//                MainActivity.MusicListMA[position].title = newName
//                MainActivity.MusicListMA[position].path = newFile.path
//                MainActivity.MusicListMA[position].artUri = Uri.fromFile(newFile)
//                notifyItemChanged(position)
//                MainActivity.dataChanged = true
//
//            }
//            else -> {
//                   MainActivity.MusicListMA[position].title = newName
//                MainActivity.MusicListMA[position].path = newFile.path
//                MainActivity.MusicListMA[position].artUri = Uri.fromFile(newFile)
//                notifyItemChanged(position)
//
//
//            }
//        }
//    }

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

//    private  fun requestPermissionR(){
//        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
//            intent.addCategory("android.intent.category.DEFAULT")
//            intent.data = Uri.parse("package:${context.applicationContext.packageName}")
//            ContextCompat.startActivity(context, intent, null)
//        }
//
//    }
//    fun onResult(requestCode : Int , resultCode : Int){
//        when(requestCode){
//            125 -> {
//                if(resultCode == Activity.RESULT_OK) updateDeleteUI(newPosition)
//
//            }
//            126 -> if(resultCode == Activity.RESULT_OK) renameFunction(newPosition)
//        }
//    }
}


