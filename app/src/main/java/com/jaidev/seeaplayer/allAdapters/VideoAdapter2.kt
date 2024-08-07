package com.jaidev.seeaplayer.allAdapters

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

class VideoAdapter2(private val context: Context, private val videoList: ArrayList<VideoData>) :
    RecyclerView.Adapter<VideoAdapter2.MyHolder>() {
    private val selectedItems = mutableSetOf<Int>()
    private var selectionChangeListener: OnSelectionChangeListener? = null

    fun getSelectedVideos(): List<VideoData> {
        return selectedItems.map { videoList[it] }
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
    override fun onBindViewHolder(holder: MyHolder, position: Int) {
        val video = videoList[position]

        holder.title.text = videoList[position].title
        holder.duration.text = DateUtils.formatElapsedTime(videoList[position].duration / 1000)
        Glide.with(context)
            .load(videoList[position].artUri)
            .apply(RequestOptions().placeholder(R.color.place_holder_video).centerCrop())
            .into(holder.image)


        if (selectedItems.contains(position)) {
            holder.emptyCheck.visibility = View.GONE
            holder.fillCheck.visibility = View.VISIBLE

        } else {
            holder.emptyCheck.visibility = View.VISIBLE
            holder.fillCheck.visibility = View.GONE
        }
        holder.root.setOnClickListener {
            if (selectedItems.contains(position)) {
                selectedItems.remove(position)
                notifyItemChanged(position)
            } else {
                selectedItems.add(position)
                notifyItemChanged(position)
            }
            // Notify listener about the change
            selectionChangeListener?.onSelectionChanged(selectedItems.size)
        }
    }

    override fun getItemCount(): Int {
        return videoList.size
    }

    fun getSelectedItemCount(): Int {
        return selectedItems.size
    }

    // Method to set the listener
    fun setOnSelectionChangeListener(listener: OnSelectionChangeListener) {
        this.selectionChangeListener = listener
    }

    interface OnSelectionChangeListener {
        fun onSelectionChanged(count: Int)
    }

}
