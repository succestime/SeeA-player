package com.jaidev.seeaplayer

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.jaidev.seeaplayer.bottomNavigation.moreNav
import com.jaidev.seeaplayer.databinding.ActivityLoginBinding

class login : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()


        binding.Signin.setOnClickListener {
            startActivity(Intent(this , signin::class.java))
            finish()
        }

        supportActionBar?.apply {

            setBackgroundDrawable(ContextCompat.getDrawable(this@login, R.drawable.background_actionbar))
        }

        binding.loginBtn.setOnClickListener {
            val email = binding.emailLogin.text.toString()
            val password = binding.passwordLogin.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty())
              moreNav.auth.signInWithEmailAndPassword(email , password).addOnCompleteListener {
                    if (it.isSuccessful){
                        startActivity(Intent(this,MainActivity::class.java))
                    }
                }.addOnFailureListener {
                    Toast.makeText(this, it.localizedMessage, Toast.LENGTH_LONG).show()
                }
        }
    }
}