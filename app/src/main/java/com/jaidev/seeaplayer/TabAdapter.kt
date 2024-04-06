
package com.jaidev.seeaplayer

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.jaidev.seeaplayer.databinding.TabBinding

class TabAdapter(private val context: Context, private val dialog: AlertDialog
): RecyclerView.Adapter<TabAdapter.MyHolder>() {


    class MyHolder(binding: TabBinding)
        :RecyclerView.ViewHolder(binding.root) {
        val cancelBtn = binding.cancelBtn
        val name = binding.tabName
        val root = binding.root
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {

        return MyHolder(TabBinding.inflate(LayoutInflater.from(context), parent, false))
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onBindViewHolder(holder: MyHolder, position: Int) {

        holder.name.text = LinkTubeActivity.tabsList[position].name
        holder.root.setOnClickListener {
            LinkTubeActivity.myPager.currentItem = position
            dialog.dismiss()
        }

        holder.cancelBtn.setOnClickListener{
            if(LinkTubeActivity.tabsList.size == 1 || position == LinkTubeActivity.myPager.currentItem)
                Toast.makeText(context, "Can't Remove this tab", Toast.LENGTH_SHORT).show()
            else{
                LinkTubeActivity.tabsList.removeAt(position)
                notifyDataSetChanged()
                LinkTubeActivity.myPager.adapter?.notifyItemRemoved(position)
            }

        }
    }

    override fun getItemCount(): Int {
        return LinkTubeActivity.tabsList.size
    }
}
