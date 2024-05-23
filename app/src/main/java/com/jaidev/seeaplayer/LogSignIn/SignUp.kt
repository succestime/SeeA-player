package com.jaidev.seeaplayer.LogSignIn

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.bottomNavigation.moreNav
import com.jaidev.seeaplayer.databinding.ActivityLoginBinding

class SignUp : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()
        setActionBarGradient()

        binding.Signin.setOnClickListener {
            startActivity(Intent(this , signin::class.java))
            finish()
        }

        binding.loginBtn.setOnClickListener {
            val email = binding.emailLogin.text.toString()
            val password = binding.passwordLogin.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty())
              moreNav.auth.signInWithEmailAndPassword(email , password).addOnCompleteListener {
                    if (it.isSuccessful){
                        Toast.makeText(this, "SignUp Successful", Toast.LENGTH_LONG).show()
                        finish()

                    }
                }.addOnFailureListener {
                    Toast.makeText(this, it.localizedMessage, Toast.LENGTH_LONG).show()
                }
        }
    }


    private fun setActionBarGradient() {
        // Check the current night mode
        val nightMode = AppCompatDelegate.getDefaultNightMode()
        if (nightMode == AppCompatDelegate.MODE_NIGHT_NO) {
            // Light mode is applied
            supportActionBar?.apply {
                setBackgroundDrawable(
                    ContextCompat.getDrawable(
                        this@SignUp,
                        R.drawable.background_actionbar_light
                    )
                )
            }
        } else if (nightMode == AppCompatDelegate.MODE_NIGHT_YES) {
            // Dark mode is applied
            supportActionBar?.apply {
                setBackgroundDrawable(
                    ContextCompat.getDrawable(
                        this@SignUp,
                        R.drawable.background_actionbar
                    )
                )
            }
        } else {
            // System Default mode is applied
            val isSystemDefaultDarkMode = when (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) {
                android.content.res.Configuration.UI_MODE_NIGHT_YES -> true
                else -> false
            }
            // Set the ActionBar color based on the System Default mode
            if (isSystemDefaultDarkMode) {
                // System Default mode is dark
                supportActionBar?.apply {
                    setBackgroundDrawable(
                        ContextCompat.getDrawable(
                            this@SignUp,
                            R.drawable.background_actionbar
                        )
                    )
                }
            } else {
                // System Default mode is light
                supportActionBar?.apply {
                    setBackgroundDrawable(
                        ContextCompat.getDrawable(
                            this@SignUp,
                            R.drawable.background_actionbar_light
                        )
                    )
                }
            }
        }
    }
}