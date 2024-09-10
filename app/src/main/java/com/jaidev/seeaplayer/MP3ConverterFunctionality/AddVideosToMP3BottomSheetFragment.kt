package com.jaidev.seeaplayer.MP3ConverterFunctionality

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.jaidev.seeaplayer.MainActivity
import com.jaidev.seeaplayer.allAdapters.MP3ConvertingAdapter.MP3MultipleConvertAdapter
import com.jaidev.seeaplayer.dataClass.ThemeHelper
import com.jaidev.seeaplayer.databinding.Mp3MusicSelectionBottomSheetBinding
import java.util.*

class AddVideosToMP3BottomSheetFragment : BottomSheetDialogFragment(), MP3MultipleConvertAdapter.OnSelectionChangeListener {

    private lateinit var _binding: Mp3MusicSelectionBottomSheetBinding
    private val binding get() = _binding
    private lateinit var adapter: MP3MultipleConvertAdapter
    private var playlistId: Long = 0L

    companion object {
        private const val ARG_PLAYLIST_ID = "playlist_id"

        fun newInstance(playlistId: Long): AddVideosToMP3BottomSheetFragment {
            val fragment = AddVideosToMP3BottomSheetFragment()
            val args = Bundle().apply {
                putLong(ARG_PLAYLIST_ID, playlistId)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val theme = ThemeHelper.getSavedTheme(requireContext())
        ThemeHelper.applyTheme(requireContext(), theme)
        arguments?.let {
            playlistId = it.getLong(ARG_PLAYLIST_ID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = Mp3MusicSelectionBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("UseCompatLoadingForDrawables", "SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize RecyclerView with LayoutManager
        binding.mp3SelectionVideoRV.layoutManager = LinearLayoutManager(context)
        binding.mp3SelectionVideoRV.setHasFixedSize(true)
        binding.mp3SelectionVideoRV.setItemViewCacheSize(15)

        adapter = MP3MultipleConvertAdapter(requireContext(), MainActivity.videoList)
        binding.mp3SelectionVideoRV.adapter = adapter
        adapter.setOnSelectionChangeListener(this@AddVideosToMP3BottomSheetFragment)

        binding.clearBottomSheet.setOnClickListener {
            dismiss()
        }

        // Set up SearchView to filter items in the adapter
        binding.searchViewSA.setOnQueryTextListener(object :
            androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                adapter.filter(newText ?: "")
                return true
            }
        })


    }

    override fun onSelectionChanged(count: Int) {
        updateTabBtnText(count)
    }

    @SuppressLint("SetTextI18n")
    private fun updateTabBtnText(count: Int) {
        binding.tabBtn.text = count.toString()  // Set the count on tabBtn
    }

    override fun onStart() {
        super.onStart()

        dialog?.let {
            val bottomSheet = dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.layoutParams?.height = ViewGroup.LayoutParams.MATCH_PARENT
            val behavior = BottomSheetBehavior.from(bottomSheet!!)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }


}
