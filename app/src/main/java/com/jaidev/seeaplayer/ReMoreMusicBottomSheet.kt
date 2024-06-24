package com.jaidev.seeaplayer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.audiofx.AudioEffect
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jaidev.seeaplayer.dataClass.exitApplication
import com.jaidev.seeaplayer.dataClass.getImgArt
import com.jaidev.seeaplayer.databinding.FragmentReMoreMusicBottomSheetBinding
import com.jaidev.seeaplayer.recantFragment.ReMusicPlayerActivity


class ReMoreMusicBottomSheet : BottomSheetDialogFragment() {
    private lateinit var binding: FragmentReMoreMusicBottomSheetBinding
    private var currentPlaybackSpeed: Float = 1.0f // Default speed

    companion object {
        private const val REQUEST_AUDIO_PERMISSION = 100

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentReMoreMusicBottomSheetBinding.inflate(inflater, container, false)
        // Retrieve the current playback speed from arguments
        currentPlaybackSpeed = arguments?.getFloat("CURRENT_SPEED") ?: 1.0f

        binding.shareText.setOnClickListener {
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.type = "audio/*"
            shareIntent.putExtra(
                Intent.EXTRA_STREAM,
                Uri.parse(ReMusicPlayerActivity.reMusicList[ReMusicPlayerActivity.songPosition].path)
            )
            startActivity(Intent.createChooser(shareIntent, "Sharing Music File!!"))
            dismiss()
        }

        loadThumbnailImage()

        binding.repeat.setOnClickListener {
            if (!ReMusicPlayerActivity.repeat) {
                ReMusicPlayerActivity.repeat = true
                ReMusicPlayerActivity.binding.repeatBtnPA.setColorFilter(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.cool_green
                    )
                )
                Toast.makeText(requireContext(), "Repeat mode is on", Toast.LENGTH_SHORT).show()
                dismiss()
            } else {
                ReMusicPlayerActivity.repeat = false
                Toast.makeText(requireContext(), "Repeat mode is off", Toast.LENGTH_SHORT).show()
                ReMusicPlayerActivity.binding.repeatBtnPA.setColorFilter(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.cool_pink
                    )
                )
                dismiss()
            }
        }

        binding.speed.setOnClickListener {
            val speedBottomSheet = SpeedMusicBottomSheet()
            speedBottomSheet.setCurrentSpeed(currentPlaybackSpeed)
            speedBottomSheet.show(parentFragmentManager, speedBottomSheet.tag)
            dismiss()
        }


        binding.equalizer.setOnClickListener {
            if (isEqualizerSupported()) {
                if (checkPermission()) {
                    launchEqualizer()
                    dismiss()
                } else {
                    requestPermission()
                }
            } else {
                Toast.makeText(
                    requireContext(),
                    "Equalizer Feature not Supported!!",
                    Toast.LENGTH_SHORT
                ).show()
                dismiss()
            }
        }


        binding.sleepTimer.setOnClickListener {
            val timer =
                ReMusicPlayerActivity.min10 || ReMusicPlayerActivity.min15 || ReMusicPlayerActivity.min20 || ReMusicPlayerActivity.min30 || ReMusicPlayerActivity.min60
            if (!timer) showBottomSheetDialog()
            else {
                val builder = MaterialAlertDialogBuilder(requireContext())
                builder.setTitle("Stop Timer")
                    .setMessage("Do you want to stop timer?")
                    .setPositiveButton("Yes") { _, _ ->
                        ReMusicPlayerActivity.min10 = false
                        ReMusicPlayerActivity.min15 = false
                        ReMusicPlayerActivity.min20 = false
                        ReMusicPlayerActivity.min30 = false
                        ReMusicPlayerActivity.min60 = false
                        dismiss()
                    }
                    .setNegativeButton("No") { dialog, _ ->
                        dialog.dismiss()
                        dismiss()
                    }
                val customDialog = builder.create()
                customDialog.show()
            }
        }
        return binding.root
    }

    private fun isEqualizerSupported(): Boolean {
        val pm = requireContext().packageManager
        return pm.hasSystemFeature(PackageManager.FEATURE_AUDIO_OUTPUT)
    }

    private fun checkPermission(): Boolean {
        val permission = Manifest.permission.MODIFY_AUDIO_SETTINGS
        return ContextCompat.checkSelfPermission(
            requireContext(),
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            requireActivity(), arrayOf(Manifest.permission.MODIFY_AUDIO_SETTINGS),
            REQUEST_AUDIO_PERMISSION
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_AUDIO_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchEqualizer()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Permission denied to access the equalizer",
                    Toast.LENGTH_SHORT
                ).show()
                // Optionally, prompt the user to grant permission from app settings
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.fromParts("package", requireContext().packageName, null)
                startActivity(intent)
            }
        }
    }

    private fun loadThumbnailImage() {
        val currentSong = (requireActivity() as ReMusicPlayerActivity).getCurrentSong()

        Glide.with(this)
            .load(getImgArt(ReMusicPlayerActivity.reMusicList[ReMusicPlayerActivity.songPosition].path))
            .placeholder(R.drawable.music_speaker_three)
            .error(R.drawable.music_speaker_three)
            .into(binding.thumbnail)
        // Set the title of the current playing music
        binding.titleMusic.text = currentSong.title
        binding.subtitleMusic.text = currentSong.album
    }

    private fun launchEqualizer() {
        try {
            val eqIntent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
            eqIntent.putExtra(
                AudioEffect.EXTRA_AUDIO_SESSION,
                ReMusicPlayerActivity.musicService!!.mediaPlayer!!.audioSessionId
            )
            eqIntent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, requireContext().packageName)
            eqIntent.putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
            startActivityForResult(eqIntent, 13)
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                "Equalizer Feature not Supported!!",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun showBottomSheetDialog() {

        val dialog = BottomSheetDialog(requireActivity())
        dialog.setContentView(R.layout.bottom_sheet_dialog)
        dialog.show()
        dialog.findViewById<LinearLayout>(R.id.min_10)?.setOnClickListener {
            Toast.makeText(
                requireActivity(),
                "Music will stop after 10 minutes",
                Toast.LENGTH_SHORT
            )
                .show()
//            binding.timerBtnPA?.setColorFilter(ContextCompat.getColor(this, R.color.cool_green))
            ReMusicPlayerActivity.min10 = true
            Thread {
                Thread.sleep((10 * 60000).toLong())
                if (ReMusicPlayerActivity.min10) exitApplication()
            }.start()
            dialog.dismiss()
            dismiss()
        }
        dialog.findViewById<LinearLayout>(R.id.min_15)?.setOnClickListener {
            Toast.makeText(
                requireActivity(),
                "Music will stop after 15 minutes",
                Toast.LENGTH_SHORT
            )
                .show()
//            binding.timerBtnPA?.setColorFilter(ContextCompat.getColor(this, R.color.cool_green))
            ReMusicPlayerActivity.min15 = true
            Thread {
                Thread.sleep((15 * 60000).toLong())
                if (ReMusicPlayerActivity.min15) exitApplication()
            }.start()
            dialog.dismiss()
            dismiss()
        }
        dialog.findViewById<LinearLayout>(R.id.min_20)?.setOnClickListener {
            Toast.makeText(
                requireActivity(),
                "Music will stop after 20 minutes",
                Toast.LENGTH_SHORT
            )
                .show()
//            binding.timerBtnPA?.setColorFilter(ContextCompat.getColor(this, R.color.cool_green))
            ReMusicPlayerActivity.min20 = true
            Thread {
                Thread.sleep((20 * 60000).toLong())
                if (ReMusicPlayerActivity.min20) exitApplication()
            }.start()
            dialog.dismiss()
            dismiss()
        }
        dialog.findViewById<LinearLayout>(R.id.min_30)?.setOnClickListener {
            Toast.makeText(
                requireActivity(),
                "Music will stop after 30 minutes",
                Toast.LENGTH_SHORT
            )
                .show()
//            binding.timerBtnPA?.setColorFilter(ContextCompat.getColor(this, R.color.cool_green))
            ReMusicPlayerActivity.min30 = true
            Thread {
                Thread.sleep((30 * 60000).toLong())
                if (ReMusicPlayerActivity.min30) exitApplication()
            }.start()
            dialog.dismiss()
            dismiss()
        }
        dialog.findViewById<LinearLayout>(R.id.min_60)?.setOnClickListener {
            Toast.makeText(
                requireActivity(),
                "Music will stop after 60 minutes",
                Toast.LENGTH_SHORT
            )
                .show()
//            binding.timerBtnPA?.setColorFilter(ContextCompat.getColor(this, R.color.cool_green))
            ReMusicPlayerActivity.min60 = true
            Thread {
                Thread.sleep((60 * 60000).toLong())
                if (ReMusicPlayerActivity.min60) exitApplication()
            }.start()
            dialog.dismiss()
            dismiss()
        }
    }
}


