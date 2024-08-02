package com.jaidev.seeaplayer.allAdapters

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.text.SpannableStringBuilder
import android.text.format.DateUtils
import android.text.format.Formatter
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.text.bold
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jaidev.seeaplayer.FoldersActivity
import com.jaidev.seeaplayer.MainActivity
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.dataClass.VideoData
import com.jaidev.seeaplayer.databinding.DetailsViewBinding
import com.jaidev.seeaplayer.databinding.VideoMoreFeaturesBinding
import java.io.File

class VideoSearchAdapter(
    private val context: Context,
    private var videoSearch: ArrayList<VideoData>,
    private val isSearchActivity: Boolean = false,
    private val isFolder: Boolean = false,
    private val isShort: Boolean = false,
    private val itemClickListener: OnItemClickListener,

    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var newPosition = 0
    private lateinit var dialogRF: AlertDialog
    private var sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    interface OnFileCountChangeListener {
        fun onFileCountChanged(newCount: Int)
    }

    interface VideoDeleteListener {
        fun onVideoDeleted()
    }
    interface OnItemClickListener {
        fun onItemClick(fileItem: VideoData)

    }

    private var videoDeleteListener: VideoDeleteListener? = null

    companion object {
        private const val PREF_NAME = "video_titles"
    }
    init {
        // Load saved music titles
        loadVideoTitles()
    }







    inner class ShortViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val videoName: TextView = itemView.findViewById(R.id.videoName)
        val videoImage: ImageView = itemView.findViewById(R.id.videoImage)

        fun bind(fileItem : VideoData) {
            val video = videoSearch[position]
           videoName.text = video.title

            Glide.with(context)
                .load(video.artUri)
                .apply(RequestOptions().placeholder(R.color.place_holder_video).centerCrop())
                .into(videoImage)

            itemView.setOnClickListener {

                itemClickListener.onItemClick(fileItem)

            }

        }
    }


    inner class LongViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val videoName: TextView = itemView.findViewById(R.id.videoName)
            val videoImage: ImageView = itemView.findViewById(R.id.videoImage)
            private val videoDuration: TextView = itemView.findViewById(R.id.duration)
            private val more: ImageButton = itemView.findViewById(R.id.MoreChoose)




            fun bind(fileItem : VideoData) {
                val video = videoSearch[position]
                videoName.text = video.title

                Glide.with(context)
                    .load(video.artUri)
                    .apply(RequestOptions().placeholder(R.color.place_holder_video).centerCrop())
                    .into(videoImage)
                videoDuration.text = DateUtils.formatElapsedTime(videoSearch[position].duration / 1000)

                more.setOnClickListener {
                    newPosition = position

                    // Inflate the custom dialog layout
                    val customDialog = LayoutInflater.from(context).inflate(R.layout.video_more_features, null)
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

                    // Set click listeners for dialog buttons
                    bindingMf.shareBtn.setOnClickListener {
                        dialog.dismiss()

                        // Create an ACTION_SEND intent to share the video
                        val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE)
                        shareIntent.type = "video/*"
                        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(videoSearch[position].path))
                        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                        // Get the list of apps that can handle the intent
                        val packageManager = context.packageManager
                        val resolvedActivityList = packageManager.queryIntentActivities(shareIntent, 0)
                        val excludedComponents = mutableListOf<ComponentName>()


                        // Iterate through the list and exclude your app
                        for (resolvedActivity in resolvedActivityList) {
                            if (resolvedActivity.activityInfo.packageName == context.packageName) {
                                excludedComponents.add(ComponentName(resolvedActivity.activityInfo.packageName, resolvedActivity.activityInfo.name))
                            }
                        }

                        // Create a chooser intent
                        val chooserIntent = Intent.createChooser(shareIntent, "Sharing Video")

                        // Exclude your app from the chooser intent
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            chooserIntent.putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, excludedComponents.toTypedArray())
                        }

                        chooserIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        ContextCompat.startActivity(context, chooserIntent, null)
                    }


                    bindingMf.infoBtn.setOnClickListener {
                        dialog.dismiss()
                        val customDialogIF = LayoutInflater.from(context).inflate(R.layout.details_view, null)
                        val bindingIF = DetailsViewBinding.bind(customDialogIF)
                        val dialogIF = MaterialAlertDialogBuilder(context)
                            .setView(customDialogIF)
                            .setCancelable(false)
                            .setPositiveButton("OK") { self, _ ->
                                self.dismiss()
                            }
                            .create()
                        dialogIF.show()

                        val infoText = SpannableStringBuilder().bold { append("DETAILS\n\nName : ") }
                            .append(videoSearch[position].title)
                            .bold { append("\n\nDuration : ") }
                            .append(DateUtils.formatElapsedTime(videoSearch[position].duration / 1000))
                            .bold { append("\n\nFile Size : ") }.append(
                                Formatter.formatShortFileSize(context, videoSearch[position].size.toLong())
                            )
                            .bold { append("\n\nLocation : ") }.append(videoSearch[position].path)
                        bindingIF.detailTV.text = infoText
                    }

                    bindingMf.renameBtn.setOnClickListener {
                        dialog.dismiss()
                        showRenameDialog(position, videoSearch[position].title)
                    }


                    bindingMf.deleteBtn.setOnClickListener {
                        dialog.dismiss()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
                            showPermissionRequestDialog()
                        } else {
                            showDeleteDialog(position)
                        }
                    }
                }

                itemView.setOnClickListener {
                    itemClickListener.onItemClick(fileItem)

                }
            }
        }
    private fun showRenameDialog(position: Int, defaultName: String) {
        val dialogBuilder = AlertDialog.Builder(context)

        // Set up the layout for the dialog
        val view = LayoutInflater.from(context).inflate(R.layout.rename_field, null)
        val editText = view.findViewById<EditText>(R.id.renameField)
        editText.setText(defaultName) // Set default text as current music title


        dialogBuilder.setView(view)
            .setTitle("Rename Video")
            .setMessage("Enter new name for the video:")
            .setCancelable(false)
            .setPositiveButton("Rename") { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty() && newName != defaultName) {
                    renameVideo(position, newName)
                    dialogRF.dismiss()
                } else {
                    Toast.makeText(context, "Please enter a valid name", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
        dialogRF = dialogBuilder.create()
        dialogRF.show()

    }
    @SuppressLint("NotifyDataSetChanged")
    private fun renameVideo(position: Int, newName: String) {
        val music = videoSearch[position]
        music.title = newName
        notifyItemChanged(position)
        saveVideoTitles(music.id, newName)
        val defaultTitle = music.title
        showRenameDialog(position, defaultTitle)
    }
    private fun saveVideoTitles(uniqueIdentifier: String, newName: String) {
        val editor = sharedPreferences.edit()
        editor.putString(uniqueIdentifier, newName)
        editor.apply()
    }

    // Load saved video titles from SharedPreferences
    private fun loadVideoTitles() {
        for (video in videoSearch) {
            val savedTitle = sharedPreferences.getString(video.id, null)
            savedTitle?.let {
                video.title = it
            }
        }
    }
    @RequiresApi(Build.VERSION_CODES.R)
    private fun showPermissionRequestDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.video_music_delete_permission_dialog, null)
        val alertDialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialogView.findViewById<Button>(R.id.buttonOpenSettings).setOnClickListener {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            intent.addCategory(Intent.CATEGORY_DEFAULT)
            intent.data = Uri.parse("package:${context.packageName}")
            ContextCompat.startActivity(context, intent, null)
            alertDialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.buttonNotNow).setOnClickListener {
            alertDialog.dismiss()
        }

        alertDialog.show()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun showDeleteDialog(position: Int) {
        val alertDialogBuilder = AlertDialog.Builder(context)
        val view = LayoutInflater.from(context).inflate(R.layout.delete_alertdialog, null)

        val videoNameDelete = view.findViewById<TextView>(R.id.videmusicNameDelete)
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
            .load(videoSearch[position].artUri)
            .apply(RequestOptions().placeholder(R.color.place_holder_video).centerCrop())
            .into(iconImageView)

        videoNameDelete.text = videoSearch[position].title

        alertDialogBuilder.setView(view)

        val alertDialog = alertDialogBuilder.create()

        deleteText.setOnClickListener {
            val file = File(videoSearch[position].path)
            if (file.exists() && file.delete()) {
                MediaScannerConnection.scanFile(context, arrayOf(file.path), null, null)
                when {
                    MainActivity.search -> {
                        MainActivity.dataChanged = true
                        videoSearch.removeAt(position)
                        notifyDataSetChanged()
                    }
                    isFolder -> {
                        MainActivity.dataChanged = true
                        FoldersActivity.currentFolderVideos.removeAt(position)
                        notifyDataSetChanged()
                        videoDeleteListener?.onVideoDeleted()
                    }
                    isShort -> {
                        MainActivity.dataChanged = true
                        videoSearch.removeAt(position)
                        notifyDataSetChanged()
                        videoDeleteListener?.onVideoDeleted()
                    }
                }
            } else {
                Toast.makeText(context, "Permission Denied!!", Toast.LENGTH_SHORT).show()
            }
            alertDialog.dismiss()
        }

        cancelText.setOnClickListener {
            alertDialog.dismiss()
        }
        alertDialog.show()
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val inflater = LayoutInflater.from(parent.context)
                return if (isSearchActivity) {
                    val longView = inflater.inflate(R.layout.grid_video_view_two, parent, false)
                    LongViewHolder(longView)
                }
                else {
                    val shortView = inflater.inflate(R.layout.video_view, parent, false)
                    ShortViewHolder(shortView)
                }

            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val fileItem = videoSearch[position]

                if (isSearchActivity) {
                    (holder as VideoSearchAdapter.LongViewHolder).bind(fileItem)

                }
                else {
                    (holder as VideoSearchAdapter.ShortViewHolder).bind(fileItem)

                }
                }


            override fun getItemCount(): Int {
                return videoSearch.size
            }


            @SuppressLint("NotifyDataSetChanged")
    fun updateList(searchList: ArrayList<VideoData>) {
        videoSearch = ArrayList()
        videoSearch.addAll(searchList)
        notifyDataSetChanged()
    }
}
