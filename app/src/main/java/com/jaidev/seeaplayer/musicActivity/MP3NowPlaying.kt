
package com.jaidev.seeaplayer.musicActivity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.jaidev.seeaplayer.MP3playerActivity
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.dataClass.setMP3SongPosition
import com.jaidev.seeaplayer.databinding.FragmentMP3NowPlayingBinding
import com.jaidev.seeaplayer.musicActivity.PlayerMusicActivity.Companion.isPlaying
import com.jaidev.seeaplayer.musicActivity.PlayerMusicActivity.Companion.songPosition

class MP3NowPlaying : Fragment() {

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var binding: FragmentMP3NowPlayingBinding

    }


    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ):
        View? {
        val view = inflater.inflate(R.layout.fragment_m_p3_now_playing, container, false)
        binding = FragmentMP3NowPlayingBinding.bind(view)
        binding.root.visibility = View.GONE


        binding.playPauseBtnNP.setOnClickListener {
            if(isPlaying) pauseMusic() else playMusic()
        }
      binding.nextBtnNP.setOnClickListener {
            setMP3SongPosition(increment = true)
            MP3playerActivity.musicMP3Service!!.createMediaPlayer()
            Glide.with(this)
                .asBitmap()
                .load(MP3playerActivity.mp3MusicPA[songPosition].artUri)
                .apply(RequestOptions().placeholder(R.drawable.music_speaker_three).centerCrop())
                .into(binding.songImgNP)
           binding.songNameNP.text = MP3playerActivity.mp3MusicPA[songPosition].title
          MP3playerActivity.musicMP3Service!!.showNotification(R.drawable.round_pause_circle_outline_24)
            playMusic()
        }
       binding.root.setOnClickListener {
            val intent = Intent(requireContext(), MP3playerActivity::class.java)
            intent.putExtra("index", songPosition)
            intent.putExtra("class", "MP3NowPlaying")
           ContextCompat.startActivity(requireContext(), intent, null)
           requireActivity().overridePendingTransition( R.anim.slide_in_bottom,
               R.anim.anim_no_change)
        }
        return view
    }

    override fun onResume() {
        super.onResume()
        if(MP3playerActivity.musicMP3Service != null) {
            binding.root.visibility = View.VISIBLE
            Glide.with(requireContext())
                .asBitmap()
                .load(MP3playerActivity.mp3MusicPA[songPosition].artUri)
                .apply(RequestOptions().placeholder(R.drawable.music_speaker_three).centerCrop())
                .into(binding.songImgNP)
            binding.songNameNP.isSelected = true
            binding.songNameNP.text = MP3playerActivity.mp3MusicPA[songPosition].title
            if(isPlaying) binding.playPauseBtnNP.setImageResource(R.drawable.round_pause_circle_outline_24)
            else binding.playPauseBtnNP.setImageResource(R.drawable.round_play_circle_outline_24)
        }else{
            binding.root.visibility = View.GONE
        }
    }

    @SuppressLint("SuspiciousIndentation")
    private fun playMusic(){
     isPlaying = true
        MP3playerActivity.musicMP3Service!!.mediaPlayer!!.start()
      binding.playPauseBtnNP.setImageResource(R.drawable.round_pause_circle_outline_24)
        MP3playerActivity.musicMP3Service!!.showNotification(R.drawable.round_pause_circle_outline_24)
    }
    private fun pauseMusic(){
        isPlaying = false
        MP3playerActivity.musicMP3Service!!.mediaPlayer!!.pause()
      binding.playPauseBtnNP.setImageResource(R.drawable.round_play_circle_outline_24)
        MP3playerActivity.musicMP3Service!!.showNotification(R.drawable.round_play_circle_outline_24)
    }

}
