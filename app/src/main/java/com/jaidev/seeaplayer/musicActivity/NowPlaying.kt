package com.jaidev.seeaplayer.musicActivity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.jaidev.seeaplayer.MainActivity
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.allAdapters.MusicAdapter
import com.jaidev.seeaplayer.allAdapters.RecantMusicAdapter
import com.jaidev.seeaplayer.dataClass.getImgArt
import com.jaidev.seeaplayer.dataClass.setSongPosition
import com.jaidev.seeaplayer.databinding.FragmentNowPlayingBinding

class NowPlaying : Fragment(), MusicAdapter.MusicDeleteListener ,  RecantMusicAdapter.MusicDeleteListener  {
    lateinit var adapter: MusicAdapter
    companion object{
       @SuppressLint("StaticFieldLeak")
        lateinit var binding: FragmentNowPlayingBinding
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_now_playing, container, false)
        binding = FragmentNowPlayingBinding.bind(view)
        binding.root.visibility = View.GONE

        adapter = MusicAdapter(requireContext(), MainActivity.MusicListMA)
        adapter.setMusicDeleteListener(this)

        binding.playPauseBtnNP.setOnClickListener {
            if(PlayerMusicActivity.isPlaying) pauseMusic() else playMusic()
        }
        binding.nextBtnNP.setOnClickListener {
            setSongPosition(increment = true)
            PlayerMusicActivity.musicService!!.createMediaPlayer()
            Glide.with(this)
                .asBitmap()
                .load(getImgArt(PlayerMusicActivity.musicListPA[PlayerMusicActivity.songPosition].path))
                .apply(RequestOptions().placeholder(R.drawable.music_speaker_three).centerCrop())
                .into(binding.songImgNP)
            binding.songNameNP.text = PlayerMusicActivity.musicListPA[PlayerMusicActivity.songPosition].title
            PlayerMusicActivity.musicService!!.showNotification(R.drawable.round_pause_24)
            playMusic()
        }
        binding.root.setOnClickListener {
            val intent = Intent(requireContext(), PlayerMusicActivity::class.java)
            intent.putExtra("index", PlayerMusicActivity.songPosition)
            intent.putExtra("class", "NowPlaying")
            ContextCompat.startActivity(requireContext(), intent, null)
        }

        return view
    }



    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @SuppressLint("NotifyDataSetChanged")
    override fun onResume() {
        super.onResume()
        try {
        if(PlayerMusicActivity.musicService != null){
            binding.root.visibility = View.VISIBLE
            binding.songNameNP.isSelected = true
            Glide.with(requireContext())
                .asBitmap()
                .load(getImgArt(PlayerMusicActivity.musicListPA[PlayerMusicActivity.songPosition].path))
                .apply(RequestOptions().placeholder(R.drawable.music_speaker_three).centerCrop())
                .into(binding.songImgNP)
            binding.songNameNP.text = PlayerMusicActivity.musicListPA[PlayerMusicActivity.songPosition].title
            if(PlayerMusicActivity.isPlaying) binding.playPauseBtnNP.setIconResource(R.drawable.round_pause_24)
            else binding.playPauseBtnNP.setIconResource(R.drawable.play_icon)
        }

        }catch (e: Exception) {
            binding.root.visibility = View.GONE
            PlayerMusicActivity.musicService?.stopService() // Stop the music service
        }
    }

    private fun playMusic(){
        PlayerMusicActivity.isPlaying = true
        PlayerMusicActivity.musicService!!.mediaPlayer!!.start()
        binding.playPauseBtnNP.setIconResource(R.drawable.pause_circle_outline_24)
        PlayerMusicActivity.musicService!!.showNotification(R.drawable.round_pause_24)
    }
    private fun pauseMusic(){
        PlayerMusicActivity.isPlaying = false
        PlayerMusicActivity.musicService!!.mediaPlayer!!.pause()
        binding.playPauseBtnNP.setIconResource(R.drawable.play_icon)
        PlayerMusicActivity.musicService!!.showNotification(R.drawable.round_play)
    }

    override fun onMusicDeleted() {
        refreshNowPlayingUI()
        if (isVisible) {
            binding.root.visibility = View.GONE
            refreshNowPlayingUI()

        }
    }

    private fun refreshNowPlayingUI() {

        if (PlayerMusicActivity.isPlaying) {
            binding.playPauseBtnNP.setIconResource(R.drawable.round_pause_24)
        } else {
            binding.playPauseBtnNP.setIconResource(R.drawable.round_play)
        }
        binding.songNameNP.text = PlayerMusicActivity.musicListPA[PlayerMusicActivity.songPosition].title
        Glide.with(requireContext())
            .asBitmap()
            .load(getImgArt(PlayerMusicActivity.musicListPA[PlayerMusicActivity.songPosition].path))
            .apply(RequestOptions().placeholder(R.drawable.music_speaker_three).centerCrop())
            .into(binding.songImgNP)
    }


}