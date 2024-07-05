package com.jaidev.seeaplayer.dataClass

import android.app.Activity
import android.graphics.Bitmap
import androidx.fragment.app.Fragment

data class Tab(var name: String, val fragment: Fragment, val activity: Activity,
               var icon: Bitmap? = null, var previewBitmap: Bitmap? = null , var url: String? = null) {


}
