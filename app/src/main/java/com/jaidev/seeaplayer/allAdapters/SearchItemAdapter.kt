package com.jaidev.seeaplayer.allAdapters

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.browserActivity.LinkTubeActivity
import com.jaidev.seeaplayer.dataClass.HistoryItem

class SearchItemAdapter(private val context: Context, private val historyItem: HistoryItem?) :
    RecyclerView.Adapter<SearchItemAdapter.SearchItemViewHolder>() {

    inner class SearchItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.historyTitle)
        private val urlTextView: TextView = itemView.findViewById(R.id.webHistoryTitle)
        private val imageView: ImageView = itemView.findViewById(R.id.historyView)
        private val writeTextView: ImageButton = itemView.findViewById(R.id.textFillArrow)
        private val copyTextView: ImageButton = itemView.findViewById(R.id.copyFillArrow)
        private val shareTextView: ImageButton = itemView.findViewById(R.id.shareFillArrow)

        fun bind(historyItem: HistoryItem) {
            val topicName = extractTopicName(historyItem.url)
            titleTextView.text = topicName
            urlTextView.text = historyItem.title
            if (historyItem.imageBitmap != null) {
                imageView.setImageBitmap(historyItem.imageBitmap)
            } else {
                imageView.setImageResource(R.drawable.search_link_tube)
            }

            writeTextView.setOnClickListener {
                // Clear the text in btnTextUrl
                // Write the URL in btnTextUrl
                val title = historyItem.url
                fillTitleInTextUrl(title)
            }
            copyTextView.setOnClickListener {
                val url = historyItem.url
                copyToClipboard(url)
                showToast("Copied")
            }
            shareTextView.setOnClickListener {
                val url = historyItem.url
                shareUrl(url)
            }
        }

        private fun extractTopicName(url: String): String {
            // Decode the URL to handle special characters properly
            val decodedUrl = java.net.URLDecoder.decode(url, "UTF-8")

            // Find the index of "?q=" in the decoded URL
            val index = decodedUrl.indexOf("?q=")
            return if (index != -1) {
                // Extract the substring after "?q="
                val queryString = decodedUrl.substring(index + 3)

                // Remove any additional parameters like "#pi=1" or "#sbfbu=1"
                val topicName = queryString.split("&")[0].split("#")[0]

                // Return the topic name
                topicName
            } else {
                // Return the entire URL if "?q=" is not found
                decodedUrl
            }
        }

        private fun copyToClipboard(text: String) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("URL", text)
            clipboard.setPrimaryClip(clip)
        }

        private fun showToast(message: String) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }

        private fun shareUrl(url: String) {
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "text/plain"
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Sharing URL")
            shareIntent.putExtra(Intent.EXTRA_TEXT, url)
            context.startActivity(Intent.createChooser(shareIntent, "Share via"))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchItemViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.history_view_hint, parent, false)
        return SearchItemViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: SearchItemViewHolder, position: Int) {
        historyItem?.let { holder.bind(it) }
    }

    override fun getItemCount() = if (historyItem != null) 1 else 0

    private fun fillTitleInTextUrl(title: String) {
        val linkTubeRef = context as LinkTubeActivity
        val editText = linkTubeRef.binding.btnTextUrl
        editText.setText(title)
        editText.setSelection(editText.text.length)



    }
}


