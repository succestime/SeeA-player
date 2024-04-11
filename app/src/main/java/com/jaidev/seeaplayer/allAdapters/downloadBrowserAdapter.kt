package com.jaidev.seeaplayer.allAdapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.jaidev.seeaplayer.DownloadItemType
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.dataClass.DownloadItem

class DownloadBrowserAdapter(private val items: List<DownloadItem>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            DownloadItemType.IMAGE.ordinal -> {
                val view = inflater.inflate(R.layout.image_browser, parent, false)
                ImageViewHolder(view)
            }
            DownloadItemType.VIDEO.ordinal -> {
                val view = inflater.inflate(R.layout.video_download_browser, parent, false)
                VideoViewHolder(view)
            }
            DownloadItemType.PDF.ordinal,
            DownloadItemType.APK.ordinal -> {
                val view = inflater.inflate(R.layout.vpa_browser, parent, false)
                PdfApkViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        when (holder) {
            is ImageViewHolder -> {
                val imageItem = item as DownloadItem.ImageItem
                Glide.with(holder.itemView)
                    .asBitmap()
                    .apply(RequestOptions().placeholder(R.drawable.speaker).centerCrop())
                    .load(imageItem.imageUrl)
                    .into(holder.imageView)
            }
            is VideoViewHolder -> {
                val videoItem = item as DownloadItem.VideoItem
                holder.videoNameTextView.text = videoItem.videoName
                holder.durationTextView.text = videoItem.videoSize
            }
            is PdfApkViewHolder -> {
                when (item) {
                    is DownloadItem.PdfItem -> {
                        holder.titleTextView.text = item.pdfTitle
                        holder.titleDetailsView.text = item.details
                        holder.iconImageView.visibility = View.VISIBLE
                        holder.iconImageView.setImageResource(R.drawable.pdf_image)
                    }
                    is DownloadItem.ApkItem -> {
                        holder.titleTextView.text = item.apkName
                        holder.titleDetailsView.text = item.size
                        holder.iconImageView.visibility = View.VISIBLE
                        holder.iconImageView.setImageResource(R.drawable.video_browser)
                    }
                    else -> {
                        // Hide iconImageView if the item type is not PDF or APK
                        holder.iconImageView.visibility = View.GONE
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int = items.size

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is DownloadItem.ImageItem -> DownloadItemType.IMAGE.ordinal
            is DownloadItem.VideoItem -> DownloadItemType.VIDEO.ordinal
            is DownloadItem.PdfItem -> DownloadItemType.PDF.ordinal
            is DownloadItem.ApkItem -> DownloadItemType.APK.ordinal

        }
    }

    // ViewHolders
    class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
    }

    class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val videoNameTextView: TextView = itemView.findViewById(R.id.videoName)
        val durationTextView: TextView = itemView.findViewById(R.id.duration)
    }

    class PdfApkViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val iconImageView: ImageView = itemView.findViewById(R.id.iconImageView)
        val titleTextView: TextView = itemView.findViewById(R.id.titleTextView)
        val titleDetailsView: TextView = itemView.findViewById(R.id.titleDetailsView)
    }
}
