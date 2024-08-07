package com.jaidev.seeaplayer.allAdapters



import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jaidev.seeaplayer.R

class VideoOfPlaylistAdapter : RecyclerView.Adapter<VideoOfPlaylistAdapter.VideoViewHolder>() {

    private var videoList: List<String> = emptyList() // Replace String with your actual data model

    @SuppressLint("NotifyDataSetChanged")
    fun setVideos(videos: List<String>) {
        videoList = videos
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_video, parent, false)
        return VideoViewHolder(view)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        holder.bind(videoList[position])
    }

    override fun getItemCount(): Int {
        return videoList.size
    }

    class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val videoTitle: TextView = itemView.findViewById(R.id.videoTitle) // Adjust this based on your item layout

        fun bind(video: String) {
            videoTitle.text = video
        }
    }
}
