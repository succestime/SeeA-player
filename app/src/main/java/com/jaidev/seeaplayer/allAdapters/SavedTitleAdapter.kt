package com.jaidev.seeaplayer.allAdapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.dataClass.SearchTitleStore

class SavedTitlesAdapter(private val context: Context) : RecyclerView.Adapter<SavedTitlesAdapter.ViewHolder>() {

    private val titles: List<String> = SearchTitleStore.getTitles(context).map { it.title }.take(10).reversed()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.history_view_browser, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val title = titles[position]
        holder.bind(title)
    }

    override fun getItemCount(): Int {
        return titles.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.historyTitle)

        fun bind(title: String) {
            titleTextView.text = title
        }
    }
}
