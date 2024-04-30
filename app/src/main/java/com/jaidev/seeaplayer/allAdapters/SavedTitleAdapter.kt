package com.jaidev.seeaplayer.allAdapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.browseFregment.BrowseFragment
import com.jaidev.seeaplayer.browserActivity.LinkTubeActivity
import com.jaidev.seeaplayer.browserActivity.changeTab
import com.jaidev.seeaplayer.dataClass.SearchTitleStore

class SavedTitlesAdapter(private val context: Context) : RecyclerView.Adapter<SavedTitlesAdapter.ViewHolder>() {

    private val titles: List<String> = SearchTitleStore.getTitles(context).map { it.title }.take(10).reversed()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.history_view_browser, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val title = titles[position]
        holder.bind(title = title)
    }

    override fun getItemCount(): Int {
        return titles.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.historyTitle)
        private val historyViewImageButton: ImageButton = itemView.findViewById(R.id.textFillArrow)

        fun bind(title: String) {
            titleTextView.text = title

            // Access the InputMethodManager from the context
            val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

            itemView.setOnClickListener {
                val query = titles[adapterPosition] // Get the query from titles list based on adapter position
                navigateToBrowserFragment(query)

                // Hide the soft keyboard
                inputMethodManager.hideSoftInputFromWindow(itemView.windowToken, 0)
            }

            historyViewImageButton.setOnClickListener {
                val clickedTitle = titles[adapterPosition]
                fillTitleInTextUrl(clickedTitle)
            }
        }

    }

    private fun navigateToBrowserFragment(query: String) {
        val browserFragment = BrowseFragment(urlNew = query)
        changeTab("Brave", browserFragment)
    }
    private fun fillTitleInTextUrl(title: String) {
        val linkTubeRef = context as LinkTubeActivity
        linkTubeRef.binding.btnTextUrl.setText(title)
    }
}
