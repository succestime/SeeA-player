package com.jaidev.seeaplayer.allAdapters

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import android.view.ActionMode
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.drawable.toDrawable
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.browseFregment.BrowseFragment
import com.jaidev.seeaplayer.browserActivity.BookmarkActivity
import com.jaidev.seeaplayer.browserActivity.LinkTubeActivity
import com.jaidev.seeaplayer.browserActivity.changeTab
import com.jaidev.seeaplayer.browserActivity.checkForInternet
import com.jaidev.seeaplayer.dataClass.Bookmark

class BookmarkAdapter(private val context: Context, private val isActivity: Boolean = false , private val callback: BookmarkAdapterCallback ,     private val bookmarkSaver: BookmarkSaver
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val colors = context.resources.getIntArray(R.array.myColors)
    private val selectedItems = mutableSetOf<Int>()
    private var isSelectionMode = false
    private var actionMode: ActionMode? = null
    private var originalBookmarkList = LinkTubeActivity.bookmarkList
    private var filteredBookmarkList = originalBookmarkList.toList()
    private var toastShown = false

    interface BookmarkAdapterCallback {
        fun onListEmpty(isEmpty: Boolean)
    }
    fun saveBookmarks() {
        Log.d("BookmarkAdapter", "Saving bookmarks: $originalBookmarkList")
        bookmarkSaver.saveBookmarks(originalBookmarkList)
    }
    interface BookmarkSaver {
        fun saveBookmarks(bookmarkList: ArrayList<Bookmark>)
    }

    inner class BookLongViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val image: TextView = itemView.findViewById(R.id.bookmarkIcon)
        private val name: TextView = itemView.findViewById(R.id.bookmarkName)
        private val urlBookmark: TextView = itemView.findViewById(R.id.UrlName)
        private val more: ImageButton = itemView.findViewById(R.id.moreBookmarkButton)
        private val selectBookmark: ImageButton = itemView.findViewById(R.id.selectBookmarkButton)

        @SuppressLint("NotifyDataSetChanged", "SetTextI18n", "ResourceType")
        fun bind(position: Int) {
            try {
                val bookmark = filteredBookmarkList[position]
                // Reset image text in case it was set in the exception block previously
                image.text = ""

                Log.d("BookmarkAdapter", "Binding position: $position, name: ${bookmark.name}")

                try {
                    val icon =
                        BitmapFactory.decodeByteArray(bookmark.image, 0, bookmark.image!!.size)
                    image.background = icon.toDrawable(context.resources)
                } catch (e: Exception) {
                    Log.e(
                        "BookmarkAdapter",
                        "Error loading icon for ${bookmark.name}: ${e.message}"
                    )
                    image.setBackgroundColor(colors.random())
                    image.text = bookmark.name[0].toString()

                }


                name.text = bookmark.name
                urlBookmark.text = bookmark.url

                if (isSelectionMode) {
                    more.setColorFilter(context.resources.getColor(R.color.gray))
                } else {
                    more.setColorFilter(null)
                }

                if (selectedItems.contains(adapterPosition)) {
                    more.visibility = View.GONE
                    selectBookmark.visibility = View.VISIBLE
                    themeBackground()
                } else {
                    more.visibility = View.VISIBLE
                    selectBookmark.visibility = View.GONE
                    itemView.setBackgroundResource(R.drawable.transparent)

                }

                itemView.setOnClickListener {
                    if (isSelectionMode) {
                        toggleSelection(adapterPosition)
                    } else {
                        if (checkForInternet(context)) {
                            changeTab(bookmark.name, BrowseFragment(urlNew = bookmark.url))
                            if (isActivity) (context as Activity).finish()
                        } else {
                            Snackbar.make(itemView, "Internet Not Connected\uD83D\uDE03", 3000)
                                .show()
                        }
                    }
                }

                itemView.setOnLongClickListener {
                    if (!isSelectionMode) {
                        startActionMode()
                    }
                    toggleSelection(adapterPosition)
                    true
                }

                more.setOnClickListener {
                    if (isSelectionMode) {
                        toggleSelection(adapterPosition)
                    } else {
                        showPopupWindow(it, adapterPosition)
                    }
                }

                selectBookmark.setOnClickListener {
                    toggleSelection(adapterPosition)
                }
            } catch (e: Exception) {
                showErrorToast()
            }
        }
        private fun themeBackground(){
            val isDarkMode = when (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) {
                android.content.res.Configuration.UI_MODE_NIGHT_YES -> true
                else -> false
            }
            if (isDarkMode) {
                itemView.setBackgroundResource(R.drawable.selectable_item_background_dark)

            } else{
                itemView.setBackgroundResource(R.drawable.selectable_item_background)

            }
        }
        private fun startActionMode() {
            actionMode = (context as AppCompatActivity).startActionMode(actionModeCallback)
            updateActionModeTitle()

            actionMode?.invalidate()
        }

        @SuppressLint("NotifyDataSetChanged")
        private fun toggleSelection(position: Int) {
            if (selectedItems.contains(position)) {
                selectedItems.remove(position)
                itemView.isSelected = false
            } else {
                selectedItems.add(position)
                itemView.isSelected = true
            }
            if (selectedItems.isEmpty()) {
                actionMode?.finish()
            } else {
                updateActionModeTitle()
                actionMode?.invalidate()
            }
            notifyDataSetChanged()
            // Reset the toastShown flag when the selection changes
            toastShown = true
        }

        @SuppressLint("ObsoleteSdkInt", "NotifyDataSetChanged")
        private fun showPopupWindow(anchorView: View, position: Int) {
            val layoutInflater = LayoutInflater.from(context)
            val popupView: View = layoutInflater.inflate(R.layout.bookmark_menu_long, null)
            val popupWindow = PopupWindow(
                popupView,
                150.dpToPx(context),
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

            val xOffset = 40.dpToPx(context)
            val yOffset = 35.dpToPx(context)
            popupWindow.showAtLocation(
                anchorView,
                Gravity.NO_GRAVITY,
                location[0] - popupWindow.width + xOffset,
                location[1] - popupWindow.height + yOffset
            )

            val selectTextView: TextView = popupView.findViewById(R.id.selectTextView)
            val editTextView: TextView = popupView.findViewById(R.id.editTextView)
            val deleteTextView: TextView = popupView.findViewById(R.id.deleteTextView)
            val moveUpTextView: TextView = popupView.findViewById(R.id.moveUpTextView)
            val moveDownTextView: TextView = popupView.findViewById(R.id.moveDownTextView)

            val bookmark = filteredBookmarkList[position]

            // Check if the bookmark is one of the default bookmarks
            if (bookmark.name == "Wikipedia - Default" || bookmark.name == "Google - Default" || bookmark.name == "YouTube - Default") {
                // Set the text color to gray
                editTextView.setTextColor(context.resources.getColor(R.color.gray__))
                deleteTextView.setTextColor(context.resources.getColor(R.color.gray__))

                // Disable the click listeners
                editTextView.isClickable = false
                deleteTextView.isClickable = false
            } else {
                // Set the click listeners for non-default bookmarks
                editTextView.setOnClickListener {
                    showEditBookmarkDialog(position)
                    popupWindow.dismiss()
                }

                deleteTextView.setOnClickListener {
                    // Remove the bookmark from the list
                    val bookmarkToDelete = filteredBookmarkList[position]
                    originalBookmarkList.remove(bookmarkToDelete)
                    filteredBookmarkList = originalBookmarkList.toList()
                    notifyDataSetChanged()

                    // Save the updated bookmark list
                    saveBookmarks()

                    popupWindow.dismiss()
                    callback.onListEmpty(filteredBookmarkList.isEmpty())
                }
            }

            if (position == 0) {
                moveUpTextView.visibility = View.GONE
            }

            if (position == filteredBookmarkList.size - 1) {
                moveDownTextView.visibility = View.GONE
            }

            selectTextView.setOnClickListener {
                toggleSelection(adapterPosition)
                startActionMode()
                popupWindow.dismiss()
            }

            moveUpTextView.setOnClickListener {
                if (position > 0) {
                    moveBookmark(position, position - 1)
                    popupWindow.dismiss()
                }
            }

            moveDownTextView.setOnClickListener {
                if (position < filteredBookmarkList.size - 1) {
                    moveBookmark(position, position + 1)
                    popupWindow.dismiss()
                }
            }
        }
    }


        private fun moveBookmark(fromPosition: Int, toPosition: Int) {
        val bookmarkToMove = filteredBookmarkList[fromPosition]
        val newList = filteredBookmarkList.toMutableList()
        newList.removeAt(fromPosition)
        newList.add(toPosition, bookmarkToMove)

        filteredBookmarkList = newList
        originalBookmarkList = ArrayList(newList)
        LinkTubeActivity.bookmarkList = originalBookmarkList // Ensure the main bookmark list is updated

        notifyItemMoved(fromPosition, toPosition)
        saveBookmarks()
    }
    private fun updateActionModeTitle() {
        actionMode?.title = "${selectedItems.size} / ${filteredBookmarkList.size} Selected"
    }
    private val actionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            mode?.menuInflater?.inflate(R.menu.multiple_bookmark_select_menu, menu)
            isSelectionMode = true
            toastShown = false

            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            updateActionModeTitle()
            val hasRestrictedBookmarks = selectedItems.any { position ->
                val bookmark = filteredBookmarkList[position]
                bookmark.name == "Wikipedia - Default" || bookmark.name == "Google - Default" || bookmark.name == "YouTube - Default"
            }

            val editItem = menu?.findItem(R.id.editBookmark)
            val deleteItem = menu?.findItem(R.id.deleteBookmark)
            editItem?.isVisible = selectedItems.size == 1

            if (hasRestrictedBookmarks) {
                editItem?.isEnabled = false
                deleteItem?.isEnabled = false
                if (!toastShown) {
                    Toast.makeText(context, "Default bookmark can't be edit or delete", Toast.LENGTH_SHORT).show()
                    toastShown = true
                }
                val isDarkMode = when (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) {
                    android.content.res.Configuration.UI_MODE_NIGHT_YES -> true
                    else -> false
                }
                if (isDarkMode) {
                    editItem?.icon?.setTint(context.resources.getColor(R.color.gray))
                    deleteItem?.icon?.setTint(context.resources.getColor(R.color.gray))
                } else{
                    editItem?.icon?.setTint(context.resources.getColor(R.color.black))
                    deleteItem?.icon?.setTint(context.resources.getColor(R.color.black))
                }
            } else {
                editItem?.isEnabled = true
                deleteItem?.isEnabled = true
                editItem?.icon?.setTintList(null)
                deleteItem?.icon?.setTintList(null)
            }

            return true
        }

        @SuppressLint("NotifyDataSetChanged")
        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            if (item?.isEnabled == false) return false

            return when (item?.itemId) {
                R.id.editBookmark -> {
                    showEditBookmarkDialog()

                    true
                }
                R.id.deleteBookmark -> {
                    // Remove the selected bookmarks
                    val bookmarksToDelete = selectedItems.map { filteredBookmarkList[it] }
                    originalBookmarkList.removeAll(bookmarksToDelete)
                    filteredBookmarkList = originalBookmarkList.toList()

                    // Save the updated bookmark list
                    bookmarkSaver.saveBookmarks(originalBookmarkList)

                    // Notify the adapter about the changes
                    notifyDataSetChanged()

                    // Check if the list is empty and notify the callback
                    callback.onListEmpty(filteredBookmarkList.isEmpty())

                    // Finish the action mode
                    mode?.finish()
                    true
                }
                R.id.moreBookmark -> {
                    showMoreOptionsMenu()
                    true
                }
                else -> false
            }
        }

        @SuppressLint("NotifyDataSetChanged")
        override fun onDestroyActionMode(mode: ActionMode?) {
            isSelectionMode = false
            selectedItems.clear()
            notifyDataSetChanged()
            actionMode = null
        }
    }
    @SuppressLint("NotifyDataSetChanged")
    private fun showEditBookmarkDialog(position: Int) {
        // Inflate the dialog with the custom layout
        val dialogView = LayoutInflater.from(context).inflate(R.layout.bookmark_actionabr_edit_dialog, null)

        // Initialize the views in the dialog
        val nameInputLayout = dialogView.findViewById<TextInputLayout>(R.id.nameEditText)
        val urlInputLayout = dialogView.findViewById<TextInputLayout>(R.id.urlEditText)
        val bookmarkNameEditText = nameInputLayout.editText as TextInputEditText
        val bookmarkUrlEditText = urlInputLayout.editText as TextInputEditText
        val backButton = dialogView.findViewById<ImageButton>(R.id.backButton)
        val delete2Button = dialogView.findViewById<ImageButton>(R.id.deleteButton)
        val doneButton = dialogView.findViewById<Button>(R.id.doneButton)

        // Populate the EditText fields with the selected bookmark details
        val selectedBookmark = filteredBookmarkList[position]
        bookmarkNameEditText.setText(selectedBookmark.name)
        bookmarkUrlEditText.setText(selectedBookmark.url)

        // Create and show the AlertDialog
        val alertDialog = AlertDialog.Builder(context).apply {
            setView(dialogView)
            create()
        }.show()

        // Set click listeners for dialog buttons
        backButton.setOnClickListener {
            alertDialog.dismiss()
        }

        delete2Button.setOnClickListener {
            // Handle delete action
            val bookmarkToDelete = filteredBookmarkList[position]
            originalBookmarkList.remove(bookmarkToDelete)
            filteredBookmarkList = originalBookmarkList.toList()
            notifyDataSetChanged()

            // Save the updated bookmark list
            bookmarkSaver.saveBookmarks(originalBookmarkList)

            // Dismiss the dialog
            alertDialog.dismiss()

            // Notify the callback if the list is empty
            callback.onListEmpty(filteredBookmarkList.isEmpty())

        }

        doneButton.setOnClickListener {
            // Save the changes made by the user
            val newName = bookmarkNameEditText.text.toString()
            val newUrl = bookmarkUrlEditText.text.toString()

            if (newName.isNotEmpty() && newUrl.isNotEmpty()) {
                selectedBookmark.name = newName
                selectedBookmark.url = newUrl
                notifyItemChanged(position)

                // Save updated bookmarks using the provided BookmarkSaver
                LinkTubeActivity.bookmarkList = filteredBookmarkList.toMutableList() as ArrayList<Bookmark>
                bookmarkSaver.saveBookmarks(LinkTubeActivity.bookmarkList)

                alertDialog.dismiss()
            } else {
                Toast.makeText(context, "Name or URL cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun showEditBookmarkDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.bookmark_actionabr_edit_dialog, null)
        val nameInputLayout = dialogView.findViewById<TextInputLayout>(R.id.nameEditText)
        val urlInputLayout = dialogView.findViewById<TextInputLayout>(R.id.urlEditText)
        val bookmarkNameEditText = nameInputLayout.editText as TextInputEditText
        val bookmarkUrlEditText = urlInputLayout.editText as TextInputEditText
        val backButton = dialogView.findViewById<ImageButton>(R.id.backButton)
        val deleteButton = dialogView.findViewById<ImageButton>(R.id.deleteButton)
        val doneButton = dialogView.findViewById<Button>(R.id.doneButton)

        // Assuming only one item is selected for editing
        val selectedBookmark = filteredBookmarkList[selectedItems.first()]
        bookmarkNameEditText.setText(selectedBookmark.name)
        bookmarkUrlEditText.setText(selectedBookmark.url)

        val alertDialog = AlertDialog.Builder(context).apply {
            setView(dialogView)
            create()
        }.show()

        backButton.setOnClickListener {
            alertDialog.dismiss()
        }

        deleteButton.setOnClickListener {
            // Remove the bookmark from the list
            originalBookmarkList.remove(selectedBookmark)
            filteredBookmarkList = originalBookmarkList.toList()
            notifyDataSetChanged()

            // Save the updated bookmark list
            bookmarkSaver.saveBookmarks(originalBookmarkList)

            alertDialog.dismiss()
            actionMode?.finish()
            callback.onListEmpty(filteredBookmarkList.isEmpty())
        }
        doneButton.setOnClickListener {
            // Save the changes made by the user
            val newName = bookmarkNameEditText.text.toString()
            val newUrl = bookmarkUrlEditText.text.toString()

            if (newName.isNotEmpty() && newUrl.isNotEmpty()) {
                selectedBookmark.name = newName
                selectedBookmark.url = newUrl
                notifyItemChanged(selectedItems.first())

                // Save updated bookmarks using the provided BookmarkSaver
                LinkTubeActivity.bookmarkList = filteredBookmarkList.toMutableList() as ArrayList<Bookmark>
                bookmarkSaver.saveBookmarks(LinkTubeActivity.bookmarkList)

                alertDialog.dismiss()

            } else {
                Toast.makeText(context, "Name or URL cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }
    }


    @SuppressLint("ObsoleteSdkInt", "NotifyDataSetChanged")
    private fun showMoreOptionsMenu() {
        val inflater = LayoutInflater.from(context)
        val popupView = inflater.inflate(R.layout.bookmark_actionbar_menu_long, null)

        // Create the PopupWindow
        val popupWindow = PopupWindow(popupView
            ,  200.dpToPx(context), ViewGroup.LayoutParams.WRAP_CONTENT, true)

        // Set the elevation for the PopupWindow
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            popupWindow.elevation = 10.0F
        }

        // Find the TextView in the popup window layout and set click listener
        val openInTab = popupView.findViewById<TextView>(R.id.openInTab)
        openInTab.setOnClickListener {
            if (checkForInternet(context)) {
            openSelectedBookmarksInNewTab()
            popupWindow.dismiss()

            } else {
                val rootView = (context as AppCompatActivity).findViewById<View>(android.R.id.content)
                Snackbar.make(rootView, "Internet Not Connected\uD83D\uDE03", Snackbar.LENGTH_LONG).show()
            }
        }


        // Show the PopupWindow
        val moreBookmarkButton = (context as AppCompatActivity).findViewById<View>(R.id.moreBookmark)
        moreBookmarkButton.post {
            val location = IntArray(2)
            moreBookmarkButton.getLocationOnScreen(location)
            val xOffset = -moreBookmarkButton.width - popupWindow.width / 1.8
            val yOffset = -moreBookmarkButton.height - popupWindow.height / 2
            popupWindow.showAtLocation(moreBookmarkButton, Gravity.NO_GRAVITY,
                ((location[0] + xOffset).toInt()), location[1] + yOffset)
        }
    }
    @SuppressLint("NotifyDataSetChanged")
    private fun openSelectedBookmarksInNewTab() {
        Log.d("BookmarkAdapter", "Opening ${selectedItems.size} bookmarks in new tabs.")

        selectedItems.forEach { position ->
            val bookmark = filteredBookmarkList[position]
            Log.d("BookmarkAdapter", "Opening bookmark: ${bookmark.name} with URL: ${bookmark.url}")

            if (checkForInternet(context)) {
                changeTab(bookmark.name, BrowseFragment(urlNew = bookmark.url))
            } else {
                Snackbar.make(
                    (context as Activity).findViewById(android.R.id.content),
                    "Internet Not Connected\uD83D\uDE03",
                    3000
                ).show()
            }
        }

        // Clear the selection after opening the bookmarks
        selectedItems.clear()
        notifyDataSetChanged()

        // Close the activity if required
        (context as? BookmarkActivity)?.finish()
    }


    inner class BookShortViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val image: TextView = itemView.findViewById(R.id.bookmarkIcon)
        private val name: TextView = itemView.findViewById(R.id.bookmarkName)

        @SuppressLint("NotifyDataSetChanged", "SuspiciousIndentation", "SetTextI18n")
        fun bind(position: Int) {
            try {
                val bookmark = filteredBookmarkList[position]
                // Reset image text in case it was set in the exception block previously
                image.text = ""

                Log.d("BookmarkAdapter", "Binding position: $position, name: ${bookmark.name}")

                try {
                    val icon = BitmapFactory.decodeByteArray(bookmark.image, 0, bookmark.image!!.size)
                    image.background = icon.toDrawable(context.resources)
                } catch (e: Exception) {
                    image.setBackgroundColor(colors.random())
                    image.text = bookmark.name[0].toString()

                }
                // Save the updated bookmark list
                saveBookmarks()
                name.text = bookmark.name

                itemView.setOnClickListener {
                    if (checkForInternet(context)) {
                        changeTab(bookmark.name, BrowseFragment(urlNew = bookmark.url))
                        if (isActivity) (context as Activity).finish()
                    } else {
                        Snackbar.make(itemView, "Internet Not Connected\uD83D\uDE03", 3000).show()
                    }
                }
            } catch (e: Exception) {
                showErrorToast()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (isActivity) {
            val longBookmarkView = inflater.inflate(R.layout.long_bookmark_view, parent, false)
            BookLongViewHolder(longBookmarkView)
        } else {
            val shortBookmarkView = inflater.inflate(R.layout.bookmark_view, parent, false)
            BookShortViewHolder(shortBookmarkView)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (isActivity) {
            (holder as BookLongViewHolder).bind(position)
        } else {
            (holder as BookShortViewHolder).bind(position)
        }
    }

    private fun showErrorToast() {
        Toast.makeText(context, "Error occurred, restart the app", Toast.LENGTH_LONG).show()
    }

    private fun Int.dpToPx(context: Context): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }

    override fun getItemCount(): Int {
        return filteredBookmarkList.size
    }
    private fun reloadBrowserFragment(fragment: BrowseFragment) {
        fragment.binding.webView.reload()
        fragment.binding.swipeRefreshBrowser.isRefreshing = false
    }


    @SuppressLint("NotifyDataSetChanged")
    fun filterBookmarks(query: String) {
        filteredBookmarkList = if (query.isEmpty()) {
            originalBookmarkList
        } else {
            originalBookmarkList.filter {
                it.name.contains(query, ignoreCase = true) || it.url.contains(query, ignoreCase = true)
            }
        }
        notifyDataSetChanged()
        callback.onListEmpty(filteredBookmarkList.isEmpty())

    }
}
