package com.jaidev.seeaplayer.allAdapters

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.ActionMode
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.cardview.widget.CardView
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.jaidev.seeaplayer.Services.FileItemPreferences
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.dataClass.FileItem
import com.jaidev.seeaplayer.dataClass.FileType
import java.io.File

class FileAdapter(
    private val context: Context,
    private var fileList: MutableList<FileItem>,
    private val itemClickListener: OnItemClickListener,

) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    interface OnItemClickListener {
        fun onItemClick(fileItem: FileItem)
    }

    private val selectedItems = HashSet<Int>()
    private var actionMode: ActionMode? = null
    private val fileItemPrefs = FileItemPreferences(context)
    @SuppressLint("NotifyDataSetChanged")
    fun filterList(filteredList: List<FileItem>) {
        fileList = ArrayList()
        fileList.addAll(filteredList)
        notifyDataSetChanged()
    }


    // ViewHolder for PDF type
    inner class PdfViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val fileNameTextView: TextView = itemView.findViewById(R.id.titleWebsiteView)
        private val fileSizeTextView: TextView = itemView.findViewById(R.id.WebsiteSizeView)
        private val fileMoreTextView: ImageButton = itemView.findViewById(R.id.iconMoreView)

        init {
            fileMoreTextView.setOnClickListener {
                showPopupMenu()
            }
        }
        private fun showPopupMenu() {
            val popupMenu = PopupMenu(itemView.context, fileMoreTextView)
            popupMenu.menuInflater.inflate(R.menu.popup_menu_browser, popupMenu.menu)

            // Set icons to be visible
            try {
                val fieldPopup = PopupMenu::class.java.getDeclaredField("mPopup")
                fieldPopup.isAccessible = true
                val popup = fieldPopup.get(popupMenu)
                popup.javaClass.getDeclaredMethod("setForceShowIcon", Boolean::class.java)
                    .invoke(popup, true)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.shareBrowser -> {
                        sharePdfFile(fileList[adapterPosition])

                    }
                    R.id.renameBrowser -> {
                        showRenameDialog(adapterPosition)

                    }
                    R.id.deleteBrowser -> {
                        deleteFile(adapterPosition) // Call deleteFile with correct position

                    }


                }
                true
            }

            popupMenu.show()
        }

        fun bind(fileItem: FileItem) {
            fileNameTextView.text = fileItem.fileName
            fileSizeTextView.text = formatFileSize(fileItem.fileSize)
            // Display the actual website name
            fileNameTextView.text = fileItemPrefs.getOriginalFileName(fileItem)

// Update item view background based on selection
            if (selectedItems.contains(adapterPosition)) {
                itemView.setBackgroundResource(R.drawable.browser_selected_background) // Set your custom selected background
            } else {
                itemView.setBackgroundResource(android.R.color.transparent) // Reset to default background
            }


            itemView.setOnLongClickListener {
                toggleMultpleSelection(position)

                true
            }
            itemView.setOnClickListener {
                if (actionMode != null) {
                    // If action mode is active, toggle selection as usual
                   toggleMultpleSelection(position)

                } else {
                   itemClickListener.onItemClick(fileItem)
                }

            }
        }
    }


    // ViewHolder for Image type
    inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.musicBrowserImage)
        private val fileSelectImageView: ImageButton = itemView.findViewById(R.id.multiMusicIcon)
        private val fileIconImageView: ImageView = itemView.findViewById(R.id.musicBrowserImage)

        fun bind(fileItem: FileItem) {
            Glide.with(context)
                .load(fileItem.artUri) // Assuming artUri contains the image URI
                .placeholder(R.drawable.image_browser) // Placeholder image while loading
                .error(R.drawable.image_browser) // Image to display on error
                .into(imageView)

            if (selectedItems.contains(adapterPosition)) {
                itemView.setBackgroundResource(R.drawable.browser_selected_image_background) // Set your custom selected background
            } else {
                itemView.setBackgroundResource(android.R.color.transparent) // Reset to default background
            }

            itemView.setOnLongClickListener {
                toggleMultpleSelection(position)

                true
            }
            itemView.setOnClickListener {
                if (actionMode != null) {
                    // If action mode is active, toggle selection as usual
                  toggleMultpleSelection(position)

                } else {
                    itemClickListener.onItemClick(fileItem)
                }

            }
        }

        }

    // ViewHolder for Video type
    inner class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val videoNameTextView: TextView = itemView.findViewById(R.id.videoBrowserName)
        private val durationTextView: TextView = itemView.findViewById(R.id.durationBrowser)
        private val videoMoreTextView: ImageButton = itemView.findViewById(R.id.MoreVideoChoose)
        private val fileSelectImageView: ImageButton = itemView.findViewById(R.id.multiVideoIcon)
        private val fileIconImageView: CardView = itemView.findViewById(R.id.videoBrowserCard)

        init {
            videoMoreTextView.setOnClickListener {
                showPopupMenu()
            }
        }
        private fun showPopupMenu() {
            val popupMenu = PopupMenu(itemView.context, videoMoreTextView)
            popupMenu.menuInflater.inflate(R.menu.popup_menu_browser, popupMenu.menu)

            // Set icons to be visible
            try {
                val fieldPopup = PopupMenu::class.java.getDeclaredField("mPopup")
                fieldPopup.isAccessible = true
                val popup = fieldPopup.get(popupMenu)
                popup.javaClass.getDeclaredMethod("setForceShowIcon", Boolean::class.java)
                    .invoke(popup, true)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.shareBrowser -> {
                        shareVideoFile(fileList[adapterPosition])

                    }
                    R.id.renameBrowser -> {
                        showRenameDialog(adapterPosition)

                    }
                    R.id.deleteBrowser -> {
                        deleteFile(adapterPosition) // Call deleteFile with correct position

                    }


                }
                true
            }

            popupMenu.show()
        }
        @SuppressLint("SetTextI18n")
        fun bind(fileItem: FileItem) {
            videoNameTextView.text = fileItem.fileName
            durationTextView.text = formatFileSize(fileItem.fileSize)
            videoNameTextView.text = fileItemPrefs.getOriginalFileName(fileItem)

            if (selectedItems.contains(adapterPosition)) {
                itemView.setBackgroundResource(R.drawable.browser_selected_background) // Set your custom selected background
            } else {
                itemView.setBackgroundResource(android.R.color.transparent) // Reset to default background
            }
            itemView.setOnLongClickListener {
                toggleMultpleSelection(position)
                // Get the current visibility state of fileIconImageView

                true
            }
            itemView.setOnClickListener {
                if (actionMode != null) {
                    // If action mode is active, toggle selection as usual
                    toggleMultpleSelection(position)

                } else {
                    itemClickListener.onItemClick(fileItem)
                }

            }
        }

    }
    // ViewHolder for Audio type
    inner class AudioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val audioNameTextView: TextView = itemView.findViewById(R.id.titleAudioView)
        private val audioSizeTextView: TextView = itemView.findViewById(R.id.AudioSizeView)
        private val audioMoreTextView: ImageButton = itemView.findViewById(R.id.iconAudioView)

        init {
            audioMoreTextView.setOnClickListener {
                showPopupMenu()
            }
        }


        private fun showPopupMenu() {
            val popupMenu = PopupMenu(itemView.context, audioMoreTextView)
            popupMenu.menuInflater.inflate(R.menu.popup_menu_browser, popupMenu.menu)

            // Set icons to be visible
            try {
                val fieldPopup = PopupMenu::class.java.getDeclaredField("mPopup")
                fieldPopup.isAccessible = true
                val popup = fieldPopup.get(popupMenu)
                popup.javaClass.getDeclaredMethod("setForceShowIcon", Boolean::class.java)
                    .invoke(popup, true)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.shareBrowser -> {
                        shareAudioFile(fileList[adapterPosition])
                    }
                    R.id.renameBrowser -> {
                        showRenameDialog(adapterPosition)

                    }
                    R.id.deleteBrowser -> {
                        deleteFile(adapterPosition) // Call deleteFile with correct position

                    }


                }
                true
            }

            popupMenu.show()
        }
        fun bind(fileItem: FileItem) {
            audioNameTextView.text = fileItem.fileName
            audioSizeTextView.text = formatFileSize(fileItem.fileSize)
            audioNameTextView.text = fileItemPrefs.getOriginalFileName(fileItem)

            if (selectedItems.contains(adapterPosition)) {
                itemView.setBackgroundResource(R.drawable.browser_selected_background) // Set your custom selected background
            } else {
                itemView.setBackgroundResource(android.R.color.transparent) // Reset to default background
            }
            itemView.setOnLongClickListener {
                toggleMultpleSelection(position)
                // Get the current visibility state of fileIconImageView

                true
            }
            itemView.setOnClickListener {
                if (actionMode != null) {
                    // If action mode is active, toggle selection as usual
                    toggleMultpleSelection(position)

                } else {
                    itemClickListener.onItemClick(fileItem)
                }

            }
        }
    }

    inner class WebsiteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val websiteNameTextView: TextView = itemView.findViewById(R.id.titleWebApkView)
        private val websiteSizeTextView: TextView = itemView.findViewById(R.id.WebApkSizeView)
        private val websiteMoreTextView: ImageButton = itemView.findViewById(R.id.iconApkView)

        init {
            websiteMoreTextView.setOnClickListener {
                showPopupMenu()
            }
        }


        private fun showPopupMenu() {
            val popupMenu = PopupMenu(itemView.context, websiteMoreTextView)
            popupMenu.menuInflater.inflate(R.menu.popup_menu_browser, popupMenu.menu)

            // Set icons to be visible
            try {
                val fieldPopup = PopupMenu::class.java.getDeclaredField("mPopup")
                fieldPopup.isAccessible = true
                val popup = fieldPopup.get(popupMenu)
                popup.javaClass.getDeclaredMethod("setForceShowIcon", Boolean::class.java)
                    .invoke(popup, true)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.shareBrowser -> {
                        shareWebsiteFile(fileList[adapterPosition])

                    }
                    R.id.renameBrowser -> {
                        showRenameDialog(adapterPosition)

                    }
                    R.id.deleteBrowser -> {
                        deleteFile(adapterPosition) // Call deleteFile with correct position

                    }


                }
                true
            }

            popupMenu.show()
        }
        fun bind(fileItem: FileItem) {
            websiteNameTextView.text = fileItem.fileName
            websiteSizeTextView.text = formatFileSize(fileItem.fileSize)
            websiteNameTextView.text = fileItemPrefs.getOriginalFileName(fileItem)

            if (selectedItems.contains(adapterPosition)) {
                itemView.setBackgroundResource(R.drawable.browser_selected_background) // Set your custom selected background
            } else {
                itemView.setBackgroundResource(android.R.color.transparent) // Reset to default background
            }
            itemView.setOnLongClickListener {
                toggleMultpleSelection(position)

                true
            }
            itemView.setOnClickListener {
                if (actionMode != null) {
                    // If action mode is active, toggle selection as usual
                    toggleMultpleSelection(position)

                } else {
                   itemClickListener.onItemClick(fileItem)
                }

            }
        }

    }

    inner class ApkViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val apkNameTextView: TextView = itemView.findViewById(R.id.titleWebApkView)
        private val apkSizeTextView: TextView = itemView.findViewById(R.id.WebApkSizeView)
        private val websiteMoreTextView: ImageButton = itemView.findViewById(R.id.iconApkView)

        init {
            websiteMoreTextView.setOnClickListener {
                showPopupMenu()
            }
        }


        private fun showPopupMenu() {
            val popupMenu = PopupMenu(itemView.context, websiteMoreTextView)
            popupMenu.menuInflater.inflate(R.menu.popup_menu_browser, popupMenu.menu)

            // Set icons to be visible
            try {
                val fieldPopup = PopupMenu::class.java.getDeclaredField("mPopup")
                fieldPopup.isAccessible = true
                val popup = fieldPopup.get(popupMenu)
                popup.javaClass.getDeclaredMethod("setForceShowIcon", Boolean::class.java)
                    .invoke(popup, true)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.shareBrowser -> {
                        shareApkFile(fileList[adapterPosition])

                    }
                    R.id.renameBrowser -> {
                        showRenameDialog(adapterPosition)

                    }
                    R.id.deleteBrowser -> {
                        deleteFile(adapterPosition) // Call deleteFile with correct position

                    }


                }
                true
            }

            popupMenu.show()
        }
        fun bind(fileItem: FileItem) {
            apkNameTextView.text = fileItem.fileName
            apkSizeTextView.text = formatFileSize(fileItem.fileSize)
            apkNameTextView.text = fileItemPrefs.getOriginalFileName(fileItem)

            if (selectedItems.contains(adapterPosition)) {
                itemView.setBackgroundResource(R.drawable.browser_selected_background) // Set your custom selected background
            } else {
                itemView.setBackgroundResource(android.R.color.transparent) // Reset to default background
            }

            itemView.setOnLongClickListener {
                toggleMultpleSelection(position)

                true
            }
            itemView.setOnClickListener {
                if (actionMode != null) {
                    // If action mode is active, toggle selection as usual
                    toggleMultpleSelection(position)

                } else {
                    itemView.setOnClickListener { itemClickListener.onItemClick(fileItem) }
                }

            }
        }

    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            FileType.PDF.ordinal -> {
                val pdfView = inflater.inflate(R.layout.other_browser, parent, false)
                PdfViewHolder(pdfView)
            }
            FileType.IMAGE.ordinal -> {
                val imageView = inflater.inflate(R.layout.image_browser, parent, false)
                ImageViewHolder(imageView)
            }
            FileType.VIDEO.ordinal -> {
                val videoView = inflater.inflate(R.layout.video_download_browser, parent, false)
                VideoViewHolder(videoView)
            }
            FileType.AUDIO.ordinal -> {
                val audioView = inflater.inflate(R.layout.audio_browser, parent, false)
                AudioViewHolder(audioView)
            }
            FileType.APK.ordinal -> {
                val audioView = inflater.inflate(R.layout.vpa_browser, parent, false)
                ApkViewHolder(audioView)
            }
            else -> {
                val websiteView = inflater.inflate(R.layout.vpa_browser, parent, false)
                WebsiteViewHolder(websiteView)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val fileItem = fileList[position]
        when (holder.itemViewType) {
            FileType.PDF.ordinal -> {
                (holder as PdfViewHolder).bind(fileItem)
            }
            FileType.IMAGE.ordinal -> {
                (holder as ImageViewHolder).bind(fileItem)
            }
            FileType.VIDEO.ordinal -> {
                (holder as VideoViewHolder).bind(fileItem)
            }
            FileType.AUDIO.ordinal -> {
                (holder as AudioViewHolder).bind(fileItem)
            }
            FileType.APK.ordinal -> {
                (holder as ApkViewHolder).bind(fileItem)
            }
            else -> {
                (holder as WebsiteViewHolder).bind(fileItem)
            }
        }
    }

    override fun getItemCount(): Int {
        return fileList.size
    }

    override fun getItemViewType(position: Int): Int {
        return fileList[position].fileType.ordinal
    }

    // Toggle selection for multi-select
    @SuppressLint("NotifyDataSetChanged")
    private fun toggleMultpleSelection(position: Int) {
        if (selectedItems.contains(position)) {
            selectedItems.remove(position)
        } else {
            selectedItems.add(position)

        }

        // Start or finish action mode based on selection
        if (selectedItems.isEmpty()) {
            actionMode?.finish()
        } else {
            startActionMode()
        }

        // Update selected items
        notifyDataSetChanged()
        actionMode?.invalidate()
    }


    // Start action mode for multi-select
    private fun startActionMode() {
        if (actionMode == null) {
            actionMode = (context as AppCompatActivity).startActionMode(actionModeCallback)
        }
        actionMode?.title = "${selectedItems.size} selected"
    }

    // Action mode callback
    private val actionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            // Inflate action mode menu
            mode?.menuInflater?.inflate(R.menu.multiple_select_menu, menu)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {

            return true
        }
      override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            // Handle action mode menu items
             when (item?.itemId) {
                R.id.shareMultiBrowser -> {
                    shareSelectedFiles()
                    return true
                }
                R.id.deleteMultiBrowser -> {
                    deleteSelectedFiles()
                    return true
                }
                // Add more action mode items as needed
            }
            return false
        }
        @SuppressLint("NotifyDataSetChanged")
        override fun onDestroyActionMode(mode: ActionMode?) {
            // Clear selection and action mode
            selectedItems.clear()
            actionMode = null
            notifyDataSetChanged()


        }
    }
    private fun shareSelectedFiles() {
        val uris = mutableListOf<Uri>()

        // Iterate through selectedItems to get selected file items
        for (position in selectedItems) {
            val fileItem = fileList[position]
            val fileUri = FileProvider.getUriForFile(
                context,
                context.applicationContext.packageName + ".provider",
                File(fileItem.filePath)
            )
            uris.add(fileUri)
        }

        // Create an ACTION_SEND intent to share multiple files
        val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE)
        shareIntent.type = "*/*"
        shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        // Start the intent chooser to share multiple files
        val chooser = Intent.createChooser(shareIntent, "Share Files")
        chooser.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(chooser)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun deleteSelectedFiles() {
        val context = context ?: return

        // Create a list to store items to be deleted
        val itemsToRemove = ArrayList<FileItem>()

        // Iterate over selected items and add corresponding file items to the list
        for (position in selectedItems) {
            val fileItem = fileList[position]
            itemsToRemove.add(fileItem)

            // Get the file corresponding to the FileItem
            val file = File(fileItem.filePath)

            // Delete the file from storage
            if (file.exists()) {
                val deleted = file.delete()
                if (!deleted) {
                    // Handle deletion failure if necessary
                    Log.e("FileAdapter", "Failed to delete file: ${fileItem.fileName}")
                }
            } else {
                // File doesn't exist, handle this scenario if needed
                Log.w("FileAdapter", "File not found: ${fileItem.fileName}")
            }
        }

        // Remove items from the main list
        fileList.removeAll(itemsToRemove)

        // Clear selected items set
        selectedItems.clear()

        // Notify adapter of the changes
        notifyDataSetChanged()
    }

    private fun formatFileSize(fileSize: Long): String {
        return when {
            fileSize < 1024 -> {
                // Display size in bytes if less than 1 KB
                "$fileSize bytes"
            }
            fileSize < 1024 * 1024 -> {
                // Display size in KB if less than 1 MB
                val fileSizeInKB = fileSize / 1024.0
                String.format("%.2f KB", fileSizeInKB)
            }
            fileSize < 1024 * 1024 * 1024 -> {
                // Display size in MB if less than 1 GB
                val fileSizeInMB = fileSize / (1024.0 * 1024)
                String.format("%.2f MB", fileSizeInMB)
            }
            else -> {
                // Display size in GB for larger files
                val fileSizeInGB = fileSize / (1024.0 * 1024 * 1024)
                String.format("%.2f GB", fileSizeInGB)
            }
        }
    }


  @SuppressLint("NotifyDataSetChanged")
    private fun showRenameDialog(position: Int) {
        val oldFileItem = fileList[position]

        val dialog = AlertDialog.Builder(context)
        dialog.setTitle("Rename File")

        val editText = EditText(context)
        editText.setText(oldFileItem.fileName) // Use fileName for display in the EditText

        dialog.setView(editText)

        dialog.setPositiveButton("Rename") { _, _ ->
            val newFileName = editText.text.toString().trim()
            if (newFileName.isNotEmpty()) {
                val oldFile = File(oldFileItem.filePath)
                val parentDir = oldFile.parentFile
                val newFile = File(parentDir, newFileName)

                if (oldFile.renameTo(newFile)) {
                    // Update the file name in the FileItem
                    oldFileItem.fileName = newFileName
                    oldFileItem.originalFileName = newFileName
                    oldFileItem.filePath = newFile.absolutePath // Update the file path if necessary
                    fileItemPrefs.saveFileItem(oldFileItem) // Save updated FileItem
                    notifyDataSetChanged() // Notify adapter of the change
                } else {
                    // Failed to rename the file
                    Log.e("FileAdapter", "Failed to rename file: ${oldFileItem.fileName}")
                    Toast.makeText(context, "Failed to rename file", Toast.LENGTH_SHORT).show()
                }
            } else {
                Log.e("FileAdapter", "New file name is empty")
                Toast.makeText(context, "Please provide a valid file name", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.setNegativeButton("Cancel", null)

        dialog.show()
    }

    private fun deleteFile(position: Int) {
        if (position != RecyclerView.NO_POSITION && position < fileList.size) {
            val fileItem = fileList[position]
            val file = File(fileItem.filePath)

            if (file.exists()) {
                try {
                    if (file.delete()) {
                        // File deleted successfully
                        fileList.removeAt(position)
                        notifyItemRemoved(position)
                        notifyItemRangeChanged(position, fileList.size)
                    } else {
                        // Failed to delete file for some reason
                        Log.e("FileAdapter", "Failed to delete file: ${fileItem.fileName}")
                        Toast.makeText(context, "Failed to delete file", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    // Log any exceptions that occurred during file deletion
                    Log.e("FileAdapter", "Error deleting file: ${fileItem.fileName}", e)
                    Toast.makeText(context, "Failed to delete file", Toast.LENGTH_SHORT).show()
                }
            } else {
                // File does not exist at the specified path
                Toast.makeText(context, "File not found", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Invalid position or index out of bounds
            Toast.makeText(context, "Invalid position", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sharePdfFile(fileItem: FileItem) {
        val uri = FileProvider.getUriForFile(
            context,
            context.applicationContext.packageName + ".provider",
            File(fileItem.filePath) // Replace fileItem.filePath with the actual file path of the PDF
        )

        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "application/pdf"
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
        context.startActivity(Intent.createChooser(shareIntent, "Share PDF File"))
    }
    private fun shareVideoFile(fileItem: FileItem) {
        val uri = FileProvider.getUriForFile(
            context,
            context.applicationContext.packageName + ".provider",
            File(fileItem.filePath) // Replace fileItem.filePath with the actual file path of the video
        )

        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "video/*"
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
        context.startActivity(Intent.createChooser(shareIntent, "Share Video"))
    }
    private fun shareAudioFile(fileItem: FileItem) {
        val uri = FileProvider.getUriForFile(
            context,
            context.applicationContext.packageName + ".provider",
            File(fileItem.filePath) // Replace fileItem.filePath with the actual file path of the audio
        )

        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "audio/*"
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
        context.startActivity(Intent.createChooser(shareIntent, "Share Audio"))
    }
    private fun shareApkFile(fileItem: FileItem) {
        val uri = FileProvider.getUriForFile(
            context,
            context.applicationContext.packageName + ".provider",
            File(fileItem.filePath) // Replace fileItem.filePath with the actual file path of the APK
        )

        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "application/vnd.android.package-archive"
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
        context.startActivity(Intent.createChooser(shareIntent, "Share APK"))
    }
    private fun shareWebsiteFile(fileItem: FileItem) {
        // Assuming fileItem.filePath is the URL of the website/page
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_TEXT, fileItem.filePath)
        context.startActivity(Intent.createChooser(shareIntent, "Share Website"))
    }


}

