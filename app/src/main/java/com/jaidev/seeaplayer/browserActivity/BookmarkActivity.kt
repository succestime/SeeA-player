package com.jaidev.seeaplayer.browserActivity

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.GsonBuilder
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.allAdapters.BookmarkAdapter
import com.jaidev.seeaplayer.dataClass.Bookmark
import com.jaidev.seeaplayer.dataClass.SharedPreferencesBookmarkSaver
import com.jaidev.seeaplayer.dataClass.ThemeHelper
import com.jaidev.seeaplayer.databinding.ActivityBookmarkBinding

class BookmarkActivity : AppCompatActivity(), BookmarkAdapter.BookmarkAdapterCallback, BookmarkAdapter.BookmarkSaver{
    private lateinit var allBookMarkLayout: ConstraintLayout
    private lateinit var binding: ActivityBookmarkBinding
    companion object {
        lateinit var bookmarkList: ArrayList<Bookmark>
    }
    private lateinit var bookmarkSaver: SharedPreferencesBookmarkSaver

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val theme = ThemeHelper.getSavedTheme(this)
        ThemeHelper.applyTheme(this,theme)
       binding = ActivityBookmarkBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "All Bookmarks"
        binding.rvBookmarks.setItemViewCacheSize(5)
        binding.rvBookmarks.hasFixedSize()
        binding.rvBookmarks.layoutManager = LinearLayoutManager(this)
        val adapter = BookmarkAdapter(
            context = this,
            isActivity = true,callback = this,
            this as BookmarkAdapter.BookmarkSaver
        )
        binding.rvBookmarks.adapter = adapter
        bookmarkSaver = SharedPreferencesBookmarkSaver(this)
        bookmarkList = bookmarkSaver.loadBookmarks()

        allBookMarkLayout = binding.allBookmarkLayout
        // Set the background color of SwipeRefreshLayout based on app theme
        setRelativeLayoutBackgroundColor()
// Initialize EditText and set TextWatcher
        // Initialize EditText and set TextWatcher
        val editTextSearch = binding.editTextSearch
        editTextSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val clearIcon = if (s.isNullOrEmpty()) null else ContextCompat.getDrawable(this@BookmarkActivity, R.drawable.clear_browser)
                editTextSearch.setCompoundDrawablesWithIntrinsicBounds(null, null, clearIcon, null)
                adapter.filterBookmarks(s.toString())
                // Call onListEmpty(true) if the text is empty
                if (s.isNullOrEmpty()) {
                    onListEmpty(true)
                }
            }

            override fun afterTextChanged(s: Editable?) {

            }
        })
        // Set initial drawable state
        editTextSearch.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)

        // Add a touch listener to handle the clear button click
        editTextSearch.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                if (event.rawX >= (editTextSearch.right - editTextSearch.compoundPaddingEnd)) {
                    editTextSearch.text.clear()
                    onListEmpty(true)
                    return@setOnTouchListener true
                }
            }
            return@setOnTouchListener false
        }
        // Add focus change listener to handle visibility of layouts
        editTextSearch.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                onListEmpty(true)
                supportActionBar?.title = "Search"
            } else {
                onListEmpty(false)
                supportActionBar?.title = "All Bookmarks"
            }
        }


    }

    override fun onBackPressed() {
        if (binding.editTextSearch.hasFocus()) {
            onListEmpty(false)
            binding.editTextSearch.text.clear()
            binding.editTextSearch.clearFocus()
        } else {
            super.onBackPressed()
        }
    }



    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    @SuppressLint("ObsoleteSdkInt")
    private fun setRelativeLayoutBackgroundColor() {
        val isDarkMode = when (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) {
            android.content.res.Configuration.UI_MODE_NIGHT_YES -> true
            else -> false
        }

        if (isDarkMode) {
            // Dark mode is enabled, set background color to #012030
            allBookMarkLayout.setBackgroundColor(resources.getColor(R.color.dark_cool_blue))
            window.navigationBarColor = ContextCompat.getColor(this, R.color.dark_cool_blue)

        } else {
            // Light mode is enabled, set background color to white
            allBookMarkLayout.setBackgroundColor(resources.getColor(android.R.color.white))
            window.navigationBarColor = ContextCompat.getColor(this, R.color.white)
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

        }
        val navigationBarDividerColor = ContextCompat.getColor(this, R.color.gray)

        // This sets the navigation bar divider color. API 28+ required.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.navigationBarDividerColor = navigationBarDividerColor
        }
    }


    override fun onListEmpty(isEmpty: Boolean) {
        binding.bookmarkEmptyStateLayout.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.rvBookmarks.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }



    override fun saveBookmarks(bookmarkList: ArrayList<Bookmark>) {
        val editor = getSharedPreferences("BOOKMARKS", MODE_PRIVATE).edit()
        val data = GsonBuilder().create().toJson(bookmarkList)
        editor.putString("bookmarkList", data)
        editor.apply()
        bookmarkSaver.saveBookmarks(bookmarkList)

    }


}