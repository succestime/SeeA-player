package com.jaidev.seeaplayer.allAdapters

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.dataClass.Playlist
import com.jaidev.seeaplayer.databinding.PlaylistMusicViewBinding
import com.jaidev.seeaplayer.musicActivity.PlaylistActivity
import com.jaidev.seeaplayer.musicActivity.PlaylistDetails

class PlaylistViewAdapter(private val context: Context, private var playlistList : ArrayList<Playlist> ): RecyclerView.Adapter<PlaylistViewAdapter.MyAdapter>() {

    class MyAdapter(binding: PlaylistMusicViewBinding) : RecyclerView.ViewHolder(binding.root) {
        val image = binding.playlistImage
        val name = binding.playlistName
        val root = binding.root
        val more = binding.playlistDelete
//        val totalMusic = binding.totalVideosDuration

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyAdapter {
        return MyAdapter(PlaylistMusicViewBinding.inflate(LayoutInflater.from(context),parent,false))
    }

    override fun onBindViewHolder(holder: MyAdapter, position: Int) {
        holder.name.text = playlistList[position].name
        holder.name.isSelected = true
        holder.more.setOnClickListener {
            val builder = MaterialAlertDialogBuilder(context)
            builder.setTitle(playlistList[position].name)
                .setMessage("Do you want to delete playlist ?")
                .setPositiveButton("Yes") { dialog, _ ->
                    PlaylistActivity.musicPlaylist.ref.removeAt(position)
                    refreshPlaylist()
                 dialog.dismiss()
                }
                .setNegativeButton("No") { dialog, _ ->
                    dialog.dismiss()
                }
            val customDialog = builder.create()
            customDialog.show()
        }
        holder.root.setOnClickListener {

            val intent = Intent(context , PlaylistDetails::class.java)
            intent.putExtra("index" , position)
            ContextCompat.startActivity(context , intent, null)
        }
        if (PlaylistActivity.musicPlaylist.ref[position].playlist.size > 0){
            Glide.with(context)
                .asBitmap()
                .load(PlaylistActivity.musicPlaylist.ref[position].playlist[0].artUri)
                .apply(RequestOptions().placeholder(R.drawable.speaker).centerCrop())
                .into(holder.image)
        }

    }

    override fun getItemCount(): Int {
        return playlistList.size
    }

    // for refreshing the Playlist when any new playlist add
    @SuppressLint("NotifyDataSetChanged")
    fun refreshPlaylist(){
     playlistList = ArrayList()
     playlistList.addAll(PlaylistActivity.musicPlaylist.ref)
     notifyDataSetChanged()
 }
}