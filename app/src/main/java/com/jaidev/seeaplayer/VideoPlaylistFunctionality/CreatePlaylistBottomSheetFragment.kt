package com.jaidev.seeaplayer.VideoPlaylistFunctionality

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.dataClass.VideoPlaylistData.OnPlaylistCreatedListener
import com.jaidev.seeaplayer.databinding.VideoPlaylistBottomDialogBinding

class CreatePlaylistBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: VideoPlaylistBottomDialogBinding? = null
    private val binding get() = _binding!!
    private var listener: OnPlaylistCreatedListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnPlaylistCreatedListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnPlaylistCreatedListener")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = VideoPlaylistBottomDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.renameField.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (s.isNullOrEmpty()) {
                    binding.buttonCreatePlaylist.setBackgroundColor(ContextCompat.getColor(requireContext(),
                        R.color.button_background_default
                    ))
                } else {
                    binding.buttonCreatePlaylist.setBackgroundColor(ContextCompat.getColor(requireContext(),
                        R.color.cool_blue
                    ))
                }
            }
        })

        binding.buttonCreatePlaylist.setOnClickListener {
            val playlistName = binding.renameField.text.toString()
            if (playlistName.isNotEmpty()) {
                listener?.onPlaylistCreated(playlistName)
                dismiss()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
