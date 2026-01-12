package com.example.candroid2

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.candroid2.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class loginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupFirebase()
        setupClickListeners()
        setupAnimations()
    }

    private fun setupFirebase() {
        auth = Firebase.auth
    }

    private fun setupClickListeners() {
        binding.btnLoginUp.setOnClickListener {
            val email = binding.editEmail.text.toString()
            val password = binding.editPassword.text.toString()

            if (email.isBlank() || password.isBlank()) {
                showToast(getString(R.string.missing_fields))
            } else {
                binding.progress.isVisible = true
                loginUser(email, password)
            }
        }

        binding.notUser.setOnClickListener {
            navigateToSignup()
        }

        binding.forgetPass.setOnClickListener {
            handlePasswordReset()
        }
    }

    private fun setupAnimations() {
        binding.root.alpha = 0f
        binding.root.animate()
            .alpha(1f)
            .setDuration(1500)
            .start()
    }

    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null && user.isEmailVerified) {
                        navigateToHome()
                    } else {
                        showToast(getString(R.string.check_your_email))
                        binding.progress.isVisible = false
                    }
                } else {
                    showToast(task.exception?.message ?: "Login failed")
                    binding.progress.isVisible = false
                }
            }
    }

    private fun handlePasswordReset() {
        val email = binding.editEmail.text.toString()

        if (email.isBlank()) {
            showToast(getString(R.string.please_enter_your_email_first))
            return
        }

        binding.progress.isVisible = true

        Firebase.auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                binding.progress.isVisible = false

                if (task.isSuccessful) {
                    showToast(getString(R.string.email_sent))
                } else {
                    val errorMessage = task.exception?.message ?: "Failed to send reset email"
                    showToast(errorMessage)
                }
            }
    }

    private fun navigateToSignup() {
        startActivity(Intent(this, signupActivity::class.java))
        finish()
    }

    private fun navigateToHome() {
        startActivity(Intent(this, homeActivity::class.java))
        finish()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onStart() {
        super.onStart()

        val currentUser = auth.currentUser
        if (currentUser != null && currentUser.isEmailVerified) {
            navigateToHome()
        }
    }
}