package com.jaidev.seeaplayer

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.jaidev.seeaplayer.databinding.ActivityMoreBinding

class More : AppCompatActivity(){

private lateinit var binding : ActivityMoreBinding

    companion object{
        lateinit var auth : FirebaseAuth

    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMoreBinding.inflate(layoutInflater)

        setContentView(binding.root)


       auth = FirebaseAuth.getInstance()

        binding.goToSignin.setOnClickListener {
                startActivity(Intent(this, FoldersActivity::class.java))
        }
if(auth.currentUser == null ){
    startActivity(Intent(this, signin::class.java))
    finish()
}

        binding.signOut.setOnClickListener{
            auth.signOut()
            binding.userDetails.text = updateData()
        }
        supportActionBar?.apply {

            setBackgroundDrawable(ContextCompat.getDrawable(this@More, R.drawable.background_actionbar))
        }

    }

    override fun onResume() {
        super.onResume()
        binding.userDetails.text = updateData()


    }
    private fun updateData(): String {

        return "Name : ${auth.currentUser?.displayName}"

    }




}