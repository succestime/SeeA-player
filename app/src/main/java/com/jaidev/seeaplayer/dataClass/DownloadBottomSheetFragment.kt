
package com.jaidev.seeaplayer.dataClass

import android.Manifest
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Switch
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.jaidev.seeaplayer.DownloadsActivity
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.databinding.BottomSheetLayoutBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
@SuppressLint("UseSwitchCompatOrMaterialCode")
class DownloadBottomSheetFragment : BottomSheetDialogFragment() {

    private lateinit var binding: BottomSheetLayoutBinding
    private lateinit var rememberChoice: Switch
    companion object {
        private const val ARG_TITLE = "title"
        private const val ARG_IMAGE_URL = "image_url"
        private const val STORAGE_PERMISSION_CODE = 1000
        fun newInstance(title: String, imageUrl: String): DownloadBottomSheetFragment {
            val fragment = DownloadBottomSheetFragment()
            val args = Bundle()
            args.putString(ARG_TITLE, title)
            args.putString(ARG_IMAGE_URL, imageUrl)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = BottomSheetLayoutBinding.inflate(inflater, container, false)
        // Initialize the rememberChoice switch
        rememberChoice = binding.rememberChoice

        // Set up the click listener for constraintLayout5
        binding.constraintLayout5.setOnClickListener {
            rememberChoice.isChecked = !rememberChoice.isChecked
            blinkBackground(binding.constraintLayout5)
        }
        binding.rememberChoice.setOnClickListener {
            rememberChoice.isChecked = rememberChoice.isChecked
            blinkBackground(binding.constraintLayout5)
        }
        // Set up the focus change listener for constraintLayout5
        binding.rememberChoice.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                blinkBackground(binding.constraintLayout5)
            }
        }
        binding.constraintLayout5.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                blinkBackground(binding.constraintLayout5)
            }
        }
        binding.downloadButton.setOnClickListener {
//
//            checkPermissionAndDownload(videoUrl)
        }

        val title = arguments?.getString(ARG_TITLE)
        val imageUrl = arguments?.getString(ARG_IMAGE_URL)

        // Set the title and image using Glide
        binding.urlname.text = title
        Glide.with(this)
            .load(imageUrl)
            .placeholder(R.mipmap.ic_logo_o) // Placeholder image until loaded
            .error(R.mipmap.ic_logo_o) // Error image if loading fails
            .into(binding.urlImage)

        return binding.root
    }
    private fun blinkBackground(view: View) {
        val colorFrom = ContextCompat.getColor(requireContext(), R.color.transparent) // Starting color
        val colorTo = Color.WHITE // Blinking color

        val animator = ObjectAnimator.ofObject(view, "backgroundColor", ArgbEvaluator(), colorFrom, colorTo)
        animator.duration = 500 // Duration of one blink cycle in milliseconds
        animator.repeatMode = ObjectAnimator.REVERSE
        animator.repeatCount = 3 // Number of blink cycles

        animator.start()
    }
    private fun showCustomDialog() {
        // Inflate the custom layout/view
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.downloaded_started, null)

        // Create AlertDialog builder
        val builder = AlertDialog.Builder(requireContext())
        builder.setView(dialogView)

        // Create AlertDialog
        val alertDialog = builder.create()

        // Set dialog properties (optional)
        alertDialog.setCanceledOnTouchOutside(true)

        // Find and set up buttons inside the dialog
        val notNowButton: Button = dialogView.findViewById(R.id.notNowButton)
        val viewButton: Button = dialogView.findViewById(R.id.viewButton)

        notNowButton.setOnClickListener {
            // Handle "Not now" button click
            alertDialog.dismiss()
            requireActivity().finish()
            dismiss()
        }

        viewButton.setOnClickListener {
            startActivity(Intent(requireContext(), DownloadsActivity::class.java))
            alertDialog.dismiss()
            dismiss()
            requireActivity().finish()
        }

        // Show the dialog
        alertDialog.show()
    }

    override fun onStart() {
        super.onStart()
        dialog?.let {
            val bottomSheet = it.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            val behavior = BottomSheetBehavior.from(bottomSheet)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return object : BottomSheetDialog(requireContext(), theme) {
            @Deprecated("Deprecated in Java")
            @SuppressLint("MissingSuperCall")
            override fun onBackPressed() {
                requireActivity().finish()
                dismiss()
            }
        }
    }
    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        requireActivity().finish()
    }

    private fun checkPermissionAndDownload(url: String) {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
            == PackageManager.PERMISSION_GRANTED) {
            downloadFile(url)
        } else {
            ActivityCompat.requestPermissions(requireActivity(),
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), STORAGE_PERMISSION_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Permission granted, start the download
            downloadFile(arguments?.getString(ARG_IMAGE_URL) ?: "")
        } else {
            // Permission denied
            Toast.makeText(requireContext(), "Permission denied to write to storage", Toast.LENGTH_SHORT).show()
        }
    }
    private fun downloadFile(url: String) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val client = OkHttpClient()
                    val request = Request.Builder().url(url).build()
                    val response = client.newCall(request).execute()
                    if (!response.isSuccessful) throw IOException("Failed to download file: ${response.message}")

                    val fileName = url.substringAfterLast("/")
                    val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
                    val sink = FileOutputStream(file)

                    sink.use {
                        it.write(response.body?.bytes())
                    }

                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "File downloaded successfully: $fileName", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Failed to download file", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}


