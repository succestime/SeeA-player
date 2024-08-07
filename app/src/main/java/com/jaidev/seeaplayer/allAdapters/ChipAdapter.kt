package com.jaidev.seeaplayer.allAdapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.dataClass.ChipItem

class ChipAdapter(private val context: Context, private val chipList: List<ChipItem>) : RecyclerView.Adapter<ChipAdapter.ChipViewHolder>() {
    private var mListener: OnItemClickListener? = null

    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        mListener = listener
    }
    inner class ChipViewHolder(itemView: View, listener: OnItemClickListener?) :
        RecyclerView.ViewHolder(itemView), View.OnClickListener {
        val chipTitle: TextView = itemView.findViewById(R.id.chipTitle)
        val chipIcon: ImageView = itemView.findViewById(R.id.chipIcon)

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
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChipViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.chip_view, parent, false)
        return ChipViewHolder(view, mListener)
    }


    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ChipViewHolder, position: Int) {
        val iconModel = chipList[position]
        holder.chipIcon.setImageResource(iconModel.iconResId)
        holder.chipTitle.text = iconModel.title


    }

    override fun getItemCount(): Int {
        return chipList.size
    }
}
