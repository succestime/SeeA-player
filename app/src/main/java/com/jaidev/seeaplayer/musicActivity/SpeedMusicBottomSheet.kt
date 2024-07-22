package com.jaidev.seeaplayer.musicActivity

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.jaidev.seeaplayer.R

class SpeedMusicBottomSheet : BottomSheetDialogFragment() {

    private lateinit var textView1: TextView
    private lateinit var textView2: TextView
    private lateinit var textView3: TextView
    private lateinit var textView4: TextView
    private lateinit var textView5: TextView
    private lateinit var textView6: TextView
    private lateinit var previousClickedTextView: TextView

    private var speedSelectionListener: SpeedSelectionListener? = null
    private var currentSpeed: Float = 1.0f // Default speed

    companion object {
        private const val PREFS_NAME = "speed_preferences"
        private const val KEY_SELECTED_SPEED = "selected_speed"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_speed_music_bottom_sheet, container, false)

        // Initialize all text views
        textView1 = view.findViewById(R.id.textview1)
        textView2 = view.findViewById(R.id.textview2)
        textView3 = view.findViewById(R.id.textview3)
        textView4 = view.findViewById(R.id.textview4)
        textView5 = view.findViewById(R.id.textview5)
        textView6 = view.findViewById(R.id.textview6)

        // Load the saved speed
        loadSavedSpeed()

        // Set the initial previous clicked text view
        previousClickedTextView = when (currentSpeed) {
            0.5f -> textView1
            0.75f -> textView2
            1.0f -> textView3
            1.25f -> textView4
            1.5f -> textView5
            2.0f -> textView6
            else -> textView3
        }

        // Set click listeners for all text views
        textView1.setOnClickListener {
            handleTextViewClick(textView1)
            speedSelectionListener?.onSpeedSelected(0.5f)
            saveSelectedSpeed(0.5f)
            dismiss()
        }
        textView2.setOnClickListener {
            handleTextViewClick(textView2)
            speedSelectionListener?.onSpeedSelected(0.75f)
            saveSelectedSpeed(0.75f)
            dismiss()
        }
        textView3.setOnClickListener {
            handleTextViewClick(textView3)
            speedSelectionListener?.onSpeedSelected(1.0f)
            saveSelectedSpeed(1.0f)
            dismiss()
        }
        textView4.setOnClickListener {
            handleTextViewClick(textView4)
            speedSelectionListener?.onSpeedSelected(1.25f)
            saveSelectedSpeed(1.25f)
            dismiss()
        }
        textView5.setOnClickListener {
            handleTextViewClick(textView5)
            speedSelectionListener?.onSpeedSelected(1.5f)
            saveSelectedSpeed(1.5f)
            dismiss()
        }
        textView6.setOnClickListener {
            handleTextViewClick(textView6)
            speedSelectionListener?.onSpeedSelected(2.0f)
            saveSelectedSpeed(2.0f)
            dismiss()
        }

        // Set the correct TextView based on the current speed
        handleTextViewClick(previousClickedTextView)

        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is SpeedSelectionListener) {
            speedSelectionListener = context
        } else {
            throw RuntimeException("$context must implement SpeedSelectionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        speedSelectionListener = null
    }


    private fun handleTextViewClick(clickedTextView: TextView) {
        // Remove the drawableEnd and reset the text color of the previously clicked text view
        previousClickedTextView.setCompoundDrawablesWithIntrinsicBounds(
            null,
            null,
            null,
            null
        )
        previousClickedTextView.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))

        // Set the drawableEnd and change the text color for the clicked text view
        val drawable: Drawable? = ContextCompat.getDrawable(requireContext(),
            R.drawable.complete_svgrepo_com
        )
        clickedTextView.setCompoundDrawablesWithIntrinsicBounds(
            null, // left drawable
            null, // top drawable
            drawable, // right drawable
            null // bottom drawable
        )
        clickedTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.cool_blue))

        // Update the previous clicked text view
        previousClickedTextView = clickedTextView
    }

    private fun saveSelectedSpeed(speed: Float) {
        val sharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putFloat(KEY_SELECTED_SPEED, speed)
            apply()
        }
    }

    private fun loadSavedSpeed() {
        val sharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        currentSpeed = sharedPreferences.getFloat(KEY_SELECTED_SPEED, 1.0f)
    }

    fun setCurrentSpeed(speed: Float) {
        currentSpeed = speed
    }

    interface SpeedSelectionListener {
        fun onSpeedSelected(speed: Float)
    }
}
