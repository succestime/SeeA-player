package com.jaidev.seeaplayer.dataClass

import android.graphics.Paint
import android.text.TextPaint
import android.text.style.CharacterStyle
import android.text.style.UpdateAppearance

class StrokeSpan(private val strokeColor: Int, private val strokeWidth: Float) : CharacterStyle(), UpdateAppearance {
    override fun updateDrawState(tp: TextPaint) {
        tp.style = Paint.Style.STROKE
        tp.strokeWidth = strokeWidth
        tp.color = strokeColor
    }
}
