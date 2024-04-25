package com.jaidev.seeaplayer.allAdapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.jaidev.seeaplayer.R


data class Model(val imageResId : Int )
class MyDotAdapter(private val items : List<Model>): RecyclerView.Adapter<MyDotAdapter.PagerViewHolder>() {

    inner class PagerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PagerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.seea_item_page,parent,false)
        return PagerViewHolder(view)
    }

    override fun onBindViewHolder(holder: PagerViewHolder, position: Int) {
        val item = items[position]

        val imageview = holder.itemView.findViewById<ImageView>(R.id.imageView)

        imageview.setImageResource(item.imageResId)
    }

    override fun getItemCount(): Int {
        return items.size   }
}
