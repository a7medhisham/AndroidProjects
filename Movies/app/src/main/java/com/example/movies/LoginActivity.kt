package com.example.movies

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.example.movies.databinding.ActivityLoginBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth


class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    @SuppressLint("ClickableViewAccessibility")
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
                Toast.makeText(this, getString(R.string.missing_fields), Toast.LENGTH_SHORT).show()
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
                Toast.makeText(
                    this,
                    getString(R.string.please_enter_your_email_first), Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            binding.progress.isVisible = true
            Firebase.auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    binding.progress.isVisible = false
                    if (task.isSuccessful) {
                        Toast.makeText(this, getString(R.string.email_sent), Toast.LENGTH_SHORT)
                            .show()
                    } else {
                        Toast.makeText(
                            this,
                            getString(R.string.failed, task.exception?.message),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }
    }

    private fun login(email: String, pass: String) {
        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    if (auth.currentUser!!.isEmailVerified) {
                        startActivity(Intent(this, HomeActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(
                            this,
                            getString(R.string.check_your_email),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(this, "${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    binding.progress.isVisible = false
                }
            }
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
