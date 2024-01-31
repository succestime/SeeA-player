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
import com.jaidev.seeaplayer.dataClass.VideoData
import com.jaidev.seeaplayer.databinding.DetailsViewBinding
import com.jaidev.seeaplayer.databinding.RenameFieldBinding
import com.jaidev.seeaplayer.databinding.VideoMoreFeaturesBinding
import com.jaidev.seeaplayer.databinding.VideoViewBinding
import java.io.File

class VideoAdapter(private val context: Context, private var videoList: ArrayList<VideoData>,private val isFolder: Boolean = false)
    : RecyclerView.Adapter<VideoAdapter.MyAdapter>() {

    private var newPosition = 0
    private lateinit var dialogRF: androidx.appcompat.app.AlertDialog

    private val layoutInflater: LayoutInflater = LayoutInflater.from(context)


    class MyAdapter(binding: VideoViewBinding) : RecyclerView.ViewHolder(binding.root) {

        var title = binding.videoName
        var duration = binding.duration
        val image = binding.videoImage
        val root = binding.root
        val more = binding.MoreChoose

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyAdapter {
        return MyAdapter(VideoViewBinding.inflate(layoutInflater, parent, false))
    }

    @RequiresApi(Build.VERSION_CODES.R)
    @SuppressLint("InflateParams", "NotifyDataSetChanged", "SuspiciousIndentation",
        "MissingInflatedId"
    )
    override fun onBindViewHolder(holder: MyAdapter, @SuppressLint("RecyclerView") position: Int) {
        holder.title.text = videoList[position].title
        holder.duration.text = DateUtils.formatElapsedTime(videoList[position].duration / 1000)
        Glide.with(context)
            .asBitmap()
            .load(videoList[position].artUri)
            .apply(RequestOptions().placeholder(R.mipmap.ic_logo_o).centerCrop())
            .into(holder.image)

        holder.root.setOnClickListener {
            when {
                isFolder -> {
                    PlayerActivity.pipStatus = 1
                    sendIntent(pos = position, ref = "FoldersActivity")
                }

                MainActivity.search -> {
                    PlayerActivity.pipStatus = 2
                    sendIntent(pos = position, ref = "SearchVideos")
                }

                videoList[position].id == PlayerActivity.nowPlayingId -> {
                    sendIntent(pos = position, ref = "NowPlaying")
                }

            }
        }

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
                shareIntent.type = "video/*"
                shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(videoList[position].path))
                startActivity(context, Intent.createChooser(shareIntent, "Sharing Video"), null)
            }

            bindingMf.infoBtn.setOnClickListener {
                dialog.dismiss()
                val customDialogIF =
                    LayoutInflater.from(context).inflate(R.layout.details_view, holder.root, false)
                val bindingIF = DetailsViewBinding.bind(customDialogIF)
                val dialogIF = MaterialAlertDialogBuilder(context).setView(customDialogIF)
                    .setCancelable(false)
                    .setPositiveButton("OK") { self, _ ->
                        self.dismiss()
                    }
                    .create()
                dialogIF.show()
                val infoText = SpannableStringBuilder().bold { append("DETAILS\n\nName : ") }
                    .append(videoList[position].title)
                    .bold { append("\n\nDuration : ") }
                    .append(DateUtils.formatElapsedTime(videoList[position].duration / 1000))
                    .bold { append("\n\nFile Size : ") }.append(
                        Formatter.formatShortFileSize(
                            context,
                            videoList[position].size.toLong()
                        )
                    )
                    .bold { append("\n\nLocation : ") }.append(videoList[position].path)
                bindingIF.detailTV.text = infoText


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
                        val currentFile = File(videoList[position].path)
                        val newName = bindingRF.renameField.text
                        if (newName != null && currentFile.exists() && newName.toString().isNotEmpty()) {
                            val newFile = File(
                                currentFile.parentFile,
                                newName.toString() + "." + currentFile.extension
                            )
                            if (currentFile.renameTo(newFile)) {
                                MediaScannerConnection.scanFile(context, arrayOf(newFile.toString()),
                                    arrayOf("video/*"), null)
                                when {
                                    MainActivity.search -> {
                                        MainActivity.searchList[position].title = newName.toString()
                                        MainActivity.searchList[position].path = newFile.path
                                        MainActivity.searchList[position].artUri = Uri.fromFile(newFile)
                                        MainActivity.dataChanged = true
                                        notifyItemChanged(position)
                                    }
                                    isFolder -> {
                                        FoldersActivity.currentFolderVideos[position].title = newName.toString()
                                        FoldersActivity.currentFolderVideos[position].path = newFile.path
                                        FoldersActivity.currentFolderVideos[position].artUri = Uri.fromFile(newFile)
                                        MainActivity.dataChanged = true
                                        notifyItemChanged(position)
                                    }
                                    else -> {
                                        MainActivity.videoList[position].title = newName.toString()
                                        MainActivity.videoList[position].path = newFile.path
                                        MainActivity.videoList[position].artUri = Uri.fromFile(newFile)
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
                bindingRF.renameField.text = SpannableStringBuilder(videoList[position].title)
                dialogRF.getButton(AlertDialog.BUTTON_POSITIVE).setBackgroundColor(Color.BLACK)
                dialogRF.getButton(AlertDialog.BUTTON_NEGATIVE).setBackgroundColor(Color.BLACK)

            }
            bindingMf.deleteBtn.setOnClickListener {
                requestPermissionR()
                dialog.dismiss()
                val alertDialogBuilder = AlertDialog.Builder(context)
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
                    .load(videoList[position].artUri)
                    .apply(RequestOptions().placeholder(R.mipmap.ic_logo_o).centerCrop())
                    .into(iconImageView)

                videoNameDelete.text = videoList[position].title

                alertDialogBuilder.setView(view)

                val alertDialog = alertDialogBuilder.create()

                deleteText.setOnClickListener {
                   val file = File(videoList[position].path)
                    if(file.exists() && file.delete()){
                        MediaScannerConnection.scanFile(context, arrayOf(file.path), null, null)
                        when{
                            MainActivity.search -> {
                                MainActivity.dataChanged = true
                              videoList.removeAt(position)
                                notifyDataSetChanged()
                            }
                            isFolder -> {
                                MainActivity.dataChanged = true
                                FoldersActivity.currentFolderVideos.removeAt(position)
                                notifyDataSetChanged()
                            }

                        }
                    }else{
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
        return videoList.size
    }

    private fun sendIntent(pos: Int, ref: String) {
        PlayerActivity.position = pos
        val intent = Intent(context, PlayerActivity::class.java)
        intent.putExtra("class", ref)
        startActivity(context, intent, null)

    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(searchList: ArrayList<VideoData>) {
        videoList = ArrayList()
        videoList.addAll(searchList)
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



