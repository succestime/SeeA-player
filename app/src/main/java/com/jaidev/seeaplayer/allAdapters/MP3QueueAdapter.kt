package com.jaidev.seeaplayer.allAdapters

import android.annotation.SuppressLint
import android.content.Context
import android.text.format.DateUtils
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.dataClass.MP3FileData
import com.jaidev.seeaplayer.databinding.Mp3QueueViewBinding

class MP3QueueAdapter(private val context: Context, private val mp3Files: ArrayList<MP3FileData>,
                      private var currentSongPosition: Int,
                      private val onCenterCurrentItem: (Int) -> Unit ,
                      private val onSongClick: (Int) -> Unit
) :
    RecyclerView.Adapter<MP3QueueAdapter.MP3ViewHolder>()
//    , ItemTouchHelperAdapter
{
//
//
//
//    override fun onItemMove(fromPosition: Int, toPosition: Int) {
//        Collections.swap(mp3Files, fromPosition, toPosition)
//        notifyItemMoved(fromPosition, toPosition)
//
//    }
//    override fun onItemDismiss(position: Int) {
//        mp3Files.removeAt(position)
//        notifyItemRemoved(position)
//
//    }
    class MP3ViewHolder(binding: Mp3QueueViewBinding) : RecyclerView.ViewHolder(binding.root) {
        val title = binding.videoName
        val duration = binding.duration
        val root = binding.root
        val mp3Image = binding.mp3Image

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MP3ViewHolder {
        return MP3ViewHolder(
            Mp3QueueViewBinding.inflate(
                LayoutInflater.from(context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: MP3ViewHolder, position: Int) {
        val mp3Audio = mp3Files[position]
        holder.title.text = mp3Audio.title
        holder.duration.text = DateUtils.formatElapsedTime(mp3Audio.duration / 1000)

        Glide.with(context)
            .load(mp3Audio.artUri)
            .apply(RequestOptions().placeholder(R.color.place_holder_video).centerCrop())
            .into(holder.mp3Image)

        // Highlight the currently playing song
        if (position == currentSongPosition) {
            // Add elevation or change the background
            holder.root.elevation = 8f // Increase elevation
            holder.root.setBackgroundResource(R.drawable.button_background_cool_blue) // Custom background
            // Set the title color to cool_blue
            holder.title.setTextColor(ContextCompat.getColor(context, R.color.cool_blue))

            // Center the currently playing song
            onCenterCurrentItem(position)
        } else {
            // Get the default text color from the theme (android:attr/textColorPrimary)
            val typedValue = TypedValue()
            val theme = context.theme
            theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)
            val defaultTextColor = ContextCompat.getColor(context, typedValue.resourceId)  // Get the default text color from the theme (android:attr/textColorPrimary)


            holder.root.elevation = 0f // Remove elevation
            holder.root.setBackgroundResource(R.drawable.button_background_cool_window_background) // Default background

            // Clear the color filter to revert to the default text color
            holder.title.setTextColor(defaultTextColor)
        }

        // Set the click listener on the root view
        holder.root.setOnClickListener {
            onSongClick(position) // Notify the activity about the click
        }

    }

    override fun getItemCount(): Int {
        return mp3Files.size
    }

    // Optional: Update the current song position
    @SuppressLint("NotifyDataSetChanged")
    fun updateCurrentSongPosition(newPosition: Int) {
        val previousPosition = currentSongPosition
        currentSongPosition = newPosition
        notifyItemChanged(previousPosition)
        notifyItemChanged(newPosition)
        notifyDataSetChanged()
    }


}
