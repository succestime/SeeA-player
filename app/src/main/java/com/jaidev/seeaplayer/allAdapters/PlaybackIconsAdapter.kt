package com.jaidev.seeaplayer.allAdapters

import android.annotation.SuppressLint
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

class PlaybackIconsAdapter(private val iconModelsList: ArrayList<IconModel>, private val context: Context ,     private var dark: Boolean
) :
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
        val agdfdgdfgfrt: ImageView = itemView.findViewById(R.id.agdfdgdfgfrt)

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

    @SuppressLint("ResourceType")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val iconModel = iconModelsList[position]
        holder.agdfdgdfgfrt.setImageResource(iconModel.imageView)
        holder.agdfdgdfgfrt.setColorFilter(ContextCompat.getColor(context, iconModel.iconTint), PorterDuff.Mode.SRC_IN)
        holder.iconName.text = iconModel.iconTitle
        holder.icon.setBackgroundResource(iconModel.iconBackground)

    }


    override fun getItemCount(): Int {
        return iconModelsList.size
    }
}