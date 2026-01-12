package com.example.candroid2

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.isVisible
import com.example.candroid2.databinding.ActivitySignupBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class signupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupTheme()
        setupFirebase()
        setupClickListeners()
        setupAnimations()
    }

    private fun setupTheme() {
        val sharedPref = getSharedPreferences("settings", MODE_PRIVATE)
        val isDarkMode = sharedPref.getBoolean("dark_mode", false)

        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        updateThemeButton()
        binding.themeToggleButton.setOnClickListener { toggleTheme() }
    }

    private fun setupFirebase() {
        auth = Firebase.auth
    }

    private fun setupClickListeners() {
        binding.btnSignUp.setOnClickListener {
            val email = binding.editEmail.text.toString()
            val password = binding.editPassword.text.toString()

            when {
                email.isBlank() || password.isBlank() -> {
                    showToast(getString(R.string.missing_fields))
                }
                password.length < 6 -> {
                    showToast(getString(R.string.password_too_short))
                }
                else -> {
                    binding.progress.isVisible = true
                    createUserAccount(email, password)
                }
            }
        }

        binding.alreadyUser.setOnClickListener {
            navigateToLogin()
        }
    }

    private fun setupAnimations() {
        binding.root.alpha = 0f
        binding.root.animate()
            .alpha(1f)
            .setDuration(1500)
            .start()
    }

    private fun toggleTheme() {
        val sharedPref = getSharedPreferences("settings", MODE_PRIVATE)
        val editor = sharedPref.edit()

        val currentMode = AppCompatDelegate.getDefaultNightMode()

        if (currentMode == AppCompatDelegate.MODE_NIGHT_YES) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            editor.putBoolean("dark_mode", false)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            editor.putBoolean("dark_mode", true)
        }

        editor.apply()
        recreate()
    }

    private fun updateThemeButton() {
        val sharedPref = getSharedPreferences("settings", MODE_PRIVATE)
        val isDarkMode = sharedPref.getBoolean("dark_mode", false)

        val iconRes = if (isDarkMode) R.drawable.sun else R.drawable.iosmoon
        binding.themeToggleButton.setCompoundDrawablesWithIntrinsicBounds(iconRes, 0, 0, 0)
    }

    private fun createUserAccount(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    showToast("Account created successfully")
                    sendVerificationEmail()
                    navigateToLogin()
                } else {
                    val errorMessage = task.exception?.message ?: "Account creation failed"
                    showToast(errorMessage)
                }
                binding.progress.isVisible = false
            }
    }

    private fun sendVerificationEmail() {
        val user = auth.currentUser
        user?.sendEmailVerification()
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    showToast(getString(R.string.check_your_email))
                }
            }
    }

    private fun navigateToLogin() {
        startActivity(Intent(this, loginActivity::class.java))
        finish()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onStart() {
        super.onStart()

        val currentUser = auth.currentUser
        if (currentUser != null && currentUser.isEmailVerified) {
            startActivity(Intent(this, homeActivity::class.java))
            finish()
        }
    }
}