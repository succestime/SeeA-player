package com.jaidev.seeaplayer.recantFragment

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
import com.jaidev.seeaplayer.allAdapters.RecantMusicAdapter
import com.jaidev.seeaplayer.dataClass.getImgArt
import com.jaidev.seeaplayer.dataClass.setReSongPosition
import com.jaidev.seeaplayer.databinding.FragmentReNowPlayingBinding

class ReNowPlaying : Fragment(), RecantMusicAdapter.MusicDeleteListener  , RecantMusicAdapter.OnFileCountChangeListener {
    lateinit var adapter: RecantMusicAdapter
    companion object{
        @SuppressLint("StaticFieldLeak")
        lateinit var binding: FragmentReNowPlayingBinding
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_re_now_playing, container, false)
        binding = FragmentReNowPlayingBinding.bind(view)
        binding.root.visibility = View.GONE
        adapter = RecantMusicAdapter(requireContext(), MainActivity.musicRecantList , isReMusic = true ,this@ReNowPlaying  , isMusic = false)
        initializeBinding()
        binding.nextBtnNP.setOnClickListener {
            setReSongPosition(increment = true)
            ReMusicPlayerActivity.createMediaPlayer(requireContext())
            ReMusicPlayerActivity.setLayout(requireContext())
            Glide.with(this)
                .asBitmap()
                .load(getImgArt(ReMusicPlayerActivity.reMusicListPA[ReMusicPlayerActivity.songPosition].path))
                .apply(RequestOptions().placeholder(R.drawable.music_speaker_three).centerCrop())
                .into(binding.songImgNP)
            binding.songNameNP.text = ReMusicPlayerActivity.reMusicListPA[ReMusicPlayerActivity.songPosition].title
            playMusic()
        }
        return view
    }

    private fun initializeBinding(){
        binding.playPauseBtnNP.setOnClickListener {
            if(ReMusicPlayerActivity.isPlaying){ pauseMusic()
            }
            else {
                playMusic()
            }
        }


        binding.root.setOnClickListener {
            try {
                val intent = Intent(requireContext(), ReMusicPlayerActivity::class.java)
                intent.putExtra("index", ReMusicPlayerActivity.songPosition)
                intent.putExtra("class", "ReNowPlaying")
                ContextCompat.startActivity(requireContext(), intent, null)
                requireActivity().overridePendingTransition( R.anim.slide_in_bottom,
                    R.anim.anim_no_change)
            }catch (_:Exception){

            }

        }

    }
    //private  fun nextButton(){
//    reSetSongPosition(increment = true)
//    ReMusicPlayerActivity.createMediaPlayer()
//
//    Glide.with(this)
//        .asBitmap()
//        .load(getImgArt(ReMusicPlayerActivity.reMusicList[ReMusicPlayerActivity.songPosition].path))
//        .apply(RequestOptions().placeholder(R.drawable.music_speaker_three).centerCrop())
//        .into(binding.songImgNP)
//    binding.songNameNP.text = ReMusicPlayerActivity.reMusicList[ReMusicPlayerActivity.songPosition].title
//    playMusic()
//}
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @SuppressLint("NotifyDataSetChanged")
    override fun onResume() {
        super.onResume()
        try {
            if (ReMusicPlayerActivity.musicService != null) {
                binding.root.visibility = View.VISIBLE
                binding.songNameNP.isSelected = true
                Glide.with(requireContext())
                    .asBitmap()
                    .load(getImgArt(ReMusicPlayerActivity.reMusicListPA[ReMusicPlayerActivity.songPosition].path))
                    .apply(
                        RequestOptions().placeholder(R.drawable.music_speaker_three).centerCrop()
                    )
                    .into(binding.songImgNP)
                binding.songNameNP.text =
                    ReMusicPlayerActivity.reMusicListPA[ReMusicPlayerActivity.songPosition].title
                if (ReMusicPlayerActivity.isPlaying) binding.playPauseBtnNP.setIconResource(R.drawable.round_pause_circle_outline_24)
                else binding.playPauseBtnNP.setIconResource(R.drawable.round_play_circle_outline_24)

            }
        }catch (_: Exception) {
            binding.root.visibility = View.GONE
        }
    }

    private fun playMusic(){
        ReMusicPlayerActivity.isPlaying = true
        ReMusicPlayerActivity.musicService!!.mediaPlayer!!.start()
        binding.playPauseBtnNP.setIconResource(R.drawable.round_pause_circle_outline_24)

//        ReMusicPlayerActivity.musicService!!.showNotification(R.drawable.ic_pause_icon)
    }
    private fun pauseMusic(){
        ReMusicPlayerActivity.isPlaying = false
        ReMusicPlayerActivity.musicService!!.mediaPlayer!!.pause()
        binding.playPauseBtnNP.setIconResource(R.drawable.round_play_circle_outline_24)

//        ReMusicPlayerActivity.musicService!!.showNotification(R.drawable.play_music_icon)
    }

    private fun refreshNowPlayingUI() {

        if (ReMusicPlayerActivity.isPlaying) {

            binding.playPauseBtnNP.setIconResource(R.drawable.round_pause_circle_outline_24)

        } else {
            binding.playPauseBtnNP.setIconResource(R.drawable.round_play_circle_outline_24)
        }
        binding.songNameNP.text = ReMusicPlayerActivity.reMusicListPA[ReMusicPlayerActivity.songPosition].title
        Glide.with(requireContext())
            .asBitmap()
            .load(getImgArt(ReMusicPlayerActivity.reMusicListPA[ReMusicPlayerActivity.songPosition].path))
            .apply(RequestOptions().placeholder(R.drawable.music_speaker_three).centerCrop())
            .into(binding.songImgNP)
    }

    override fun onFileCountChanged(newCount: Int) {
    }

    override fun onMusicDeleted() {
        refreshNowPlayingUI()

    }


}