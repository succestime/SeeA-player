package com.jaidev.seeaplayer.dataClass

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Switch
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.jaidev.seeaplayer.DownloadsActivity
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.databinding.BottomSheetLayoutBinding

@SuppressLint("UseSwitchCompatOrMaterialCode")
class DownloadBottomSheetFragment : BottomSheetDialogFragment() {

    private lateinit var binding: BottomSheetLayoutBinding
    private lateinit var rememberChoice: Switch
    companion object {
        private const val ARG_TITLE = "title"
        private const val ARG_IMAGE_URL = "image_url"

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
            showCustomDialog()
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
}
