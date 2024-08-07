package com.jaidev.seeaplayer.allAdapters

import android.annotation.SuppressLint
import android.content.Context
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.imageview.ShapeableImageView
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.dataClass.VideoData

class PlaylistVideoShowAdapter(private val context: Context, private var videoList: List<VideoData>) :
    RecyclerView.Adapter<PlaylistVideoShowAdapter.VideoViewHolder>() {

    @SuppressLint("NotifyDataSetChanged")
    fun updateVideoList(newList: List<VideoData>) {
        videoList = newList
        notifyDataSetChanged()
    }
    inner class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.videoName)
        val durationTextView: TextView = itemView.findViewById(R.id.duration)
        val videoImage: ShapeableImageView = itemView.findViewById(R.id.videoImage)
        // Add other views if needed
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.recant_download_view, parent, false)
        return VideoViewHolder(view)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val video = videoList[position]
        holder.titleTextView.text = video.title
        holder.durationTextView.text = DateUtils.formatElapsedTime(videoList[position].duration / 1000)
        Glide.with(context)
            .load(videoList[position].artUri)
            .apply(RequestOptions().placeholder(R.color.place_holder_video).centerCrop())
            .into(holder.videoImage)
    }

    override fun getItemCount() = videoList.size
}
