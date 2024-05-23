package com.jaidev.seeaplayer.LogSignIn

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.bottomNavigation.moreNav
import com.jaidev.seeaplayer.databinding.ActivitySigninBinding

class signin : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setTheme(More.themesList[More.themeIndex])
        val binding = ActivitySigninBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.client_id))
            .requestEmail()
            .build()

        val googleSignInClient = GoogleSignIn.getClient(this,gso)

        binding.login.setOnClickListener {
            startActivity(Intent(this , SignUp::class.java))
            finish()
        }
        setActionBarGradient()

        binding.createAccountBtn.setOnClickListener {
            val name = binding.userName.text.toString()
            val email = binding.emailResister.text.toString()
            val password = binding.passwordResister.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty() && name.isNotEmpty()) {
                moreNav.auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { createUserTask ->
                        if (createUserTask.isSuccessful) {
                            val user = moreNav.auth.currentUser
                            val profileUpdates = UserProfileChangeRequest.Builder()
                                .setDisplayName(name)
                                .build()

                            user?.updateProfile(profileUpdates)
                                ?.addOnCompleteListener { updateProfileTask ->
                                    if (updateProfileTask.isSuccessful) {
                                        Toast.makeText(this, "SignIn Successful", Toast.LENGTH_LONG).show()
                                        finish()

                                    } else {
                                        // Handle failure to update profile
                                        Toast.makeText(this, "Failed to update profile", Toast.LENGTH_LONG).show()
                                    }
                                }
                        } else {
                            // Handle failure to create user
                            Toast.makeText(this, "Failed to create user: ${createUserTask.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }.addOnFailureListener {
                    Toast.makeText(this, it.localizedMessage, Toast.LENGTH_LONG).show()
                }
            } else {
                // Handle empty fields
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_LONG).show()
            }
        }

        binding.googleBtn.setOnClickListener {
            googleSignInClient.signOut()
            startActivityForResult(googleSignInClient.signInIntent,4)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 4 && resultCode == RESULT_OK ){
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)!!
            firebaseAuthWithGoogle(account.idToken!!)
        }

    }
private fun firebaseAuthWithGoogle(idToken : String){
    val credential = GoogleAuthProvider.getCredential(idToken , null)
    moreNav.auth.signInWithCredential(credential)
        .addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "SignIn Successful", Toast.LENGTH_LONG).show()
                finish()
            }
        }.addOnFailureListener {
            Toast.makeText(this, it.localizedMessage, Toast.LENGTH_LONG).show()
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
                        this@signin,
                        R.drawable.background_actionbar_light
                    )
                )
            }
        } else if (nightMode == AppCompatDelegate.MODE_NIGHT_YES) {
            // Dark mode is applied
            supportActionBar?.apply {
                setBackgroundDrawable(
                    ContextCompat.getDrawable(
                        this@signin,
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
                            this@signin,
                            R.drawable.background_actionbar
                        )
                    )
                }
            } else {
                // System Default mode is light
                supportActionBar?.apply {
                    setBackgroundDrawable(
                        ContextCompat.getDrawable(
                            this@signin,
                            R.drawable.background_actionbar_light
                        )
                    )
                }
            }
        }
    }
}