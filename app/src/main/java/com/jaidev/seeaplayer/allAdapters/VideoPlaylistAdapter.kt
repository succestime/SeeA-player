package com.jaidev.seeaplayer.allAdapters

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.jaidev.seeaplayer.PlaylistVideoActivity
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.dataClass.PlaylistVideo
import com.jaidev.seeaplayer.databinding.PlaylistVideoViewBinding

class PlaylistAdapter(private val context: Context,private val playlists: MutableList<PlaylistVideo>) :
    RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        val binding = PlaylistVideoViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PlaylistViewHolder(binding, context)
    }

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        holder.bind(playlists[position])
    }

    override fun getItemCount(): Int = playlists.size

    fun addPlaylist(playlist: PlaylistVideo) {
        playlists.add(playlist)
        notifyItemInserted(playlists.size - 1)
    }
    fun isEmpty(): Boolean = playlists.isEmpty()

    @SuppressLint("NotifyDataSetChanged")
    fun updatePlaylists(newPlaylists: List<PlaylistVideo>) {
        playlists.clear()
        playlists.addAll(newPlaylists)
        notifyDataSetChanged()
    }

    class PlaylistViewHolder(private val binding: PlaylistVideoViewBinding, private val context: Context) : RecyclerView.ViewHolder(binding.root) {
        fun bind(playlist: PlaylistVideo) {
            binding.playlistName.text = playlist.name
                binding.root.setOnClickListener {
                    val intent = Intent(context, PlaylistVideoActivity::class.java).apply {
                        putExtra("playlistId", playlist.id)  // Assuming Playlist has an id property
                    }
                context.startActivity(intent)
                (context as Activity).overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_right)
            }
            // Additional binding logic here
        }
    }
}
