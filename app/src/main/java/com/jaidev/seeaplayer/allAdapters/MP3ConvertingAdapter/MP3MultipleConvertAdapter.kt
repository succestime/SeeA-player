package com.jaidev.seeaplayer.allAdapters.MP3ConvertingAdapter

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
import com.jaidev.seeaplayer.dataClass.VideoData
import com.jaidev.seeaplayer.databinding.PlaylistAddVideoViewBinding

class MP3MultipleConvertAdapter(
    private val context: Context,
    private val originalVideoList: ArrayList<VideoData>
) : RecyclerView.Adapter<MP3MultipleConvertAdapter.MyHolder>() {

    private var filteredVideoList: ArrayList<VideoData> = ArrayList(originalVideoList) // Use this for filtering
    private var selectedItem: Int? = null
    private var selectionChangeListener: OnSelectionChangeListener? = null

    fun getSelectedVideos(): List<VideoData> {
        return selectedItem?.let { listOf(filteredVideoList[it]) } ?: emptyList()
    }

    fun getSelectedVideo(): ArrayList<VideoData> {
        return selectedItem?.let {
            listOf(filteredVideoList[it]).toCollection(ArrayList())
        } ?: ArrayList()
    }


    class MyHolder(binding: PlaylistAddVideoViewBinding) : RecyclerView.ViewHolder(binding.root) {
        val title = binding.videoName
        val duration = binding.duration
        val image = binding.videoImage
        val emptyCheck = binding.emptyCheck
        val fillCheck = binding.fillCheck
        val root = binding.root
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        return MyHolder(
            PlaylistAddVideoViewBinding.inflate(
                LayoutInflater.from(context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: MyHolder, @SuppressLint("RecyclerView") position: Int) {
        val video = filteredVideoList[position]

        holder.title.text = video.title
        holder.duration.text = DateUtils.formatElapsedTime(video.duration / 1000)
        Glide.with(context)
            .load(video.artUri)
            .apply(RequestOptions().placeholder(R.color.place_holder_video).centerCrop())
            .into(holder.image)

        if (selectedItem == position) {
            holder.emptyCheck.visibility = View.GONE
            holder.fillCheck.visibility = View.VISIBLE
        } else {
            holder.emptyCheck.visibility = View.VISIBLE
            holder.fillCheck.visibility = View.GONE
        }

        holder.root.setOnClickListener {
            if (!holder.root.isEnabled) return@setOnClickListener

            val previousSelectedItem = selectedItem
            selectedItem = position

            // Notify the previous selected item to update its state
            previousSelectedItem?.let { notifyItemChanged(it) }
            // Notify the newly selected item to update its state
            notifyItemChanged(position)

            selectionChangeListener?.onSelectionChanged(1)
        }
    }

    override fun getItemCount(): Int {
        return filteredVideoList.size
    }

    // Method to set the listener
    fun setOnSelectionChangeListener(listener: OnSelectionChangeListener) {
        this.selectionChangeListener = listener
    }

    // Method to filter the list
    @SuppressLint("NotifyDataSetChanged")
    fun filter(query: String) {
        val escapedQuery = Regex.escape(query.trim())

        filteredVideoList = if (escapedQuery.isEmpty()) {
            ArrayList(originalVideoList)
        } else {
            val filteredList = originalVideoList.filter { it.title.contains(escapedQuery.toRegex(RegexOption.IGNORE_CASE)) }
            ArrayList(filteredList)
        }

        selectedItem = null // Clear the selection after filtering
        notifyDataSetChanged()
        selectionChangeListener?.onSelectionChanged(0)
    }

    interface OnSelectionChangeListener {
        fun onSelectionChanged(count: Int)
    }
}
