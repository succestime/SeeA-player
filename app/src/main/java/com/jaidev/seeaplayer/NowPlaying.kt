package com.jaidev.seeaplayer

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
import com.jaidev.seeaplayer.dataClass.setSongPosition
import com.jaidev.seeaplayer.databinding.FragmentNowPlayingBinding

class NowPlaying : Fragment(), MusicAdapter.MusicDeleteListener  {
    lateinit var adapter: MusicAdapter
    companion object{
       @SuppressLint("StaticFieldLeak")
        lateinit var binding: FragmentNowPlayingBinding
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_now_playing, container, false)
        binding = FragmentNowPlayingBinding.bind(view)
        binding.root.visibility = View.INVISIBLE

        adapter = MusicAdapter(requireContext(), MainActivity.MusicListMA)
        adapter.setMusicDeleteListener(this)

        binding.playPauseBtnNP.setOnClickListener {
            if(PlayerMusicActivity.isPlaying) pauseMusic() else playMusic()
        }
        binding.nextBtnNP.setOnClickListener {
            setSongPosition(increment = true)
            PlayerMusicActivity.musicService!!.createMediaPlayer()
            Glide.with(this)
                .load(PlayerMusicActivity.musicListPA[PlayerMusicActivity.songPosition].artUri)
                .apply(RequestOptions().placeholder(R.drawable.speaker).centerCrop())
                .into(binding.songImgNP)
            binding.songNameNP.text = PlayerMusicActivity.musicListPA[PlayerMusicActivity.songPosition].title
            PlayerMusicActivity.musicService!!.showNotification(R.drawable.ic_pause_icon)
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
        if(PlayerMusicActivity.musicService != null){
           // requireActivity().registerReceiver(receiver, IntentFilter(ACTION_MUSIC_DELETED),
             //   Context.RECEIVER_NOT_EXPORTED)

            binding.root.visibility = View.VISIBLE
            binding.songNameNP.isSelected = true
            Glide.with(requireContext())
                .load(PlayerMusicActivity.musicListPA[PlayerMusicActivity.songPosition].artUri)
                .apply(RequestOptions().placeholder(R.drawable.speaker).centerCrop())
                .into(binding.songImgNP)
            binding.songNameNP.text = PlayerMusicActivity.musicListPA[PlayerMusicActivity.songPosition].title
            if(PlayerMusicActivity.isPlaying) binding.playPauseBtnNP.setIconResource(R.drawable.ic_pause_icon)
            else binding.playPauseBtnNP.setIconResource(R.drawable.play_music_icon)

        }
    }

    private fun playMusic(){
        PlayerMusicActivity.isPlaying = true
        PlayerMusicActivity.musicService!!.mediaPlayer!!.start()
        binding.playPauseBtnNP.setIconResource(R.drawable.ic_pause_icon)
        PlayerMusicActivity.musicService!!.showNotification(R.drawable.ic_pause_icon)
    }
    private fun pauseMusic(){
        PlayerMusicActivity.isPlaying = false
        PlayerMusicActivity.musicService!!.mediaPlayer!!.pause()
        binding.playPauseBtnNP.setIconResource(R.drawable.play_music_icon)
        PlayerMusicActivity.musicService!!.showNotification(R.drawable.play_music_icon)
    }

    override fun onMusicDeleted() {
        // Check if the NowPlaying fragment is currently visible
        if (isVisible) {
            // Refresh the UI with the current playing music or take any other necessary action
            refreshNowPlayingUI()
        }
    }

    private fun refreshNowPlayingUI() {

        if (PlayerMusicActivity.isPlaying) {
            binding.playPauseBtnNP.setIconResource(R.drawable.ic_pause_icon)
        } else {
            binding.playPauseBtnNP.setIconResource(R.drawable.play_music_icon)
        }
        binding.songNameNP.text = PlayerMusicActivity.musicListPA[PlayerMusicActivity.songPosition].title
        Glide.with(requireContext())
            .load(PlayerMusicActivity.musicListPA[PlayerMusicActivity.songPosition].artUri)
            .apply(RequestOptions().placeholder(R.drawable.speaker).centerCrop())
            .into(binding.songImgNP)
    }


}