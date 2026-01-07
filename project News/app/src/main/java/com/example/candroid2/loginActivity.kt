package com.example.candroid2

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        auth = Firebase.auth
        binding.loginBtn.setOnClickListener {
            val email = binding.emailEt.text.toString()
            val pass = binding.passEt.text.toString()

            if (email.isBlank() || pass.isBlank())
                Toast.makeText(this, "Missing fields", Toast.LENGTH_SHORT).show()
            else {
                binding.progress.isVisible = true
                login(email,pass)
            }
        }
        binding.notUser.setOnClickListener {
            startActivity(Intent(this, signupActivity::class.java))
            finish()
        }
        binding.forgetPass.setOnClickListener {
            val email = binding.emailEt.text.toString()
            binding.progress.isVisible=true
            Firebase.auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        binding.progress.isVisible=false
                        Toast.makeText(this, "Email sent!", Toast.LENGTH_SHORT).show()
                    }
                }

        }
    }

    private fun login(email: String, pass: String) {
        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    if (auth.currentUser!!.isEmailVerified){
                        startActivity(Intent(this,homeActivity::class.java))
                        finish()
                    }
                    else
                        Toast.makeText(this, "Check Your Email!!!!!!!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    binding.progress.isVisible=false
                }
            }
    }
    public override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        if (currentUser != null && currentUser.isEmailVerified) {
            startActivity(Intent(this,homeActivity::class.java))
            finish()
        }
    }
}