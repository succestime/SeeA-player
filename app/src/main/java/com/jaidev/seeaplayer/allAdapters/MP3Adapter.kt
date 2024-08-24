package com.jaidev.seeaplayer.allAdapters

import android.content.Context
import android.content.Intent
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jaidev.seeaplayer.MP3playerActivity
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.dataClass.MP3FileData

class MP3Adapter(private val context: Context, private val mp3Files: ArrayList<MP3FileData>) :
    RecyclerView.Adapter<MP3Adapter.MP3ViewHolder>() {

    inner class MP3ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.videoName)
        val duration: TextView = itemView.findViewById(R.id.duration)

        fun bind(mp3FileData: MP3FileData) {
            title.text = mp3FileData.title
            duration.text = DateUtils.formatElapsedTime(mp3FileData.duration / 1000)
            itemView.setOnClickListener {
                val intent = Intent(context, MP3playerActivity::class.java).apply {
                    putExtra("mp3FilePath", mp3FileData.path)
                    putExtra("mp3FileTitle", mp3FileData.title)
                    putExtra("mp3Files", mp3Files)
                    putExtra("currentIndex", position)
                }
                context.startActivity(intent)
            }


        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MP3ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.recant_download_view, parent, false)
        return MP3ViewHolder(view)
    }

    override fun onBindViewHolder(holder: MP3ViewHolder, position: Int) {
        holder.bind(mp3Files[position])
    }

    override fun getItemCount(): Int {
        return mp3Files.size
    }




}
