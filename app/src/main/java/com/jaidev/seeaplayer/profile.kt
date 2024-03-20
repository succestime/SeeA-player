package com.jaidev.seeaplayer

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jaidev.seeaplayer.databinding.FragmentProfileBinding

class profile : Fragment() {
    private lateinit var binding:FragmentProfileBinding
    private var checkedItem: Int = 0
    private var selected: String = ""
    private val CHECKED_ITEM = "checked_item"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.fragment_profile, container, false)
        binding = FragmentProfileBinding.bind(view)


    binding.themeFragment.setOnClickListener {
        showDialog()
    }


        return view
    }

    fun showDialog() {
        val themes = resources.getStringArray(R.array.theme)
        val builder = MaterialAlertDialogBuilder(requireContext())
        builder.setTitle("Select Theme")
        builder.setSingleChoiceItems(
            R.array.theme,
            getCheckedItem()
        ) { dialogInterface: DialogInterface, i: Int ->
            selected = themes[i]
            checkedItem = i
        }

        builder.setPositiveButton("OK") { dialogInterface: DialogInterface, i: Int ->
            if (selected == null) {
                selected = themes[i]
                checkedItem = i
            }

            when (selected) {
                "System Default" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                "Dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                "Light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

            }
            setCheckedItem(checkedItem)
        }

        builder.setNegativeButton("Cancel") { dialogInterface: DialogInterface, i: Int ->
            dialogInterface.dismiss()
        }

        val dialog = builder.create()
        dialog.show()
    }
    private fun getCheckedItem(): Int {
        return requireContext().getSharedPreferences("YourSharedPreferencesName", Context.MODE_PRIVATE)
            .getInt(CHECKED_ITEM, checkedItem)
    }

    private fun setCheckedItem(i: Int) {
        requireContext().getSharedPreferences("YourSharedPreferencesName", Context.MODE_PRIVATE)
            .edit()
            .putInt(CHECKED_ITEM, i)
            .apply()
    }
}