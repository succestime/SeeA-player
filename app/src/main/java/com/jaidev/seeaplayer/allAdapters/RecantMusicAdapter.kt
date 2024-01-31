package com.jaidev.seeaplayer.allAdapters

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.SpannableStringBuilder
import android.text.format.DateUtils
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.text.bold
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.dataClass.RecantMusic
import com.jaidev.seeaplayer.databinding.DetailsViewBinding
import com.jaidev.seeaplayer.databinding.RecantMusicViewBinding
import com.jaidev.seeaplayer.databinding.RecantVideoMoreFeaturesBinding
import com.jaidev.seeaplayer.recantFragment.ReMusicPlayerActivity

class RecantMusicAdapter (val  context : Context,  var musicReList : ArrayList<RecantMusic>, val isReMusic: Boolean = false,
                         ): RecyclerView.Adapter<RecantMusicAdapter.MyAdapter>() {

    private var newPosition = 0



    class MyAdapter(binding: RecantMusicViewBinding) : RecyclerView.ViewHolder(binding.root) {
        var title = binding.songName
        val image = binding.musicViewImage
        val album = binding.songAlbum
        val root = binding.root
        val more = binding.MoreChoose
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyAdapter {
        return MyAdapter(
            RecantMusicViewBinding.inflate(
                LayoutInflater.from(context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: MyAdapter, @SuppressLint("RecyclerView") position: Int) {
        holder.title.text = musicReList[position].title
        holder.album.text = musicReList[position].album
        Glide.with(context)
            .asBitmap()
            .load(musicReList[position].albumArtUri)
            .apply(RequestOptions().placeholder(R.drawable.speaker)).centerCrop()
            .into(holder.image)

        holder.more.setOnClickListener {
            newPosition = position
            val customDialog = LayoutInflater.from(context)
                .inflate(R.layout.recant_video_more_features, holder.root, false)
            val bindingMf = RecantVideoMoreFeaturesBinding.bind(customDialog)
            val dialog = MaterialAlertDialogBuilder(context).setView(customDialog)
                .create()
            dialog.show()


            bindingMf.shareBtn.setOnClickListener {

                dialog.dismiss()
                val shareIntent = Intent()
                shareIntent.action = Intent.ACTION_SEND
                shareIntent.type = "audio/*"
                shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(musicReList[position].path))
                ContextCompat.startActivity(
                    context,
                    Intent.createChooser(shareIntent, "Sharing Music File!!"),
                    null
                )


            }
            bindingMf.infoBtn.setOnClickListener {
                dialog.dismiss()
                val customDialogIf = LayoutInflater.from(context)
                    .inflate(R.layout.details_view, holder.root, false)
                val bindingIf = DetailsViewBinding.bind(customDialogIf)
                val dialogIf = MaterialAlertDialogBuilder(context).setView(customDialogIf)
                    .setCancelable(false)
                    .setPositiveButton("OK") { self, _ ->


                        self.dismiss()
                    }
                    .create()
                dialogIf.show()
                val infoText = SpannableStringBuilder().bold { append("DETAILS\n\nName : ") }
                    .append(musicReList[position].title)
                    .bold { append("\n\nDuration : ") }
                    .append(DateUtils.formatElapsedTime(musicReList[position].duration / 1000))
                    .bold { append("\n\nFile Size : ") }.append(
                        Formatter.formatShortFileSize(
                            context,
                            musicReList[position].size.toLong()
                        )
                    )
                    .bold { append("\n\nLocation : ") }.append(musicReList[position].path)
                bindingIf.detailTV.text = infoText
            }


        }


        holder.root.setOnClickListener {
            val intent = Intent(context, ReMusicPlayerActivity::class.java)
            intent.putExtra("index", position)
            intent.putExtra("class", "RecantMusicAdapter")
            ContextCompat.startActivity(context, intent, null)
        }

    }

    override fun getItemCount(): Int {
        return musicReList.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateRecentMusics(recantMusic: List<RecantMusic>) {
        musicReList.clear()
        musicReList.addAll(recantMusic)
        notifyDataSetChanged()
    }

}