
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
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.dataClass.Music
import com.jaidev.seeaplayer.databinding.FavouriteViewBinding
import com.jaidev.seeaplayer.musicActivity.FavouriteActivity.Companion.favouriteSongs
import com.jaidev.seeaplayer.musicActivity.PlayerMusicActivity

class FavouriteAdapter(private val context: Context, private var musicList : ArrayList<Music>): RecyclerView.Adapter<FavouriteAdapter.MyAdapter>() {

    class MyAdapter(binding: FavouriteViewBinding) : RecyclerView.ViewHolder(binding.root) {
        val image = binding.songImgFA
        val name = binding.songNameFA
        val root = binding.root
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyAdapter {
        return MyAdapter(FavouriteViewBinding.inflate(LayoutInflater.from(context),parent,false))
    }

    override fun onBindViewHolder(holder: MyAdapter, position: Int) {


        holder.name.text = musicList[position].title
        Glide.with(context)
            .load(musicList[position].artUri)
            .apply(RequestOptions()
                .placeholder(R.color.gray) // Use the newly created drawable
                .error(R.drawable.music_note_svgrepo_com)
                .centerCrop())
            . into(holder.image)

        holder.root.setOnClickListener {
            val intent = Intent(context , PlayerMusicActivity::class.java)
            intent.putExtra("index" , position)
            intent.putExtra("class" , "FavouriteAdapter")
            ContextCompat.startActivity(context , intent , null)
        }
    }

    override fun getItemCount(): Int {
        return musicList.size
    }
    @SuppressLint("NotifyDataSetChanged")
    fun updateFavourites(newFavourites: List<Music>) {
        favouriteSongs.clear()
        favouriteSongs.addAll(newFavourites)
        notifyDataSetChanged()
    }

}
