package com.jaidev.seeaplayer

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import com.google.android.exoplayer2.ui.PlayerView

class CustomPlayerView(context: Context, attrs: AttributeSet? = null) :
    PlayerView(context, attrs) {

    private val borderPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 8f // Adjust as needed
        color = context.getColor(R.color.black) // Use your desired border color
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        // Prevent touch events from being passed through
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Draw a border around the PlayerView
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), borderPaint)
    }
}
