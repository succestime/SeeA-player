package com.jaidev.seeaplayer.browserActivity

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.print.PrintAttributes
import android.print.PrintJob
import android.print.PrintManager
import android.speech.RecognizerIntent
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.webkit.WebView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ShareCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textview.MaterialTextView
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.jaidev.seeaplayer.MainActivity
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.allAdapters.BookmarkAdapter
import com.jaidev.seeaplayer.allAdapters.TabAdapter
import com.jaidev.seeaplayer.allAdapters.TabQuickButtonAdapter
import com.jaidev.seeaplayer.browseFregment.BrowseFragment
import com.jaidev.seeaplayer.browseFregment.HomeFragment
import com.jaidev.seeaplayer.browserActivity.LinkTubeActivity.Companion.myPager
import com.jaidev.seeaplayer.browserActivity.LinkTubeActivity.Companion.tabs2Btn
import com.jaidev.seeaplayer.browserActivity.LinkTubeActivity.Companion.tabsBtn
import com.jaidev.seeaplayer.dataClass.Bookmark
import com.jaidev.seeaplayer.dataClass.SharedPreferencesBookmarkSaver
import com.jaidev.seeaplayer.dataClass.Tab
import com.jaidev.seeaplayer.databinding.ActivityLinkTubeBinding
import com.jaidev.seeaplayer.databinding.BookmarkDialogBinding
import com.jaidev.seeaplayer.databinding.ClearTabDialogLayoutBinding
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URL
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


class LinkTubeActivity : AppCompatActivity(), BookmarkAdapter.BookmarkSaver {


    private var printJob : PrintJob? = null
    lateinit var mAdView: AdView
    private var mInterstitialAd : InterstitialAd? = null
    @SuppressLint("StaticFieldLeak")
    lateinit var binding: ActivityLinkTubeBinding
    private lateinit var bookmarkSaver: SharedPreferencesBookmarkSaver

    companion object {
        var tabsList: ArrayList<Tab> = ArrayList()
        private var isFullscreen: Boolean = true
        var isDesktopSite: Boolean = false
        var bookmarkIndex : Int = -1
        var bookmarkList: ArrayList<Bookmark> = ArrayList()

        @SuppressLint("StaticFieldLeak")
        lateinit var adapter: TabQuickButtonAdapter

        lateinit var myPager : ViewPager2
        lateinit var tabsBtn : MaterialTextView
        lateinit var tabs2Btn : MaterialTextView
        const val REQUEST_CODE_SPEECH_INPUT = 2000
        // Flag to track if the Home tab has been added
        private var isHomeTabAdded: Boolean = false
    }

