
package com.jaidev.seeaplayer

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
import android.os.Handler
import android.print.PrintAttributes
import android.print.PrintJob
import android.print.PrintManager
import android.speech.RecognizerIntent
import android.view.View
import android.view.WindowManager
import android.webkit.WebView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.app.ShareCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textview.MaterialTextView
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.jaidev.seeaplayer.LinkTubeActivity.Companion.myPager
import com.jaidev.seeaplayer.LinkTubeActivity.Companion.tabsBtn
import com.jaidev.seeaplayer.browseFregment.BrowseFragment
import com.jaidev.seeaplayer.browseFregment.HomeFragment
import com.jaidev.seeaplayer.dataClass.Bookmark
import com.jaidev.seeaplayer.dataClass.Tab
import com.jaidev.seeaplayer.dataClass.exitApplication
import com.jaidev.seeaplayer.databinding.ActivityLinkTubeBinding
import com.jaidev.seeaplayer.databinding.BookmarkDialogBinding
import com.jaidev.seeaplayer.databinding.TabViewBinding
import java.io.ByteArrayOutputStream
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class LinkTubeActivity : AppCompatActivity() {
    lateinit var binding: ActivityLinkTubeBinding
    private var printJob : PrintJob? = null
    // Assuming 'this' is a valid Context from an Activity or Fragment
    private lateinit var dbHandler: MydbHandler


    companion object {
        var tabsList: ArrayList<Tab> = ArrayList()
        private var isFullscreen: Boolean = true
        var isDesktopSite: Boolean = false
        var bookmarkList: ArrayList<Bookmark> = ArrayList()
        var bookmarkIndex : Int = -1
        lateinit var myPager : ViewPager2
        lateinit var tabsBtn : MaterialTextView
        const val REQUEST_CODE_SPEECH_INPUT = 2000



    }

    @SuppressLint("ObsoleteSdkInt")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setTheme(More.themesList[More.themeIndex])
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        binding = ActivityLinkTubeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()


        dbHandler = MydbHandler(this, null, null, 1)


        getAllBookmarks()
        tabsList.add(Tab("Home",HomeFragment(), LinkTubeActivity()))
        binding.myPager.adapter = TabsAdapter(supportFragmentManager, lifecycle)
        binding.myPager.isUserInputEnabled = false
        myPager = binding.myPager
        tabsBtn = binding.tabBtn
        initializeView()
        changeFullscreen(enable = true)
        binding.googleMicBtn.setOnClickListener {
            speak()
        }

        binding.moreBtn.setOnClickListener {
            val popupMenu = PopupMenu(this@LinkTubeActivity, binding.moreBtn)
            popupMenu.menuInflater.inflate(R.menu.browser_menu, popupMenu.menu)


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
                    R.id.newTab -> {
                        changeTab("Home", HomeFragment())
                    }

                    R.id.history -> {

                        startActivity(Intent(this, History::class.java))
                    }

                    R.id.download -> {
                      startActivity(Intent(this, FileActivity::class.java))
                    }

                    R.id.save -> {
                    save()
                    }

                    R.id.bookmark -> {
                        bookMark()
                    }

//                    R.id.recantTabs -> {
//
//                    }

                    R.id.desktop -> {
                        desktopMade()
                    }
                    R.id.fullScreen -> {
                        fullScreen()
                    }

                    R.id.share -> {
                        share()
                    }

                    R.id.exit -> {
                        exit()
                    }
                }
                true
            }

            popupMenu.show()

        }
        binding.bottomMoreBrowser.setOnClickListener {
            val popupMenu = PopupMenu(this@LinkTubeActivity, binding.bottomMoreBrowser)
            popupMenu.menuInflater.inflate(R.menu.browser_menu, popupMenu.menu)

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
                    R.id.newTab -> {
                        changeTab("Home", HomeFragment())
                    }

                    R.id.history -> {
                        startActivity(Intent(this, History::class.java))
                    }

                    R.id.download      -> {
                        startActivity(Intent(this, FileActivity::class.java))
                    }
                  R.id.save -> {
                    save()
                    }
                    R.id.bookmark -> {
                        bookMark()
                    }
                    R.id.desktop -> {
                        desktopMade()
                    }
                    R.id.fullScreen -> {
                        fullScreen()
                    }
                    R.id.share -> {
                        share()

                    }
//
//                    R.id.setting -> {
//
//                    }
                    R.id.exit -> {
                        exit()
                    }
                }
                true
            }

            popupMenu.show()


        }

        binding.bookMarkBtn.setOnClickListener {
            bookMark()
        }


        val isTablet = resources.configuration.smallestScreenWidthDp >= 600

        // Show or hide the buttons based on the device type
        if (isTablet) {
           binding.backBrowserBtn.visibility = View.VISIBLE
           binding.forwardBrowserBtn.visibility = View.VISIBLE
        } else {
            binding.backBrowserBtn.visibility = View.GONE
         binding.forwardBrowserBtn.visibility = View.GONE
        }

    }

    private fun desktopMade(){
        var frag: BrowseFragment? = null
        try {
            frag = tabsList[binding.myPager.currentItem].fragment as BrowseFragment
        } catch (_: Exception) {
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

        }

    }

    private fun bookMark(){
        var frag: BrowseFragment? = null
        try {
            frag = tabsList[binding.myPager.currentItem].fragment as BrowseFragment
        } catch (_: Exception) {
        }

        frag?.let {
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

    private fun share(){
        var frag: BrowseFragment? = null
        try {
            frag = tabsList[binding.myPager.currentItem].fragment as BrowseFragment
        } catch (_: Exception) {
        }
        val message = Handler().obtainMessage()
        frag?.binding?.webView?.requestFocusNodeHref(message)
        val url = message.data.getString("url")

        if (url != null) {
            ShareCompat.IntentBuilder(this@LinkTubeActivity)
                .setChooserTitle("Share URL")
                .setType("text/plain")
                .setText(url)
                .startChooser()
        } else {
            Snackbar.make(
                binding.root,
                "No URL available to share",
                Snackbar.LENGTH_SHORT
            ).show()
        }

    }

    private fun exit(){
        val builder = MaterialAlertDialogBuilder(this)
        builder.setTitle("Exit")
            .setMessage("Do you want to exit the app?")
            .setPositiveButton("Yes") { _, _ ->
                exitApplication()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
        val customDialog = builder.create()
        customDialog.show()

        customDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.RED)
        customDialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            .setTextColor(Color.GREEN)

    }

    private fun save(){
        var frag: BrowseFragment? = null
        try {
            frag = tabsList[binding.myPager.currentItem].fragment as BrowseFragment
        } catch (_: Exception) {
        }
        if (frag != null)
            saveAsPdf(web = frag.binding.webView)
        else Snackbar.make(binding.root, "First Open A WebPage\uD83D\uDE03", 3000)
            .show()
    }

    //   Assuming 'webView' and 'dbHandler' are properly initialized within the class
    fun saveData() {
        var frag: BrowseFragment? = null
        try {
            frag = tabsList[binding.myPager.currentItem].fragment as BrowseFragment
        } catch (_: Exception) {
        }
        val webUrl =  frag?.binding?.webView?.url
        dbHandler.addUrl(webUrl)
    }

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
        val url = "https://search.brave.com/search?q=${Uri.encode(query)}"
        val browserFragment = BrowseFragment(urlNew = url)
        changeTab("Brave", browserFragment)
    }


    private fun handleSpeechInput(webView: WebView, speechInput: String?) {
        if (!speechInput.isNullOrEmpty()) {
            // Check if the input is a number or a string
            val searchQuery =
                if (speechInput.matches(Regex("-?\\d+(\\.\\d+)?"))) { // Check if input is a number
                    "https://search.brave.com/search?q=$speechInput"
                } else {
                    "https://search.brave.com/search?q=${Uri.encode(speechInput)}"
                }
            webView.loadUrl(searchQuery)
        }
    }


    @Deprecated("Deprecated in Java")
    @SuppressLint("NotifyDataSetChanged")
    override fun onBackPressed() {

        var frag: BrowseFragment? = null
        try {
            frag = tabsList[binding.myPager.currentItem] as BrowseFragment
        } catch (_: Exception) {
        }
        when {
            frag?.binding?.webView?.canGoBack() == true -> frag.binding.webView.goBack()
            binding.myPager.currentItem != 0 -> {
                tabsList.removeAt(binding.myPager.currentItem)
                binding.myPager.adapter!!.notifyDataSetChanged()
                binding.myPager.currentItem = tabsList.size - 1
                // saveData()
            }

            else -> super.onBackPressed()
        }

    }

    private inner class TabsAdapter(fa: FragmentManager, lc: Lifecycle) :
        FragmentStateAdapter(fa, lc) {
        override fun getItemCount(): Int = tabsList.size

        override fun createFragment(position: Int): Fragment = tabsList[position].fragment
    }



    private fun initializeView() {

        binding.homeBrowserBtn.setOnClickListener {
            changeTab("Home", HomeFragment())
        }
        binding.bottomHomeBrowser.setOnClickListener {
            changeTab("Home", HomeFragment())
        }
        binding.bottomMediaBrowser.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        binding.tabBtn.setOnClickListener {
            val viewTabs =
                layoutInflater.inflate(R.layout.tab_view, binding.root, false)
            val bindingTabs = TabViewBinding.bind(viewTabs)

            val dialogTabs = MaterialAlertDialogBuilder(this , R.style.roundCornerDialog).setView(viewTabs)
                .setTitle("Select Tab")
                .setPositiveButton("Home"){self, _ ->
                    changeTab("Home", HomeFragment())
                    self.dismiss()
                }
                .setNeutralButton("Google"){self, _ ->
                    changeTab("Google", BrowseFragment(urlNew = "www.google.com"))
                    self.dismiss()
                }
                .create()

            bindingTabs.tabsRV.setHasFixedSize(true)
            bindingTabs.tabsRV.layoutManager = LinearLayoutManager(this)
            bindingTabs.tabsRV.adapter = TabAdapter(this, dialogTabs)

            dialogTabs.show()

            val pBtn =   dialogTabs.getButton(AlertDialog.BUTTON_POSITIVE)
            val nBtn =   dialogTabs.getButton(AlertDialog.BUTTON_NEUTRAL)

            pBtn.isAllCaps = false
            nBtn.isAllCaps = false

            pBtn.setCompoundDrawablesWithIntrinsicBounds( ResourcesCompat.getDrawable(resources, R.drawable.home_browse, theme)
                , null, null, null)
            nBtn.setCompoundDrawablesWithIntrinsicBounds( ResourcesCompat.getDrawable(resources, R.drawable.plus_icon, theme)
                , null, null, null)



        }
    }

    override fun onResume() {
        super.onResume()
        printJob?.let {
            when{
                it.isCompleted -> Snackbar.make(binding.root, "Successful -> ${it.info.label}", 4000).show()
                it.isFailed -> Snackbar.make(binding.root, "Failed -> ${it.info.label}", 4000).show()
            }
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

    private fun changeFullscreen(enable: Boolean){
        if(enable){
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowInsetsControllerCompat(window, binding.root).let { controller ->
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }else{
            WindowCompat.setDecorFitsSystemWindows(window, true)
            WindowInsetsControllerCompat(window, binding.root).show(WindowInsetsCompat.Type.systemBars())
        }
    }

    fun isBookmarked(url: String): Int{
        bookmarkList.forEachIndexed { index, bookmark ->
            if(bookmark.url == url) return index
        }
        return -1
    }

    fun saveBookmarks(){
        //for storing bookmarks data using shared preferences
        val editor = getSharedPreferences("BOOKMARKS", MODE_PRIVATE).edit()

        val data = GsonBuilder().create().toJson(bookmarkList)
        editor.putString("bookmarkList", data)

        editor.apply()
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

    fun onPageStarted(url: String?) {

    }

    fun onPageFinished(url: String?) {

    }


}

@SuppressLint("NotifyDataSetChanged")
fun changeTab(url: String, fragment: Fragment , isBackground : Boolean = false) {
    LinkTubeActivity.tabsList.add(Tab(name = url,fragment = fragment , activity = Activity()))
    myPager.adapter?.notifyDataSetChanged()
    tabsBtn.text = LinkTubeActivity.tabsList.size.toString()
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
