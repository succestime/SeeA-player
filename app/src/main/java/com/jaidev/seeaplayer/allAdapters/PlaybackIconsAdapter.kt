package com.jaidev.seeaplayer.allAdapters

import android.content.Context
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.dataClass.IconModel

class PlaybackIconsAdapter(private val iconModelsList: ArrayList<IconModel>, private val context: Context) :
    RecyclerView.Adapter<PlaybackIconsAdapter.ViewHolder>() {

    private var mListener: OnItemClickListener? = null

    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        mListener = listener
    }

    inner class ViewHolder(itemView: View, listener: OnItemClickListener?) :
        RecyclerView.ViewHolder(itemView), View.OnClickListener {
        val iconName: TextView = itemView.findViewById(R.id.icon_title)
        val icon: ImageView = itemView.findViewById(R.id.playback_icon)

        init {
            itemView.setOnClickListener(this)
            mListener = listener
        }

        override fun onClick(v: View) {
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                mListener?.onItemClick(position)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.icons_layout, parent, false)
        return ViewHolder(view, mListener)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val iconModel = iconModelsList[position]
        holder.icon.setImageResource(iconModel.imageView)
        holder.icon.setColorFilter(ContextCompat.getColor(context, iconModel.iconTint), PorterDuff.Mode.SRC_IN)
        holder.iconName.text = iconModel.iconTitle
    }


    override fun getItemCount(): Int {
        return iconModelsList.size
    }
}