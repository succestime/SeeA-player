
package com.jaidev.seeaplayer

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.text.SpannableStringBuilder
import android.text.format.DateUtils
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.text.bold
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jaidev.seeaplayer.dataClass.RecantVideo
import com.jaidev.seeaplayer.databinding.DetailsViewBinding
import com.jaidev.seeaplayer.databinding.RecantDownloadViewBinding
import com.jaidev.seeaplayer.databinding.RecantVideoMoreFeaturesBinding
import com.jaidev.seeaplayer.recantFragment.ReVideoPlayerActivity

class RecentVideoAdapter(private val context: Context, private var videoReList: ArrayList<RecantVideo> ,private val isRecantVideo: Boolean = false ) :
    RecyclerView.Adapter<RecentVideoAdapter.MyAdapter>() {

    private  var newPosition = 0


    class MyAdapter(binding: RecantDownloadViewBinding) : RecyclerView.ViewHolder(binding.root) {
        val title = binding.videoName
        val duration = binding.duration
        val image = binding.videoImage
        val more = binding.MoreChoose
        val root = binding.root

    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyAdapter {
        val binding = RecantDownloadViewBinding.inflate(
            LayoutInflater.from(context),
            parent,
            false
        )
        return MyAdapter(binding)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onBindViewHolder(holder: MyAdapter, @SuppressLint("RecyclerView") position: Int) {
        holder.title.text = videoReList[position].title
        holder.duration.text = DateUtils.formatElapsedTime(videoReList[position].duration / 1000)
        Glide.with(context)
            .asBitmap()
            .load(videoReList[position].artUri)
            .apply(RequestOptions().placeholder(R.mipmap.ic_logo_o).centerCrop())
            .into(holder.image)


        holder.root.setOnClickListener {
            when{
                isRecantVideo -> {
                    ReVideoPlayerActivity.pipStatus = 1
                    sendIntent(pos = position, ref = "RecantVideo")
                }
            }
        }
        holder.more.setOnClickListener {
            newPosition = position

            val customDialog = LayoutInflater.from(context)
                .inflate(R.layout.recant_video_more_features, holder.root, false)
            val bindingMf = RecantVideoMoreFeaturesBinding.bind(customDialog)
            val dialog = MaterialAlertDialogBuilder(context).setView(customDialog)
                .create()
            dialog.show()


//            bindingMf.renameBtn.setOnClickListener {
//                dialog.show()
//                 removeClickListener?.onRemoveClicked(position)
//
//            }



            bindingMf.shareBtn.setOnClickListener {
                dialog.dismiss()
                val shareIntent = Intent()
                shareIntent.action = Intent.ACTION_SEND
                shareIntent.type = "video/*"
                shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(videoReList[position].path))
                ContextCompat.startActivity(
                    context,
                    Intent.createChooser(shareIntent, "Sharing Video"),
                    null
                )
            }

            bindingMf.infoBtn.setOnClickListener {
                dialog.dismiss()
                val customDialogIF =
                    LayoutInflater.from(context).inflate(R.layout.details_view, holder.root, false)
                val bindingIF = DetailsViewBinding.bind(customDialogIF)
                val dialogIF = MaterialAlertDialogBuilder(context).setView(customDialogIF)
                    .setCancelable(false)
                    .setPositiveButton("OK") { self, _ ->
                        self.dismiss()
                    }
                    .create()
                dialogIF.show()
                val infoText = SpannableStringBuilder().bold { append("DETAILS\n\nName : ") }
                    .append(videoReList[position].title)
                    .bold { append("\n\nDuration : ") }
                    .append(DateUtils.formatElapsedTime(videoReList[position].duration / 1000))
                    .bold { append("\n\nFile Size : ") }.append(
                        Formatter.formatShortFileSize(
                            context,
                            videoReList[position].size.toLong()
                        )
                    )
                    .bold { append("\n\nLocation : ") }.append(videoReList[position].path)
                bindingIF.detailTV.text = infoText


            }

        }

    }

    override fun getItemCount(): Int {
        return videoReList.size
    }


    @SuppressLint("NotifyDataSetChanged")
    fun updateRecentVideos(recentVideos: List<RecantVideo>) {
        videoReList.clear()
        videoReList.addAll(recentVideos)
        notifyDataSetChanged()
    }


    private fun sendIntent(pos: Int, ref: String) {
        ReVideoPlayerActivity.position = pos
        val intent = Intent(context, ReVideoPlayerActivity::class.java)
        intent.putExtra("class", ref)
        ContextCompat.startActivity(context, intent, null)

    }


}
