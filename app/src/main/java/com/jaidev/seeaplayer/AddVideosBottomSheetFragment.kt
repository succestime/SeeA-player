package com.jaidev.seeaplayer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.jaidev.seeaplayer.allAdapters.VideoAdapter2
import com.jaidev.seeaplayer.databinding.PlaylsitVideoSelectionBottomSheetBinding

class AddVideosBottomSheetFragment : BottomSheetDialogFragment(), VideoAdapter2.OnSelectionChangeListener{

    private lateinit var _binding: PlaylsitVideoSelectionBottomSheetBinding
    private val binding get() = _binding
    private lateinit var adapter: VideoAdapter2

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = PlaylsitVideoSelectionBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.playlistSelectionVideoRV.layoutManager = LinearLayoutManager(context)
        binding.playlistSelectionVideoRV.setHasFixedSize(true)
        binding.playlistSelectionVideoRV.setItemViewCacheSize(15)
        adapter = VideoAdapter2(requireContext(), MainActivity.videoList)
        binding.playlistSelectionVideoRV.adapter = adapter
        adapter.setOnSelectionChangeListener(this)


        binding.clearBottomSheet.setOnClickListener {
            dismiss()
        }
        binding.addSelectedVideo.setOnClickListener {
            val selectedVideos = adapter.getSelectedVideos()
            (activity as? PlaylistVideoActivity)?.addSelectedVideos(selectedVideos)
            dismiss()
        }

        updateAddSelectedVideoButtonVisibility()

    }
    override fun onSelectionChanged(count: Int) {
        updateAddSelectedVideoButtonVisibility()
    }

    private fun updateAddSelectedVideoButtonVisibility() {
        if (adapter.getSelectedItemCount() > 0) {
            binding.addSelectedVideo.visibility = View.VISIBLE
        } else {
            binding.addSelectedVideo.visibility = View.GONE
        }
    }


}
