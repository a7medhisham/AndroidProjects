package com.example.candroid2

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
        enableEdgeToEdge()
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // Initialize Firebase Auth
        auth = Firebase.auth
        binding.signBtn.setOnClickListener {
            val email = binding.emailEt.text.toString()
            val pass = binding.passEt.text.toString()
            val compass = binding.confirmEt.text.toString()
            if (email.isBlank() || pass.isBlank() || compass.isBlank())
                Toast.makeText(this, "Missing fields", Toast.LENGTH_SHORT).show()
            else if (pass.length < 6)
                Toast.makeText(this, "password is too short", Toast.LENGTH_SHORT).show()
            else if (pass != compass)
                Toast.makeText(this, "password dont match", Toast.LENGTH_SHORT).show()
            else {
                binding.progress.isVisible = true
                createUser(email,pass)
            }
        }
        binding.alreadyUser.setOnClickListener {
            startActivity(Intent(this, loginActivity::class.java))
            finish()
        }
    }

    private fun createUser(email: String, pass: String) {
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful)
                    verifyEmail()
                else
                    Toast.makeText(this, "${task.exception?.message}", Toast.LENGTH_SHORT).show()
                binding.progress.isVisible=false
            }
    }

    private fun verifyEmail() {
        val user = Firebase.auth.currentUser

        user!!.sendEmailVerification()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Check Your Email", Toast.LENGTH_SHORT).show()
                    binding.progress.isVisible=false
                }
            }
    }
}