package com.jaidev.seeaplayer.allAdapters.VideoPlaylistAdapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.text.format.DateUtils
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.imageview.ShapeableImageView
import com.jaidev.seeaplayer.PlayerActivity
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.VideoPlaylistFunctionality.PlaylistVideoActivity
import com.jaidev.seeaplayer.browserActivity.PlayerFileActivity
import com.jaidev.seeaplayer.dataClass.VideoData
import java.text.NumberFormat

class PlaylistVideoShowAdapter(
    private val context: Context,
    private var videoList: ArrayList<VideoData>,
    private val onVideoRemoved: (VideoData) -> Unit
) : RecyclerView.Adapter<PlaylistVideoShowAdapter.VideoViewHolder>() {

    private val selectedItems = mutableSetOf<Int>()

    private var isAllSelected = false // Add this flag
    var isSelectionMode = false
    interface OnSelectionChangeListener {


        fun onSelectionChanged(isAllSelected: Boolean)
    }

    var selectionChangeListener: OnSelectionChangeListener? = null
    fun getVideos(): ArrayList<VideoData> {
        return videoList
    }

    @SuppressLint("NotifyDataSetChanged", "SuspiciousIndentation")
    fun updateVideoList(newList: ArrayList<VideoData>) {
      videoList = newList
        notifyDataSetChanged()
    }
    fun getSelectedVideos(): List<VideoData> {
        return videoList.filter { it.selected }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun clearSelection() {
        selectedItems.clear()
        isSelectionMode = true
        notifyDataSetChanged()
        selectionChangeListener?.onSelectionChanged(isAllSelected = false)

        // Notify the activity to update UI elements like the title
        val activity = context as PlaylistVideoActivity
        activity.updateSelectionMode(true)
        activity.updatePlaylistName(0)
    }
    @SuppressLint("NotifyDataSetChanged")
    private fun updateSelectionMode() {
        val activity = context as PlaylistVideoActivity

        // Update the playlist name to show the number of selected items
        val selectedCount = selectedItems.size
        activity.updatePlaylistName(selectedCount)

        // Keep the selection mode active even if no items are selected
        isSelectionMode = true
        activity.updateSelectionMode(true)

        // Trigger UI updates
        notifyDataSetChanged()

        // Notify the listener about the selection state
        selectionChangeListener?.onSelectionChanged(selectedCount == videoList.size)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun disableSelectionMode() {
        isSelectionMode = false
        selectedItems.clear()
        notifyDataSetChanged()

        // Notify the activity to update UI elements like the title
        val activity = context as PlaylistVideoActivity
        activity.updateSelectionMode(false)
        activity.updatePlaylistName(0) // Set to "0 videos selected"
    }

    @SuppressLint("NotifyDataSetChanged")
    fun selectAllVideos(select: Boolean) {
        selectedItems.clear()
        if (select) {
            videoList.forEachIndexed { index, video ->
                video.selected = true
                selectedItems.add(index)
            }
        } else {
            videoList.forEach { video ->
                video.selected = false
            }
        }
        notifyDataSetChanged()
        isSelectionMode = true
        val activity = context as PlaylistVideoActivity
        activity.updateSelectionMode(true)
        activity.updatePlaylistName(selectedItems.size)
        selectionChangeListener?.onSelectionChanged(select)
    }

    inner class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.videoName)
        val durationTextView: TextView = itemView.findViewById(R.id.duration)
        val videoImage: ShapeableImageView = itemView.findViewById(R.id.videoImage)
        val moreChoose: ImageButton = itemView.findViewById(R.id.MoreChoose)
        val emptyCheck: ImageButton = itemView.findViewById(R.id.emptyCheck)
        val fillCheck: ImageButton = itemView.findViewById(R.id.fillCheck)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.playlist_video_video_view, parent, false)
        return VideoViewHolder(view)
    }

    @SuppressLint("NotifyDataSetChanged", "UseCompatLoadingForDrawables")
    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val video = videoList[position]
        holder.titleTextView.text = video.title
        holder.durationTextView.text = DateUtils.formatElapsedTime(video.duration / 1000)
        Glide.with(context)
            .load(video.artUri)
            .apply(RequestOptions().placeholder(R.color.place_holder_video).centerCrop())
            .into(holder.videoImage)


        // If selection mode is enabled
        if (isSelectionMode) {
            holder.emptyCheck.visibility = View.VISIBLE
            holder.moreChoose.visibility = View.GONE

            // If the item is selected
            if (selectedItems.contains(position)) {
                holder.fillCheck.visibility = View.VISIBLE
                holder.emptyCheck.visibility = View.GONE
            } else {
                holder.fillCheck.visibility = View.GONE
                holder.emptyCheck.visibility = View.VISIBLE
            }
        } else {
            // If selection mode is not enabled
            holder.emptyCheck.visibility = View.GONE
            holder.fillCheck.visibility = View.GONE
            holder.moreChoose.visibility = View.VISIBLE
        }
        holder.itemView.setOnClickListener {
            if (isSelectionMode) {
                toggleSelection(position)
            } else {
                sendIntent(pos = position, ref = "playlistPlaying")

            }
        }

        // Handle long click to enable selection mode
        holder.itemView.setOnLongClickListener {
            if (!isSelectionMode) {
                isSelectionMode = true
                updateSelectionMode()
                notifyDataSetChanged() // Refresh to show/hide checkboxes for all items
            }
            toggleSelection(position)
            true
        }

        // Handle more button click only when not in selection mode
        holder.moreChoose.setOnClickListener {
            if (!isSelectionMode) {
                showBottomSheetDialog(video)
            }
        }
    }
    private fun sendIntent(pos: Int, ref: String) {
        PlayerActivity.position = pos
        val intent = Intent(context, PlayerActivity::class.java)
        intent.putExtra("class", ref)

        ContextCompat.startActivity(context, intent, null)

    }
    private fun toggleSelection(position: Int) {
        if (selectedItems.contains(position)) {
            selectedItems.remove(position)
        } else {
            selectedItems.add(position)
        }
        val video = videoList[position]
        video.selected = !video.selected
        notifyItemChanged(position)
        updateSelectionMode()
    }

    @SuppressLint("UseCompatLoadingForDrawables", "SetTextI18n")
    private fun showBottomSheetDialog(video: VideoData) {
        val bottomSheetDialog = BottomSheetDialog(context)
        val view = LayoutInflater.from(context).inflate(R.layout.playlist_video_bottom_sheet, null)
        bottomSheetDialog.setContentView(view)
        view.background = context.getDrawable(R.drawable.rounded_bottom_sheet_2)

        val videoTitle: TextView = view.findViewById(R.id.videoTitle)
        val videoThumbnail: ShapeableImageView = view.findViewById(R.id.videoThumbnail)
        val duration: TextView = view.findViewById(R.id.duration)

        duration.text = DateUtils.formatElapsedTime(video.duration / 1000)
        videoTitle.text = video.title
        Glide.with(context)
            .load(video.artUri)
            .apply(RequestOptions().placeholder(R.color.place_holder_video).centerCrop())
            .into(videoThumbnail)

        val playOptionLayout: LinearLayout = view.findViewById(R.id.playOptionLayout)
        val removeOptionLayout: LinearLayout = view.findViewById(R.id.removeOptionLayout)
        val propertiesOptionLayout: LinearLayout = view.findViewById(R.id.propertiesOptionLayout)

        playOptionLayout.setOnClickListener {
            val intent = Intent(context, PlayerFileActivity::class.java).apply {
                putExtra("videoUri", video.path)
                putExtra("videoTitle", video.title)
            }
            context.startActivity(intent)
            bottomSheetDialog.dismiss()
        }

        removeOptionLayout.setOnClickListener {
            val builder = android.app.AlertDialog.Builder(context)
            val dialogView = LayoutInflater.from(context).inflate(R.layout.playlist_remove, null)
            builder.setView(dialogView)

            val dialog = builder.create()

            val cancelButton: Button = dialogView.findViewById(R.id.cancel_button)
            val removeButton: Button = dialogView.findViewById(R.id.remove_button)

            cancelButton.setOnClickListener {
                dialog.dismiss()
            }

            removeButton.setOnClickListener {
                onVideoRemoved(video)
                dialog.dismiss()
                bottomSheetDialog.dismiss()
            }

            dialog.show()
            bottomSheetDialog.dismiss()
        }

        propertiesOptionLayout.setOnClickListener {
            val customDialogIF = LayoutInflater.from(context).inflate(R.layout.info_one_dialog, null)
            val positiveButton = customDialogIF.findViewById<Button>(R.id.positiveButton)
            val fileNameTextView = customDialogIF.findViewById<TextView>(R.id.fileName)
            val durationTextView = customDialogIF.findViewById<TextView>(R.id.DurationDetail)
            val sizeTextView = customDialogIF.findViewById<TextView>(R.id.sizeDetail)
            val locationTextView = customDialogIF.findViewById<TextView>(R.id.locationDetail)
//            val dateTextView = customDialogIF.findViewById<TextView>(R.id.dateDetail)

            fileNameTextView.text = video.title
            durationTextView.text = DateUtils.formatElapsedTime(video.duration / 1000)
            locationTextView.text = video.path

            // Ensure video.size is properly converted to a numeric type
            val sizeInBytes = video.size.toLongOrNull() ?: 0L

            // Format the size to display both in human-readable format and in bytes with commas
            val formattedSize = Formatter.formatShortFileSize(context, sizeInBytes)
            val bytesWithCommas = NumberFormat.getInstance().format(sizeInBytes)
            sizeTextView.text = "$formattedSize ($bytesWithCommas bytes)"

            val dialogIF = MaterialAlertDialogBuilder(context)
                .setView(customDialogIF)
                .setCancelable(false)
                .create()
            positiveButton.setOnClickListener {
                dialogIF.dismiss()
            }
            dialogIF.show()
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.show()
    }

    override fun getItemCount() = videoList.size
}
