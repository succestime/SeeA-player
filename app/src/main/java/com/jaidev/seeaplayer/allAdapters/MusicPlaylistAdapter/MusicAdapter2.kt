package com.jaidev.seeaplayer.allAdapters.MusicPlaylistAdapter

import android.annotation.SuppressLint
import android.content.Context
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.dataClass.Music
import com.jaidev.seeaplayer.dataClass.getImgArt
import com.jaidev.seeaplayer.databinding.PlaylistAddMusicViewBinding

class MusicAdapter2(
    private val context: Context,
    private val originalVideoList: ArrayList<Music>, // Keep the original list
    private val videosInPlaylist: Set<String>
) : RecyclerView.Adapter<MusicAdapter2.MyHolder>() {

    private var filteredVideoList: ArrayList<Music> = ArrayList(originalVideoList) // Use this for filtering
    private val selectedItems = mutableSetOf<Int>()
    private var selectionChangeListener: OnSelectionChangeListener? = null

    fun getSelectedVideos(): List<Music> {
        return selectedItems.map { filteredVideoList[it] }
    }

    class MyHolder(binding: PlaylistAddMusicViewBinding) : RecyclerView.ViewHolder(binding.root) {
        val title = binding.videoName
        val duration = binding.duration
        val image = binding.videoImage
        val emptyCheck = binding.emptyCheck
        val fillCheck = binding.fillCheck
        val root = binding.root
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        return MyHolder(
            PlaylistAddMusicViewBinding.inflate(
                LayoutInflater.from(context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: MyHolder, position: Int) {
        val video = filteredVideoList[position]

        holder.title.text = video.title
        holder.duration.text = DateUtils.formatElapsedTime(video.duration / 1000)
        Glide.with(context)
            .load(getImgArt(video.path))
            .apply(
                RequestOptions()
                    .placeholder(R.color.gray) // Use the newly created drawable
                    .error(R.drawable.music_note_svgrepo_com) // Use the newly created drawable
                    .centerCrop()
            )
            .into(holder.image)
        // Check if the video is already in the playlist
        if (videosInPlaylist.contains(video.musicid)) {
            holder.emptyCheck.visibility = View.GONE
            holder.fillCheck.visibility = View.VISIBLE
            holder.root.isEnabled = false
            holder.root.alpha = 0.5f
        } else {
            holder.emptyCheck.visibility = if (selectedItems.contains(position)) View.GONE else View.VISIBLE
            holder.fillCheck.visibility = if (selectedItems.contains(position)) View.VISIBLE else View.GONE
            holder.root.isEnabled = true
            holder.root.alpha = 1.0f
        }

        holder.root.setOnClickListener {
            if (!holder.root.isEnabled) return@setOnClickListener
            if (selectedItems.contains(position)) {
                selectedItems.remove(position)
            } else {
                selectedItems.add(position)
            }
            notifyItemChanged(position)
            selectionChangeListener?.onSelectionChanged(selectedItems.size)
        }
    }

    override fun getItemCount(): Int {
        return filteredVideoList.size
    }

    fun getSelectedItemCount(): Int {
        return selectedItems.size
    }

    // Method to set the listener
    fun setOnSelectionChangeListener(listener: OnSelectionChangeListener) {
        this.selectionChangeListener = listener
    }

    // Method to filter the list
    @SuppressLint("NotifyDataSetChanged")
    fun filter(query: String) {
        // Escape special characters to prevent issues in the filter
        val escapedQuery = Regex.escape(query.trim())

        // Perform the filtering
        filteredVideoList = if (escapedQuery.isEmpty()) {
            ArrayList(originalVideoList)
        } else {
            val filteredList = originalVideoList.filter { it.title.contains(escapedQuery.toRegex(RegexOption.IGNORE_CASE)) }
            ArrayList(filteredList)
        }

        // Clear selections and update the view
        selectedItems.clear()
        notifyDataSetChanged()
        selectionChangeListener?.onSelectionChanged(selectedItems.size)
    }


    interface OnSelectionChangeListener {
        fun onSelectionChanged(count: Int)
    }
}
