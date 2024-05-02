package com.jaidev.seeaplayer.allAdapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.browseFregment.BrowseFragment
import com.jaidev.seeaplayer.browserActivity.LinkTubeActivity
import com.jaidev.seeaplayer.browserActivity.changeTab
import com.jaidev.seeaplayer.dataClass.SearchTitle
import com.jaidev.seeaplayer.dataClass.SearchTitleStore

class SavedTitlesAdapter(private val context: Context) : RecyclerView.Adapter<SavedTitlesAdapter.ViewHolder>() {

    private val titles: MutableList<String> = SearchTitleStore.getTitles(context).map { it.title }.toMutableList()

    fun addItem(title: String) {
        titles.add(0, title) // Add new item at the beginning of the list
        saveTitlesToStore()
        notifyItemInserted(0) // Notify RecyclerView that a new item has been added at position 0
    }

    fun removeItem(position: Int) {
        titles.removeAt(position)
        saveTitlesToStore()
        notifyItemRemoved(position)
    }

    private fun saveTitlesToStore() {
        val searchTitles = titles.map { SearchTitle(it) } // Assuming SearchTitle has a constructor that takes a title string
        SearchTitleStore.saveTitles(context, searchTitles)
    }

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
                val clickedPosition = adapterPosition
                moveItemToTop(clickedPosition)
                inputMethodManager.hideSoftInputFromWindow(itemView.windowToken, 0)

            }

            historyViewImageButton.setOnClickListener {
                val clickedTitle = titles[adapterPosition]
                fillTitleInTextUrl(clickedTitle)
            }
            // Long press listener
            itemView.setOnLongClickListener {
                showRemoveSuggestionDialog(title)
                true
            }

        }

        private fun showRemoveSuggestionDialog(title: String) {
            val alertDialogBuilder = AlertDialog.Builder(context)
            // Set the title of the dialog to the selected item's name
            alertDialogBuilder.setTitle(title)
            alertDialogBuilder.setMessage("Remove suggestion from the search history")

            // Set positive button (Remove)
            alertDialogBuilder.setPositiveButton("OK") { _, _ ->
                removeItem(adapterPosition)
            }

            // Set negative button (Cancel)
            alertDialogBuilder.setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }

            // Create and show the AlertDialog
            val alertDialog = alertDialogBuilder.create()
            alertDialog.show()
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

    @SuppressLint("NotifyDataSetChanged")
    private fun moveItemToTop(clickedPosition: Int) {
        if (clickedPosition != 0) {
            val clickedItem = titles.removeAt(clickedPosition)
            titles.add(0, clickedItem)
            saveTitlesToStore()
            notifyDataSetChanged()
        }
    }
}
