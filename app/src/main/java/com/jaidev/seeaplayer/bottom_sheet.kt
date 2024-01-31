package com.jaidev.seeaplayer

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import com.jaidev.seeaplayer.databinding.ActivityBottomSheetBinding

class bottom_sheet : AppCompatActivity() {
    private lateinit var videoAdapter: VideoAdapter
    private lateinit var binding: ActivityBottomSheetBinding

    companion object {
  }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBottomSheetBinding.inflate(layoutInflater)
        setTheme(R.style.coolBlueNav)
        setContentView(binding.root)
        videoAdapter = VideoAdapter(this, MainActivity.videoList)

        binding.renameBtn.setOnClickListener {
            // Add your functionality here
            // For instance, you can show a dialog to get a new name for the video
            showRenameDialog()
        }


    }
    private fun showRenameDialog() {
        val dialog = AlertDialog.Builder(this)
        val editText = EditText(this)
        editText.hint = "Enter new video name"
        dialog.setView(editText)
            .setTitle("Rename Video")
            .setPositiveButton("Rename") { _, _ ->
                val newName = editText.text.toString()

            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

}