    @SuppressLint("ObsoleteSdkInt", "ClickableViewAccessibility", "InternalInsetResource",
        "DiscouragedApi"
    )
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                window.attributes.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES }
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            window.statusBarColor = Color.parseColor("#373636")
            window.navigationBarColor = Color.parseColor("#373636")

            binding = ActivityLinkTubeBinding.inflate(layoutInflater)
            setContentView(binding.root)

            supportActionBar?.hide()
            // Send broadcast to notify MainActivity that this activity is opened
            val intent = Intent("com.yourapp.LINK_TUBE_OPENED")
            sendBroadcast(intent)
            bookmarkSaver = SharedPreferencesBookmarkSaver(this)
            BookmarkActivity.bookmarkList = bookmarkSaver.loadBookmarks()
            initializeView()
            initializeBinding()
            getAllBookmarks()
            setSwipeRefreshBackgroundColor()
            changeFullscreen(enable = true)
            setupRecyclerView()
            MobileAds.initialize(this) {}
            mAdView = findViewById(R.id.adView)
            val adRequest = AdRequest.Builder().build()
            mAdView.loadAd(adRequest)
            mAdView.adListener = object : AdListener() {
                override fun onAdLoaded() {
                    binding.adsLayout.visibility = View.VISIBLE
                }
            }

            // Initially set the adsLayout visibility to GONE until the ad is loaded
            binding.adsLayout.visibility = View.GONE

            // Add the Home tab if it hasn't been added yet
            if (!isHomeTabAdded) {
                tabsList.add(Tab("Home", HomeFragment(), LinkTubeActivity(), null, null))
                isHomeTabAdded = true
            }

            binding.myPager.adapter = TabsAdapter(supportFragmentManager, lifecycle)
            binding.myPager.isUserInputEnabled = false
            myPager = binding.myPager
            tabsBtn = binding.tabBtn
            tabs2Btn = binding.tab2Btn

            ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
                val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                view.setPadding(systemBarsInsets.left, systemBarsInsets.top, systemBarsInsets.right, 0)
                insets
            }

            binding.bottomRightBrowser.setOnTouchListener { view, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        view.tag = Pair(view.x - event.rawX, view.y - event.rawY)
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val tag = view.tag as Pair<Float, Float>
                        val newX = event.rawX + tag.first
                        val newY = event.rawY + tag.second

                        // Get the screen width and height
                        val displayMetrics = resources.displayMetrics
                        val screenWidth = displayMetrics.widthPixels
                        val screenHeight = displayMetrics.heightPixels

                        // Get the system window insets (status bar, navigation bar, gesture insets)
                        val insets = ViewCompat.getRootWindowInsets(view)?.getInsets(WindowInsetsCompat.Type.systemBars()) ?: Insets.NONE

                        // Get the status bar, navigation bar, and gesture insets
                        val statusBarHeight = insets.top
                        val navigationBarHeight = insets.bottom

                        // Get the bounds of linearLayout6 if it's visible
                        val linearLayout6 = binding.linearLayout6
                        val isLinearLayout6Visible = linearLayout6.visibility == View.VISIBLE
                        val linearLayout6Location = IntArray(2)
                        if (isLinearLayout6Visible) {
                            linearLayout6.getLocationOnScreen(linearLayout6Location)
                        }
                        val linearLayout6Top = linearLayout6Location[1]
                        val linearLayout6Bottom = linearLayout6Top + linearLayout6.height

                        // Ensure the view does not go outside the screen boundaries, considering the insets and linearLayout6
                        val viewWidth = view.width
                        val viewHeight = view.height

                        val boundedX = when {
                            newX < 0 -> 0f
                            newX + viewWidth > screenWidth -> (screenWidth - viewWidth).toFloat()
                            else -> newX
                        }

                        val boundedY = when {
                            newY < statusBarHeight -> statusBarHeight.toFloat()
                            newY + viewHeight > (screenHeight - navigationBarHeight) -> (screenHeight - navigationBarHeight - viewHeight).toFloat()
                            isLinearLayout6Visible && newY + viewHeight > linearLayout6Top && newY < linearLayout6Bottom -> linearLayout6Top.toFloat() - viewHeight
                            else -> newY
                        }

                        view.animate()
                            .x(boundedX)
                            .y(boundedY)
                            .setDuration(0)
                            .start()
                        true
                    }
                    else -> false
                }
            }


        } catch (e: Exception) {
            showToast(this@LinkTubeActivity, "Something went wrong, try to refresh")
            e.printStackTrace()
        }
        adapter.updateTabs()  // Notify the adapter to update the tabs


    }
    @SuppressLint("NotifyDataSetChanged")
    private fun setupRecyclerView() {
        binding.tabQuickButton.setHasFixedSize(true)
        binding.tabQuickButton.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        adapter = TabQuickButtonAdapter(this )
        binding.tabQuickButton.adapter = adapter
        adapter.notifyDataSetChanged()

    }

    private fun initializeView() {

        binding.homeBrowserBtn.setOnClickListener {
            animateAndSwitchToFirstTab()
        }

        binding.tab2Btn.setOnClickListener {
            binding.linearLayout6.visibility = View.VISIBLE
            binding.tab2Btn.visibility = View.GONE
            binding.browserClear.visibility = View.VISIBLE
            adapter.updateTabs()  // Notify the adapter to update the tabs

        }

        binding.browserClear.setOnClickListener {
            binding.linearLayout6.visibility = View.GONE
            binding.tab2Btn.visibility = View.VISIBLE
            binding.browserClear.visibility = View.GONE
            adapter.updateTabs()  // Notify the adapter to update the tabs

        }

        binding.showTabActivity.setOnClickListener {
            showTabDialog()
        }
        binding.topDownloadBrowser.setOnClickListener {
            handleDownload()
        }
        binding.bottomMediaBrowser.setOnClickListener {

            navigateToMainActivity()
        }
        binding.addNewTab.setOnClickListener {
            changeTab("Home", HomeFragment())
        }

        binding.tabBtn.setOnClickListener {
            val intent = Intent(this, TabActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_top_right, 0)
        }


    }

    private fun animateAndSwitchToFirstTab() {
        if (myPager.currentItem != 0) {
            // Store initial position
            val initialX = myPager.x
            val initialY = myPager.y

            // Set initial position to top left outside the screen
            myPager.x = -myPager.width.toFloat()
            myPager.y = -myPager.height.toFloat()

            // Create the ObjectAnimator for x and y properties
            val animatorX = ObjectAnimator.ofFloat(myPager, "x", initialX)
            val animatorY = ObjectAnimator.ofFloat(myPager, "y", initialY)

            // Create AnimatorSet to play animations together
            val animatorSet = AnimatorSet()
            animatorSet.playTogether(animatorX, animatorY)
            animatorSet.interpolator = AccelerateDecelerateInterpolator()
            animatorSet.duration = 500 // Animation duration in milliseconds

            animatorSet.addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {
                    myPager.setCurrentItem(0, false) // Set without animation
                    // Check if the current item is set to 0
                    if (myPager.currentItem == 0) {
                        tabsList[myPager.currentItem].name = "Home"
                        binding.btnTextUrl.setText("")
                        binding.webIcon.setImageResource(R.drawable.search_licktube_icon)
                        adapter.updateTabs()
                    }
                }

                override fun onAnimationEnd(animation: Animator) {
                    // After the animation ends, switch to the first tab
                }

                override fun onAnimationCancel(animation: Animator) {
                    // No-op
                }

                override fun onAnimationRepeat(animation: Animator) {
                    // No-op
                }
            })

            // Start the animation
            animatorSet.start()
        } else {
            // If already on the first tab, no need to animate, just ensure it's set to 0
            myPager.currentItem = 0
        }
    }

    @SuppressLint("DiscouragedApi")
    private fun showTabDialog() {
        val viewTabs = layoutInflater.inflate(R.layout.clear_tab_dialog_layout, binding.root, false)
        val bindingTabs = ClearTabDialogLayoutBinding.bind(viewTabs)

        val dialogClear = viewTabs.findViewById<ImageButton>(R.id.clearDialog)


        val dialogTabs = MaterialAlertDialogBuilder(this)
            .setView(viewTabs)
            .create()


        // Set up the RecyclerView
        bindingTabs.tabsRV.setHasFixedSize(true)
        bindingTabs.tabsRV.layoutManager = GridLayoutManager(this , 2)
        bindingTabs.tabsRV.adapter = TabAdapter(this, dialogTabs , isLinktubeActivity = true)
        dialogTabs.show()

        dialogClear.setOnClickListener {
            dialogTabs.dismiss()
        }
        val tabView = viewTabs
        tabView.requestLayout()
        dialogTabs.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
    }



    private fun showToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    fun initializeBinding(){
        binding.googleMicBtn.setOnClickListener {
            speak()
        }

        binding.moreBtn.setOnClickListener {
            showPopupMenu(it)
        }
        binding.bottomLeftBrowser.setOnClickListener {
            slideOutLinearLayout()
        }
        binding.bottomRightBrowserButton.setOnClickListener {
            slideInLinearLayout()
        }

        binding.bookMarkBtn.setOnClickListener {
            bookMark()
        }

        binding.adsRemove.setOnClickListener {
            binding.adsLayout.visibility = View.GONE
        }
        val isTablet = resources.configuration.smallestScreenWidthDp >= 600

        // Show or hide the buttons based on the device type
        if (isTablet) {
            binding.backBrowserBtn.visibility = View.VISIBLE
            binding.forwardBrowserBtn.visibility = View.VISIBLE
            binding.topDownloadBrowser.visibility = View.VISIBLE
        } else {
            binding.backBrowserBtn.visibility = View.GONE
            binding.forwardBrowserBtn.visibility = View.GONE
            binding.topDownloadBrowser.visibility = View.GONE
        }

    }

    @SuppressLint("MissingInflatedId", "InflateParams", "ObsoleteSdkInt")
    private fun showPopupMenu(anchorView: View) {
        // Inflate the popup_menu layout
        val layoutInflater = LayoutInflater.from(this)
        val popupView: View = layoutInflater.inflate(R.layout.linktube_pop_window, null)

        // Create the PopupWindow
        val popupWindow = PopupWindow(popupView,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT)

        // Show the PopupWindow
        popupWindow.isFocusable = true
        popupWindow.update()
        // Set the elevation for the PopupWindow
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            popupWindow.elevation = 10.0F
        }
        // Calculate the offset
        val location = IntArray(2)
        anchorView.getLocationOnScreen(location)
        val offsetX = location[0]
        val offsetY = -(location[1] * 1.5).toInt()  // 1.5 times the location[1] value


        popupWindow.showAsDropDown(anchorView, offsetX, offsetY)

        // Find the menu items
        val historyItem = popupView.findViewById<LinearLayout>(R.id.history)
        val viewOneItem = popupView.findViewById<View>(R.id.menu1)
        val viewTwoItem = popupView.findViewById<View>(R.id.menu2)
        val viewThreeItem = popupView.findViewById<View>(R.id.menu3)
        val recentItem = popupView.findViewById<LinearLayout>(R.id.recantTab)
        val downloadItem = popupView.findViewById<LinearLayout>(R.id.download)
        val saveItem = popupView.findViewById<LinearLayout>(R.id.save)
        val bookmarkItem = popupView.findViewById<LinearLayout>(R.id.bookmark)
        val desktopItem = popupView.findViewById<LinearLayout>(R.id.desktop)
        val fullScreenItem = popupView.findViewById<LinearLayout>(R.id.fullScreen)
        val shareItem = popupView.findViewById<LinearLayout>(R.id.share)
        val exitItem = popupView.findViewById<LinearLayout>(R.id.exit)

        // List of menu items for animation
        val menuItems = listOf(historyItem,viewOneItem, downloadItem, bookmarkItem,recentItem ,viewTwoItem,saveItem,  desktopItem, shareItem, viewThreeItem, fullScreenItem,  exitItem)

        // Initially hide all menu items
        menuItems.forEach { it.visibility = View.INVISIBLE }

        // Animate the appearance of each menu item with a delay
        menuItems.forEachIndexed { index, item ->
            val delay = index * 50L // Delay in milliseconds
            Handler(Looper.getMainLooper()).postDelayed({
                item.visibility = View.VISIBLE
                item.alpha = 0f
                item.animate().alpha(1f).setDuration(100).start()
            }, delay)
        }
        val forwardMenu = popupView.findViewById<ImageView>(R.id.forwardMenu)
        val downloadMenu = popupView.findViewById<ImageView>(R.id.downloadMenu)
        val bookmarkMenu = popupView.findViewById<ImageView>(R.id.bookmarkMenu)
        val refreshMenu = popupView.findViewById<ImageView>(R.id.clearMenu)

        // Set initial translationX for animation
        forwardMenu.translationX = 200f
        downloadMenu.translationX = 200f
        bookmarkMenu.translationX = 200f
        refreshMenu.translationX = 200f

        // Animate the icons
        refreshMenu.animate().translationX(0f).setDuration(200).setStartDelay(0).start()
        downloadMenu.animate().translationX(0f).setDuration(200).setStartDelay(10).start()
        bookmarkMenu.animate().translationX(0f).setDuration(200).setStartDelay(20).start()
        forwardMenu.animate().translationX(0f).setDuration(200).setStartDelay(30).start()


        var frag: BrowseFragment? = null
        try {
            frag = tabsList[binding.myPager.currentItem].fragment as BrowseFragment
        } catch (_: Exception) {
        }

        if (frag?.binding?.webView?.canGoForward() == true) {
            // Set the color of forwardMenu to default if it can go forward
            forwardMenu.colorFilter = null
        } else {
            // Set the color of forwardMenu to gray if it cannot go forward
            forwardMenu.setColorFilter(Color.parseColor("#515151"))

        }

        // Check if the current webpage is bookmarked
        val currentUrl = frag?.binding?.webView?.url
        val isBookmarked = bookmarkList.any { it.url == currentUrl }

        // Update bookmark icon based on bookmark status
        bookmarkMenu.setImageResource(
            if (isBookmarked) R.drawable._star_bookmark_24 else R.drawable._star_bookmark_border_24
        )
        // Update bookmark icon based on current status
        updateBookmarkIcon(bookmarkMenu)
        forwardMenu.setOnClickListener {
            frag?.apply {
                if (binding.webView.canGoForward()) {
                    binding.webView.goForward()
                }
            }
            popupWindow.dismiss()
        }

        popupView.findViewById<ImageView>(R.id.downloadMenu).setOnClickListener {
            handleDownload()
            popupWindow.dismiss()
        }
        popupView.findViewById<ImageView>(R.id.bookmarkMenu).setOnClickListener {
            bookMark()
            popupWindow.dismiss()
        }
        popupView.findViewById<ImageView>(R.id.clearMenu).setOnClickListener {
            popupWindow.dismiss()
        }
        popupView.findViewById<LinearLayout>(R.id.newTab).setOnClickListener {
            changeTab("Home", HomeFragment())
            popupWindow.dismiss()
        }
        popupView.findViewById<LinearLayout>(R.id.recantTab).setOnClickListener {
            startActivity(Intent(this, RecantTabActivity::class.java))
            popupWindow.dismiss()
        }
        popupView.findViewById<LinearLayout>(R.id.history).setOnClickListener {
            startActivity(Intent(this, HistoryBrowser::class.java))
            popupWindow.dismiss()
        }
        popupView.findViewById<LinearLayout>(R.id.download).setOnClickListener {
            startActivity(Intent(this, FileActivity::class.java))
            popupWindow.dismiss()
        }
        popupView.findViewById<LinearLayout>(R.id.save).setOnClickListener {
            save()
            popupWindow.dismiss()
        }

        popupView.findViewById<LinearLayout>(R.id.bookmark).setOnClickListener {
            startActivity(Intent(this, BookmarkActivity::class.java))
            popupWindow.dismiss()
        }
        popupView.findViewById<LinearLayout>(R.id.desktop).setOnClickListener {
            desktopMade()
            popupWindow.dismiss()
        }
        popupView.findViewById<LinearLayout>(R.id.fullScreen).setOnClickListener {
            fullScreen()
            popupWindow.dismiss()
        }
        popupView.findViewById<LinearLayout>(R.id.share).setOnClickListener {
            share()
            popupWindow.dismiss()
        }
        popupView.findViewById<LinearLayout>(R.id.exit).setOnClickListener {
            exit()
            popupWindow.dismiss()
        }
    }

    @SuppressLint("Recycle")
    private fun slideOutLinearLayout() {
        val linearLayout5 = findViewById<LinearLayout>(R.id.linearLayout5)
        val bottomRightBrowser = findViewById<LinearLayout>(R.id.bottomRightBrowser)
        val screenWidth = resources.displayMetrics.widthPixels.toFloat()

        // Find the banner ad view
        val bannerAdView = findViewById<LinearLayout>(R.id.adsLayout)

        // Create the ObjectAnimator to animate the translationX property of linearLayout5
        val slideOutAnimator = ObjectAnimator.ofFloat(linearLayout5, "translationX", screenWidth)
        slideOutAnimator.duration = 500 // Duration in milliseconds

        // Create an AnimatorSet to play animations together
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(slideOutAnimator)

        // Start the animation
        animatorSet.start()

        // Set the visibility to gone after the animation and animate bottomRightBrowser
        slideOutAnimator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}

            override fun onAnimationEnd(animation: Animator) {
                linearLayout5.visibility = View.GONE
                // Set the banner ad visibility to GONE if linearLayout5 is GONE
                bannerAdView?.visibility = View.GONE
                // Now animate bottomRightBrowser to slide in from the right
                bottomRightBrowser.visibility = View.VISIBLE
                val slideInAnimator = ObjectAnimator.ofFloat(bottomRightBrowser, "translationX", screenWidth, 0f)
                slideInAnimator.duration = 500 // Duration in milliseconds
                slideInAnimator.start()
            }
            override fun onAnimationCancel(animation: Animator) {}

            override fun onAnimationRepeat(animation: Animator) {}
        })
    }
    private fun slideInLinearLayout() {
        val linearLayout5 = findViewById<LinearLayout>(R.id.linearLayout5)
        val bottomRightBrowser = findViewById<LinearLayout>(R.id.bottomRightBrowser)
        val screenWidth = resources.displayMetrics.widthPixels.toFloat()
        // Find the banner ad view
        val bannerAdView = findViewById<LinearLayout>(R.id.adsLayout)
        // Animate bottomRightBrowser to slide out
        val slideOutAnimatorBottom = ObjectAnimator.ofFloat(bottomRightBrowser, "translationX", 0f, screenWidth)
        slideOutAnimatorBottom.duration = 500 // Duration in milliseconds
        // Animate linearLayout5 to slide in
        val slideInAnimatorLinearLayout = ObjectAnimator.ofFloat(linearLayout5, "translationX", screenWidth, 0f)
        slideInAnimatorLinearLayout.duration = 500 // Duration in milliseconds
        // Create an AnimatorSet to play animations together
        val animatorSetReverse = AnimatorSet()
        animatorSetReverse.playTogether(slideOutAnimatorBottom, slideInAnimatorLinearLayout)
        // Start the reverse animation
        animatorSetReverse.start()
        // Set the visibility after the animation
        slideOutAnimatorBottom.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}

            override fun onAnimationEnd(animation: Animator) {
                bottomRightBrowser.visibility = View.GONE
                linearLayout5.visibility = View.VISIBLE
                mAdView.adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        bannerAdView?.visibility = View.VISIBLE
                    }
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        bannerAdView?.visibility = View.GONE
                    }
                }
            }

            override fun onAnimationCancel(animation: Animator) {}

            override fun onAnimationRepeat(animation: Animator) {}
        })
    }



    private fun desktopMade(){
        var frag: BrowseFragment? = null
        try {
            frag = tabsList[binding.myPager.currentItem].fragment as BrowseFragment
        } catch (_: Exception) {
        }
        if (frag?.binding?.webView?.url.isNullOrEmpty()) {
            Toast.makeText(this@LinkTubeActivity, "First Open A Webpage \uD83D\uDE03", Toast.LENGTH_SHORT).show()
            return
        }
        frag?.binding?.webView?.apply {
            val isTablet = resources.configuration.smallestScreenWidthDp >= 600

            if (isTablet) {
                // Tablet-specific desktop mode
                if (isDesktopSite) {
                    // Currently in desktop mode on tablet, switch to mobile/desktop mode
                    settings.userAgentString = null

                    isDesktopSite = false // Toggle desktop mode state
                    Toast.makeText(this@LinkTubeActivity, "Desktop Mode Disabled", Toast.LENGTH_LONG).show()
                } else {
                    // Currently in mobile/desktop mode on tablet, switch to tablet-specific desktop mode
                    settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4501.0 Safari/537.36 Edge/91.0.866.0"
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    evaluateJavascript("document.querySelector('meta[name=\"viewport\"]').setAttribute('content', 'width=1200px')", null)
                    isDesktopSite = true // Toggle desktop mode state
                    Toast.makeText(this@LinkTubeActivity, "Desktop Mode Enabled", Toast.LENGTH_LONG).show()
                }
            } else {
                // Mobile/desktop mode for non-tablet devices
                if (isDesktopSite) {
                    // Currently in desktop mode on mobile, switch to mobile/desktop mode
                    settings.userAgentString = null
                    isDesktopSite = false // Toggle desktop mode state
                    Toast.makeText(this@LinkTubeActivity, "Desktop Mode Disabled", Toast.LENGTH_LONG).show()
                } else {
                    // Currently in mobile mode on mobile, switch to desktop mode
                    settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4501.0 Safari/537.36 Edge/91.0.866.0"
                    settings.useWideViewPort = true
                    isDesktopSite = true // Toggle desktop mode state
                    Toast.makeText(this@LinkTubeActivity, "Desktop Mode Enabled", Toast.LENGTH_LONG).show()
                }

            }
            frag.binding.webView.reload()
            frag.binding.swipeRefreshBrowser.isRefreshing = false
        }

    }

    private fun bookMark(){
        var frag: BrowseFragment? = null
        try {
            frag = tabsList[binding.myPager.currentItem].fragment as BrowseFragment
        } catch (_: Exception) {
        }
        if (frag?.binding?.webView?.url.isNullOrEmpty()) {
            Toast.makeText(this@LinkTubeActivity, "First Open A Website \uD83D\uDE03", Toast.LENGTH_SHORT).show()
            return
        }
        frag?.let {
            val currentUrl = it.binding.webView.url!!
            val bookmarkIndex = bookmarkList.indexOfFirst { bookmark -> bookmark.url == currentUrl }

            if (bookmarkIndex == -1) {
                val viewB = layoutInflater.inflate(
                    R.layout.bookmark_dialog,
                    binding.root,
                    false
                )
                val bBinding = BookmarkDialogBinding.bind(viewB)

                val dialogB = MaterialAlertDialogBuilder(this@LinkTubeActivity)
                    .setTitle("Add Bookmark")
                    .setMessage("Url: ${it.binding.webView.url}")
                    .setPositiveButton("Add") { dialog, _ ->
                        try {
                            val array = ByteArrayOutputStream()
                            it.webIcon?.compress(
                                Bitmap.CompressFormat.PNG,
                                100,
                                array
                            )
                            bookmarkList.add(
                                Bookmark(
                                    name = bBinding.bookmarkTitle.text.toString(),
                                    url = it.binding.webView.url!!,
                                    image = array.toByteArray()
                                )
                            )
                            Toast.makeText(
                                this@LinkTubeActivity,
                                "Bookmark added successfully",
                                Toast.LENGTH_SHORT
                            ).show()
                        } catch (e: Exception) {
                            Toast.makeText(
                                this@LinkTubeActivity,
                                "Bookmark failed",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        dialog.dismiss()
                    }
                    .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
                    .setView(viewB)
                    .create()
                dialogB.show()
                bBinding.bookmarkTitle.setText(it.binding.webView.title)
            } else {

                val dialogB = MaterialAlertDialogBuilder(this@LinkTubeActivity)
                    .setTitle("Remove Bookmark")
                    .setMessage("Url: ${it.binding.webView.url}")
                    .setPositiveButton("Remove") { dialog, _ ->
                        bookmarkList.removeAt(bookmarkIndex)

                        Toast.makeText(
                            this@LinkTubeActivity,
                            "Bookmark removed successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                        dialog.dismiss()
                    }
                    .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
                    .create()
                dialogB.show()
            }

        }
    }

    private fun fullScreen(){
        isFullscreen = if (isFullscreen) {
            changeFullscreen(enable = false)
            false
        }
        else {
            changeFullscreen(enable = true)
            true
        }

    }

    private fun changeFullscreen(enable: Boolean){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if(enable){
                // Do not hide the status bar, only hide the navigation bar
                WindowCompat.setDecorFitsSystemWindows(window, false)
                WindowInsetsControllerCompat(window, binding.root).let { controller ->
                    controller.hide(WindowInsetsCompat.Type.navigationBars())
                    controller.show(WindowInsetsCompat.Type.statusBars())
                    controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }

            }else{
                // Show the navigation bar, status bar remains visible
                WindowCompat.setDecorFitsSystemWindows(window, true)
                WindowInsetsControllerCompat(window, binding.root).show(WindowInsetsCompat.Type.navigationBars())
                WindowInsetsControllerCompat(window, binding.root).show(WindowInsetsCompat.Type.statusBars())
            }
        } else {
            ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
                val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                view.setPadding(systemBarsInsets.left, systemBarsInsets.top, systemBarsInsets.right, 0)
                insets
            }
        }
        // Ensure the navigation bar color is set
        window.navigationBarColor = Color.parseColor("#373636")
    }

    private fun share() {
        var frag: BrowseFragment? = null
        try {
            frag = tabsList[myPager.currentItem].fragment as BrowseFragment
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val url = frag?.binding?.webView?.url

        if (!url.isNullOrEmpty()) {
            // If URL is present, share the URL
            ShareCompat.IntentBuilder(this@LinkTubeActivity)
                .setChooserTitle("Share URL")
                .setType("text/plain")
                .setText(url)
                .startChooser()
        } else {
            // If URL is not present, notify the user
            Toast.makeText(this@LinkTubeActivity, "URL not found", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("ResourceAsColor")
    private fun exit(){
        val builder = MaterialAlertDialogBuilder(this)
        builder.setTitle("Exit")
            .setMessage("Do you want to exit the app?")
            .setPositiveButton("Yes") { _, _ ->
                finishAffinity() // This will finish all activities in the task
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
        val customDialog = builder.create()
        customDialog.show()


    }

    @SuppressLint("SuspiciousIndentation")
    private fun save(){
        var frag: BrowseFragment? = null
        try {
            frag = tabsList[binding.myPager.currentItem].fragment as BrowseFragment
        } catch (_: Exception) {
        }
        if (frag != null)
            saveAsPdf(web = frag.binding.webView)
        else
            Toast.makeText(this@LinkTubeActivity, "First Open A WebPage \uD83D\uDE03", Toast.LENGTH_SHORT).show()

    }

//    private fun checkHistorySize() {
//
//        if (HistoryManager.getHistorySize(this) >= MAX_HISTORY_SIZE) {
//            val builder = MaterialAlertDialogBuilder(this)
//            builder.setTitle("Delete History Items")
//                .setMessage("The history has reached its maximum capacity. Delete some items?")
//                .setPositiveButton("Delete") { _, _ ->
//                    startActivity(Intent(this, HistoryBrowser::class.java))
//                    finish()
//                }
//            val customDialog = builder.create()
//            customDialog.show()
//        }
//    }

    fun speak() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Hi, speak something")

        try {
            startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT)
        } catch (e: Exception) {
            Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
        }
    }

    fun loadAd(){
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(this,"ca-app-pub-3940256099942544/1033173712", adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                mInterstitialAd = null
            }

            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                mInterstitialAd = interstitialAd
            }
        })

    }



    private fun navigateToMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE_SPEECH_INPUT -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    val speechInput = result?.get(0)
                    val currentFragment = tabsList[myPager.currentItem].fragment
                    val activity = tabsList[myPager.currentItem].activity
                    if (currentFragment is BrowseFragment) {
                        val webView = currentFragment.binding.webView
                        handleSpeechInput(webView, speechInput)
                    } else if (activity is LinkTubeActivity) {
                        activity.navigateToBrowserFragment(speechInput ?: "")
                    } else {
                        // Handle the case when the current fragment is not a BrowseFragment or LinkTubeActivity
                    }
                }
            }
        }
    }

    fun navigateToBrowserFragment(query: String) {
        val url = "https://www.google.com/search?q=${Uri.encode(query)}"
        val browserFragment = BrowseFragment(urlNew = url)
        changeTab("Brave", browserFragment)
    }


    private fun handleSpeechInput(webView: WebView, speechInput: String?) {
        if (!speechInput.isNullOrEmpty()) {
            // Check if the input is a number or a string
            val searchQuery =
                if (speechInput.matches(Regex("-?\\d+(\\.\\d+)?"))) { // Check if input is a number
                    "https://www.google.com/search?q=$speechInput"
                } else {
                    "https://www.google.com/search?q=${Uri.encode(speechInput)}"
                }
            webView.loadUrl(searchQuery)
        }
    }



    @Deprecated("Deprecated in Java")
    @SuppressLint("NotifyDataSetChanged")
    override fun onBackPressed() {
        var handled = false
        val currentFragment = tabsList.getOrNull(binding.myPager.currentItem)?.fragment
        if (currentFragment is BrowseFragment) {
            // Check if the current fragment is a BrowseFragment and can handle the back press
            handled = currentFragment.onBackPressed()
        }

        if (!handled) {
            // If back press is not handled by the current fragment, perform default behavior
            if (currentFragment is BrowseFragment && currentFragment.isLoading()) {
                // If the current fragment is a BrowseFragment and a page is loading, do nothing
            } else {
                when {
                    binding.myPager.currentItem != 0 -> {
                        tabsList.removeAt(binding.myPager.currentItem)
                        binding.myPager.adapter?.notifyDataSetChanged()
                        adapter.updateTabs()
                        TabAdapter.updateTabs()
                        if (myPager.currentItem > 0 ) {
                            myPager.currentItem = tabsList.size - 1
                        } else if (tabsList.isNotEmpty()) {
                            myPager.currentItem = 0
                        }
                    }
                    else -> super.onBackPressed()
                }
            }
        }
    }


    private  class TabsAdapter(fa: FragmentManager, lc: Lifecycle) :
        FragmentStateAdapter(fa, lc) {
        override fun getItemCount(): Int = tabsList.size

        override fun createFragment(position: Int): Fragment = tabsList[position].fragment


    }



    @SuppressLint("ObsoleteSdkInt")
    fun setSwipeRefreshBackgroundColor() {
        val isDarkMode = when (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) {
            android.content.res.Configuration.UI_MODE_NIGHT_YES -> true
            else -> false
        }

        if (isDarkMode) {
            // Dark mode is enabled, set background color to #012030
            window.navigationBarColor = ContextCompat.getColor(this, R.color.link_tube_tab_color)
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()


        } else {
            window.navigationBarColor = ContextCompat.getColor(this, R.color.link_tube_tab_color)
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()

        }
        val navigationBarDividerColor = ContextCompat.getColor(this, R.color.gray)

        // This sets the navigation bar divider color. API 28+ required.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.navigationBarDividerColor = navigationBarDividerColor
        }
    }



    private fun handleDownload() {
        var frag: BrowseFragment? = null
        try {
            frag = tabsList[myPager.currentItem].fragment as BrowseFragment
        } catch (e: Exception) {
            e.printStackTrace()
        }
        // Get the current URL from the WebView
        val currentUrl = frag?.binding?.webView?.url
        if (currentUrl != null) {
            downloadCurrentWebPage(currentUrl)
        } else {
            Toast.makeText(this@LinkTubeActivity, "No webpage to download", Toast.LENGTH_SHORT).show()
        }
    }

    private fun downloadCurrentWebPage(url: String) {
        var frag: BrowseFragment? = null
        try {
            frag = tabsList[myPager.currentItem].fragment as BrowseFragment
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val fileName = generateFileName(url)
        val destinationDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(destinationDir, fileName)

        // Save the webpage to the file
        frag?.binding?.webView?.saveWebArchive(file.absolutePath, false) { savedFile ->
            if (savedFile != null) {
                Toast.makeText(this, "Page saved as $fileName", Toast.LENGTH_SHORT).show()
                Log.d("Downloaded URL", url)
            } else {
                Toast.makeText(this, "Failed to save page", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun generateFileName(url: String): String {
        val searchQuery = extractSearchQuery(url)
        val decodedQuery = if (searchQuery != null) {
            URLDecoder.decode(searchQuery, "UTF-8")
        } else {
            val uri = Uri.parse(url)
            uri.host ?: "webpage"
        }
        val fileName = "$decodedQuery - SeeA LinkTube.mhtml"
        return fileName
    }

    private fun extractSearchQuery(url: String): String? {
        val queryIndex = url.indexOf("search?q=")
        return if (queryIndex != -1) {
            val startIndex = queryIndex + "search?q=".length
            val endIndex = findEndIndex(url, startIndex)
            if (endIndex != -1) {
                url.substring(startIndex, endIndex)
            } else {
                url.substring(startIndex)
            }
        } else {
            null
        }
    }

    private fun findEndIndex(url: String, startIndex: Int): Int {
        val endIndexChars = listOf("&", "#")
        for (char in endIndexChars) {
            val endIndex = url.indexOf(char, startIndex)
            if (endIndex != -1) {
                return endIndex
            }
        }
        return -1
    }




    override fun onResume() {
        super.onResume()
        try {
            mAdView = findViewById(R.id.adView)
            val adRequest = AdRequest.Builder().build()
            mAdView.loadAd(adRequest)
            mAdView.adListener = object : AdListener() {
                override fun onAdLoaded() {
                    binding.adsLayout.visibility = View.VISIBLE
                }
                override fun onAdFailedToLoad(p0: LoadAdError) {
                    // Initially set the adsLayout visibility to GONE until the ad is loaded
                    binding.adsLayout.visibility = View.GONE
                }
            }
            printJob?.let {
                when{
                    it.isCompleted -> Snackbar.make(binding.root, "Successful -> ${it.info.label}", 4000).show()
                    it.isFailed -> Snackbar.make(binding.root, "Failed -> ${it.info.label}", 4000).show()
                }
            }
        } catch (e: Exception) {
            showToast(this@LinkTubeActivity, "Something went wrong, try to refresh")
            e.printStackTrace()
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            mAdView = findViewById(R.id.adView)
            val adRequest = AdRequest.Builder().build()
            mAdView.loadAd(adRequest)
            mAdView.adListener = object : AdListener() {
                override fun onAdLoaded() {
                    binding.adsLayout.visibility = View.VISIBLE
                }
                override fun onAdFailedToLoad(p0: LoadAdError) {
                    // Initially set the adsLayout visibility to GONE until the ad is loaded
                    binding.adsLayout.visibility = View.GONE
                }
            }

        } catch (e: Exception) {
            showToast(this@LinkTubeActivity, "Something went wrong, try to refresh")
            e.printStackTrace()
        }
    }

    private fun saveAsPdf(web: WebView){
        val pm = getSystemService(Context.PRINT_SERVICE) as PrintManager
        val jobName = "${URL(web.url).host}_${SimpleDateFormat("HH:mm d_MMM_yy", Locale.ENGLISH)
            .format(Calendar.getInstance().time)}"
        val printAdapter = web.createPrintDocumentAdapter(jobName)
        val printAttributes = PrintAttributes.Builder()
        printJob = pm.print(jobName , printAdapter, printAttributes.build())

    }




    fun isBookmarked(url: String): Int{
        bookmarkList.forEachIndexed { index, bookmark ->
            if(bookmark.url == url) return index
        }
        return -1
    }

    fun saveBookmarks() {

        // Save bookmarks data using shared preferences
        val editor = getSharedPreferences("BOOKMARKS", MODE_PRIVATE).edit()
        val data = GsonBuilder().create().toJson(bookmarkList)
        editor.putString("bookmarkList", data)
        editor.apply()

    }
    private fun updateBookmarkIcon(bookmarkMenu: ImageView) {
        var frag: BrowseFragment? = null
        try {
            frag = tabsList[binding.myPager.currentItem].fragment as BrowseFragment
        } catch (_: Exception) {
        }

        val currentUrl = frag?.binding?.webView?.url
        val isBookmarked = bookmarkList.any { it.url == currentUrl }

        bookmarkMenu.setImageResource(
            if (isBookmarked) R.drawable._star_bookmark_24 else R.drawable._star_bookmark_border_24
        )
    }
    private fun getAllBookmarks(){
        //for getting bookmarks data using shared preferences from storage
        bookmarkList = ArrayList()
        val editor = getSharedPreferences("BOOKMARKS", MODE_PRIVATE)
        val data = editor.getString("bookmarkList", null)

        if(data != null){

            val list: ArrayList<Bookmark> = GsonBuilder().create().fromJson(data, object: TypeToken<ArrayList<Bookmark>>(){}.type)
            bookmarkList.addAll(list)
        }
    }

    override fun saveBookmarks(bookmarkList: ArrayList<Bookmark>) {
        val editor = getSharedPreferences("BOOKMARKS", MODE_PRIVATE).edit()
        val data = GsonBuilder().create().toJson(bookmarkList)
        editor.putString("bookmarkList", data)
        editor.apply()
    }
}

@SuppressLint("NotifyDataSetChanged")
fun changeTab(url: String, fragment: Fragment , isBackground : Boolean = false) {
    LinkTubeActivity.tabsList.add(Tab(name = url,fragment = fragment , activity = Activity() , null, null ))
    myPager.adapter?.notifyDataSetChanged()
    LinkTubeActivity.adapter.updateTabs()
    TabQuickButtonAdapter.updateTabs()
    TabAdapter.updateTabs()
    tabsBtn.text = LinkTubeActivity.tabsList.size.toString()
    tabs2Btn.text = LinkTubeActivity.tabsList.size.toString()
    if(!isBackground) myPager.currentItem = LinkTubeActivity.tabsList.size - 1

}



@SuppressLint("ObsoleteSdkInt")
fun checkForInternet(context: Context): Boolean {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            else -> false
        }
    } else {
        @Suppress("DEPRECATION") val networkInfo =
            connectivityManager.activeNetworkInfo ?: return false
        @Suppress("DEPRECATION")
        return networkInfo.isConnected
    }


}