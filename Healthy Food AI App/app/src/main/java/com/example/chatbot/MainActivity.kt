package com.example.chatbot

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.example.chatbot.databinding.ActivityMainBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth

class MainActivity : AppCompatActivity() {
    private lateinit var binding :ActivityMainBinding
    private lateinit var auth :FirebaseAuth
    private lateinit var themeToggleButton: Button
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 9002

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding =ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val sharedPref = getSharedPreferences("settings", MODE_PRIVATE)
        val darkMode = sharedPref.getBoolean("dark_mode", false)
        if (darkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        themeToggleButton = findViewById(R.id.themeToggleButton)
        updateButton()
        themeToggleButton.setOnClickListener { toggleTheme() }

        binding.root.alpha = 0f
        binding.root.animate().alpha(1f).setDuration(1500).start()
        // Firebase Auth
        auth =Firebase.auth
        binding.btnSignUp.setOnClickListener {
            val email = binding.editEmail.text.toString()
            val pass = binding.editPassword.text.toString()
            val name = binding.editName.text.toString()
            when {
                email.isBlank() || pass.isBlank() || name.isBlank() ->
                    Toast.makeText(this, getString(R.string.missing_fields), Toast.LENGTH_SHORT).show()

                pass.length < 6 ->
                    Toast.makeText(this, getString(R.string.password_too_short), Toast.LENGTH_SHORT).show()

                else -> {
                    binding.progress.isVisible = true
                    createUser(email, pass)
                }
            }
        }

        binding.alreadyUser.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        binding.guest.setOnClickListener {
            showAnimatedToast("Welcome Guest 🎉", R.raw.food, 7000L)
            Handler(Looper.getMainLooper()).postDelayed({
                startActivity(Intent(this, HomeActivity::class.java))
                finish()
            }, 7000)
        }

               val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(BuildConfig.GOOGLE_CLIENT_ID)
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        binding.btnGoogleSignIn.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }

    }

    private fun toggleTheme(){
        val sharedPref = getSharedPreferences("settings", MODE_PRIVATE)
        val editor = sharedPref.edit()
        val currentNightMode = AppCompatDelegate.getDefaultNightMode()

        if (currentNightMode == AppCompatDelegate.MODE_NIGHT_YES) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            editor.putBoolean("dark_mode", false)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            editor.putBoolean("dark_mode", true)
        }
        editor.apply()
        recreate()
    }

    private fun updateButton() {
        val sharedPref = getSharedPreferences("settings", MODE_PRIVATE)
        val darkMode = sharedPref.getBoolean("dark_mode", false)
        if (darkMode) {
            themeToggleButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.sun, 0, 0, 0)
        } else {
            themeToggleButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.iosmoon, 0, 0, 0)
        }
    }
    private fun createUser(email: String, pass: String) {
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val uid = Firebase.auth.currentUser?.uid ?: return@addOnCompleteListener
                    val name = binding.editName.text.toString()
                    val prefsName = "UserData_${email.hashCode()}"
                    val sharedPref = getSharedPreferences(prefsName, Context.MODE_PRIVATE)
                    sharedPref.edit().apply {
                        putString("user_name", name)
                        putString("user_email", email)
                        apply()
                    }
                    verifyEmail()
                } else {
                    Toast.makeText(this, task.exception?.message, Toast.LENGTH_SHORT).show()
                }
                binding.progress.isVisible = false
            }
    }

    private fun verifyEmail() {
        val user = Firebase.auth.currentUser
        user?.sendEmailVerification()?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, getString(R.string.check_your_email), Toast.LENGTH_SHORT).show()
                binding.progress.isVisible = false
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Toast.makeText(this, "Error Code: ${e.statusCode}", Toast.LENGTH_LONG).show()
            }
        }
    }
    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                val account = GoogleSignIn.getLastSignedInAccount(this)
                val name = account?.displayName
                val email = account?.email
                val prefsName = "UserData_${email?.hashCode() ?: 0}"
                val sharedPref = getSharedPreferences(prefsName, Context.MODE_PRIVATE)
                sharedPref.edit().apply {
                    putString("user_name", name)
                    putString("user_email", email)
                    apply()
                }
                showAnimatedToast("Logged in successfully! 🎉", R.raw.food)

                Handler(Looper.getMainLooper()).postDelayed({
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                }, 3000)
            } else {
                Toast.makeText(this, getString(R.string.authentication_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }
    override fun onStart() {
        super.onStart()
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }
    }
    override fun onBackPressed() {
        val exit = Exit()
        exit.isCancelable = false
        exit.show(supportFragmentManager, null)
    }

    private fun showAnimatedToast(message: String, lottieFile: Int, durationMs: Long = 3000L) {
        binding.toastLayout.visibility = View.VISIBLE
        binding.lottieToast.setAnimation(lottieFile)
        binding.lottieToast.playAnimation()
        binding.toastMessage.text = message

        Handler(Looper.getMainLooper()).postDelayed({
            binding.toastLayout.visibility = View.GONE
        }, durationMs)
    }

}