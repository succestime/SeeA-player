package com.jaidev.seeaplayer.allAdapters

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.jaidev.seeaplayer.FoldersActivity
import com.jaidev.seeaplayer.dataClass.Folder
import com.jaidev.seeaplayer.databinding.FoldersViewBinding

class FoldersAdapter(private val context: Context, private var foldersList: ArrayList<Folder>) : RecyclerView.Adapter<FoldersAdapter.MyHolder>() {

    class MyHolder(binding: FoldersViewBinding) : RecyclerView.ViewHolder(binding.root) {
        val folderName = binding.folderNameFV
        val totalVideoOfNumberContaining = binding.totalVideoOfNumberContaining

        val root = binding.root
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        return MyHolder(FoldersViewBinding.inflate(LayoutInflater.from(context), parent, false))
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: MyHolder, position: Int) {
        val folder = foldersList[position]

        // Capitalize the first letter of the folder name
        val folderName = if (folder.folderName.isNullOrEmpty()) {
            "Internal memory"
        } else {
            folder.folderName.capitalize()
        }

        holder.folderName.text = folderName
        // Update the text to show "1 video" or "X videos"
        holder.totalVideoOfNumberContaining.text = if (folder.videoCount == 1) {
            "${folder.videoCount} video"
        } else {
            "${folder.videoCount} videos"
        }

        holder.root.setOnClickListener {
            val intent = Intent(context, FoldersActivity::class.java)
            intent.putExtra("position", position)
            ContextCompat.startActivity(context, intent, null)
        }
    }

    override fun getItemCount(): Int {
        return foldersList.size
    }
}
