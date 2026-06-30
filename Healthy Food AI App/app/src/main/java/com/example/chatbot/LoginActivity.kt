package com.example.chatbot

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.example.chatbot.databinding.ActivityLoginBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        auth = Firebase.auth
        binding.root.alpha = 0f
        binding.root.animate().alpha(1f).setDuration(1500).start()

        binding.btnLoginUp.setOnClickListener {
            val email = binding.editEmail.text.toString()
            val pass = binding.editPassword.text.toString()

            if (email.isBlank() || pass.isBlank()) {
                showAnimatedToast(getString(R.string.missing_fields), R.raw.food)
            } else {
                binding.progress.isVisible = true
                login(email, pass)
            }
        }

        binding.notUser.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        binding.forgetPass.setOnClickListener {
            val email = binding.editEmail.text.toString()
            if (email.isBlank()) {
                showAnimatedToast(getString(R.string.please_enter_your_email_first), R.raw.food)
                return@setOnClickListener
            }
            binding.progress.isVisible = true
            Firebase.auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    binding.progress.isVisible = false
                    if (task.isSuccessful) {
                        showAnimatedToast(getString(R.string.email_sent), R.raw.food)
                    } else {
                        showAnimatedToast(getString(R.string.filled), R.raw.healthy_food_for_diet_fitness)
                    }
                }
        }
    }

    private fun login(email: String, pass: String) {
        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    if (auth.currentUser!!.isEmailVerified) {
                        showAnimatedToast("Welcome! \uD83C\uDF89", R.raw.food)
                        Handler(Looper.getMainLooper()).postDelayed({
                            startActivity(Intent(this, HomeActivity::class.java))
                            finish()
                        }, 2000)
                    } else {
                        showAnimatedToast(getString(R.string.check_your_email), R.raw.healthy_food_for_diet_fitness)
                    }
                } else {
                    showAnimatedToast("${task.exception?.message}", R.raw.healthy_food_for_diet_fitness)
                    binding.progress.isVisible = false
                }
            }
    }

    private fun showAnimatedToast(message: String, lottieFile: Int) {
        binding.toastLayout.visibility = View.VISIBLE
        binding.lottieToast.setAnimation(lottieFile)
        binding.lottieToast.playAnimation()
        binding.toastMessage.text = message

        Handler(Looper.getMainLooper()).postDelayed({
            binding.toastLayout.visibility = View.GONE
        }, 3000)
    }

    public override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if (currentUser != null && currentUser.isEmailVerified) {
            val email = currentUser.email
            if (!email.isNullOrBlank()) {
                startActivity(Intent(this, HomeActivity::class.java))
            }
            finish()
        }
    }

    override fun onBackPressed() {
        val exit = Exit()
        exit.isCancelable = false
        exit.show(supportFragmentManager, null)
    }
}