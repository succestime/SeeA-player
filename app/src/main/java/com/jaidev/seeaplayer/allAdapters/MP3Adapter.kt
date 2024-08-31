package com.jaidev.seeaplayer.allAdapters

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.text.format.DateUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.arthenica.mobileffmpeg.FFmpeg
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.jaidev.seeaplayer.MP3playerActivity
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.dataClass.MP3AppDatabase
import com.jaidev.seeaplayer.dataClass.MP3FileData
import com.jaidev.seeaplayer.databinding.RecantDownloadViewBinding
import com.jaidev.seeaplayer.musicActivity.PlayerMusicActivity.Companion.nowMusicPlayingId
import com.jaidev.seeaplayer.musicActivity.PlayerMusicActivity.Companion.songPosition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

class MP3Adapter(private val context: Context, private val mp3Files: ArrayList<MP3FileData>) :
    RecyclerView.Adapter<MP3Adapter.MP3ViewHolder>() {

    private val database = MP3AppDatabase.getDatabase(context).mp3FileDao()
    private val job = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + job)

    companion object {
        const val REQUEST_WRITE_SETTINGS = 1234 // Unique request code for settings permission
    }

    class MP3ViewHolder(binding: RecantDownloadViewBinding) : RecyclerView.ViewHolder(binding.root) {
        val title = binding.videoName
        val duration = binding.duration
        val videoImage = binding.videoImage
        val root = binding.root
        val more = binding.MoreChoose
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MP3ViewHolder {
        return MP3ViewHolder(
            RecantDownloadViewBinding.inflate(
                LayoutInflater.from(context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: MP3ViewHolder, position: Int) {
        val mp3Audio = mp3Files[position]
        holder.title.text = mp3Audio.title
        holder.duration.text = DateUtils.formatElapsedTime(mp3Audio.duration / 1000)
        Glide.with(context)
            .load(mp3Audio.artUri)
            .apply(RequestOptions().placeholder(R.color.place_holder_video).centerCrop())
            .into(holder.videoImage)

        holder.root.setOnClickListener {
            when (mp3Audio.id) {
                nowMusicPlayingId ->
                    sendIntent(
                        ref = "MP3NowPlaying",
                        pos = songPosition
                    )
                else -> sendIntent(ref = "MP3AudioList", pos = position)
            }
        }

        holder.more.setOnClickListener {
            showMoreOptionsPopup(holder.more, position)
        }
    }

    private fun sendIntent(ref: String, pos: Int) {
        val intent = Intent(context, MP3playerActivity::class.java)
        intent.putExtra("index", pos)
        intent.putExtra("class", ref)
        ContextCompat.startActivity(context, intent, null)
    }

    override fun getItemCount(): Int {
        return mp3Files.size
    }

    @SuppressLint("ObsoleteSdkInt", "InflateParams")
    private fun showMoreOptionsPopup(anchorView: View, position: Int) {
        val popupView = LayoutInflater.from(context).inflate(R.layout.mp3_menu_long, null)
        val popupWindow = PopupWindow(
            popupView,
            ConstraintLayout.LayoutParams.WRAP_CONTENT,
            ConstraintLayout.LayoutParams.WRAP_CONTENT,
            true
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            popupWindow.elevation = 15f
        }
        popupWindow.isFocusable = true
        popupWindow.update()

        val location = IntArray(2)
        anchorView.getLocationOnScreen(location)
        val xOffset = 20.spToPx(context)
        val yOffset = 20.spToPx(context)
        popupWindow.showAtLocation(
            anchorView,
            Gravity.NO_GRAVITY,
            location[0] - popupWindow.width + xOffset,
            location[1] - popupWindow.height + yOffset
        )

        popupView.findViewById<View>(R.id.removeTextView).setOnClickListener {
            val builder = android.app.AlertDialog.Builder(context)
            val dialogView = LayoutInflater.from(context).inflate(R.layout.mp3_remove, null)
            builder.setView(dialogView)


            val dialog = builder.create()

            val cancelButton: Button = dialogView.findViewById(R.id.cancel_button)
            val removeButton: Button = dialogView.findViewById(R.id.remove_button)

            cancelButton.setOnClickListener {
                dialog.dismiss()
            }

            removeButton.setOnClickListener {
                // Remove from database and update RecyclerView
                val fileId = mp3Files[position].id

                coroutineScope.launch {
                    withContext(Dispatchers.IO) {
                        database.deleteById(fileId)
                    }
                    mp3Files.removeAt(position)
                    notifyItemRemoved(position)
                    notifyItemRangeChanged(position, mp3Files.size)
                    // Check if the list is empty and update no_playlists_text visibility
                    if (mp3Files.isEmpty()) {
                        (context as? Activity)?.findViewById<TextView>(R.id.no_playlists_text)?.visibility = View.VISIBLE
                    }
                    dialog.dismiss()
                }
            }

            dialog.show()
            popupWindow.dismiss()
        }

        popupView.findViewById<View>(R.id.shareTextView).setOnClickListener {
            val currentMP3File = mp3Files[position]
            val mp3File = File(currentMP3File.path)
            val fileUri = getUriForFile(mp3File)

            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "audio/*"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            ContextCompat.startActivity(
                context,
                Intent.createChooser(shareIntent, "Share MP3 file via"),
                null
            )

            popupWindow.dismiss()
        }

        popupView.findViewById<View>(R.id.ringtoneTextView).setOnClickListener {
            showRingtoneOptionsBottomSheet(position)
            popupWindow.dismiss()
        }

        popupWindow.showAsDropDown(anchorView, 0, 0)
    }

    private fun Int.spToPx(context: Context): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }

    @SuppressLint("InflateParams", "ObsoleteSdkInt")
    private fun showRingtoneOptionsBottomSheet(position: Int) {
        val bottomSheetDialog = BottomSheetDialog(context)
        val bottomSheetView = LayoutInflater.from(context).inflate(
            R.layout.ringtone_music_bottom_sheet,
            null
        )

        val currentMP3File = mp3Files[position]
        bottomSheetView.findViewById<TextView>(R.id.MP3Title).text = currentMP3File.title

        bottomSheetView.findViewById<View>(R.id.setAsPhoneRingtoneButton).setOnClickListener {

            val builderTone = AlertDialog.Builder(context)
            val dialogViewTone =
                LayoutInflater.from(context).inflate(R.layout.favurite_ringtone, null)
            builderTone.setView(dialogViewTone)

            val dialogTone = builderTone.create()

            val notButton: Button = dialogViewTone.findViewById(R.id.not_button)
            val yesButton: Button = dialogViewTone.findViewById(R.id.yes_button)

            notButton.setOnClickListener {
                dialogTone.dismiss()
            }

            // Inside yesButton.setOnClickListener
            yesButton.setOnClickListener {
                if (checkAndRequestPermissions()) {
                    setAsRingtone(currentMP3File)
                }
                dialogTone.dismiss()
                bottomSheetDialog.dismiss()
            }

            dialogTone.show()
            bottomSheetDialog.dismiss()
        }

        bottomSheetView.findViewById<View>(R.id.setAsNotificationRingtoneButton).setOnClickListener {
            if (checkAndRequestPermissions()) {
                setAsNotificationTone(currentMP3File)
            }
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.setContentView(bottomSheetView)
        bottomSheetDialog.show()
    }
    private fun setAsRingtone(mp3FileData: MP3FileData) {
        try {
            val sourceFile = File(mp3FileData.path)
            val ringtoneDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_RINGTONES),
                "SeeA MP3 Audio"
            )

            if (!ringtoneDir.exists()) {
                ringtoneDir.mkdirs()
            }

            val newFile = File(ringtoneDir, sourceFile.name)
            sourceFile.copyTo(newFile, overwrite = true)

            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DATA, newFile.absolutePath)
                put(MediaStore.MediaColumns.TITLE, mp3FileData.title)
                put(MediaStore.MediaColumns.MIME_TYPE, "audio/mpeg")
                put(MediaStore.Audio.Media.IS_RINGTONE, true)
                put(MediaStore.Audio.Media.IS_NOTIFICATION, false)
                put(MediaStore.Audio.Media.IS_ALARM, false)
                put(MediaStore.Audio.Media.IS_MUSIC, false)
            }

            val uri = MediaStore.Audio.Media.getContentUriForPath(newFile.absolutePath)
            val newUri = context.contentResolver.insert(uri!!, values)

            RingtoneManager.setActualDefaultRingtoneUri(
                context,
                RingtoneManager.TYPE_RINGTONE,
                newUri
            )

            Toast.makeText(context, "Ringtone set to ${mp3FileData.title}", Toast.LENGTH_LONG).show()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to set ringtone.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to set ringtone.", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun checkAndRequestPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(context)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_WRITE_SETTINGS,
                    Uri.parse("package:${context.packageName}")
                )
                (context as Activity).startActivityForResult(intent, REQUEST_WRITE_SETTINGS)
                return false
            }
        }
        return true
    }

    private fun setAsNotificationTone(mp3FileData: MP3FileData) {
        try {
            val sourceFile = File(mp3FileData.path)
            val notificationDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_RINGTONES),
                "SeeA MP3 Audio"
            )

            if (!notificationDir.exists()) {
                notificationDir.mkdirs()
            }

            val trimmedFile = File(notificationDir, "trimmed_${sourceFile.name}")

            // Use FFmpeg to trim the audio to 3 seconds
            val command = arrayOf("-i", sourceFile.absolutePath, "-t", "00:00:03", "-c", "copy", trimmedFile.absolutePath)
            val rc = FFmpeg.execute(command)

            if (rc != 0) {
                throw Exception("FFmpeg failed with return code $rc")
            }

            // Make the trimmed file accessible
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DATA, trimmedFile.absolutePath)
                put(MediaStore.MediaColumns.TITLE, "Notification Tone")
                put(MediaStore.MediaColumns.MIME_TYPE, "audio/mp3")
                put(MediaStore.Audio.Media.IS_RINGTONE, false)
                put(MediaStore.Audio.Media.IS_NOTIFICATION, true)
                put(MediaStore.Audio.Media.IS_ALARM, false)
                put(MediaStore.Audio.Media.IS_MUSIC, false)
            }

            // Insert the file into the media store
            val uri = MediaStore.Audio.Media.getContentUriForPath(trimmedFile.absolutePath)
            val newUri = context.contentResolver.insert(uri!!, values)

            // Set the new notification tone
            RingtoneManager.setActualDefaultRingtoneUri(
                context,
                RingtoneManager.TYPE_NOTIFICATION,
                newUri
            )

            // Notify the user
            Toast.makeText(context, "Notification tone set successfully.", Toast.LENGTH_SHORT).show()

        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to set notification tone.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to trim or set notification tone.", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun getUriForFile(file: File): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
        } else {
            Uri.fromFile(file)
        }
    }
}
