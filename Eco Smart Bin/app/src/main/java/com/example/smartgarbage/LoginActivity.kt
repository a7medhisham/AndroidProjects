package com.example.smartgarbage

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.example.smartgarbage.databinding.ActivityLoginBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import okhttp3.ResponseBody
import java.util.Locale

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var isGettingLocation = false

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
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        binding.root.alpha = 0f
        binding.root.animate().alpha(1f).setDuration(1500).start()

        Glide.with(this)
            .load("https://images.unsplash.com/photo-1501004318641-b39e6451bec6")
            .centerCrop()
            .into(binding.bgImage)

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
                Toast.makeText(this,
                    getString(R.string.please_enter_your_email_first), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            binding.progress.isVisible = true
            Firebase.auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    binding.progress.isVisible = false
                    if (task.isSuccessful) {
                        Toast.makeText(this, getString(R.string.email_sent), Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(
                            this,
                            getString(R.string.failed_message_text, task.exception?.message ?: ""),
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
                        binding.progress.isVisible = false
                        val prefsName = "UserData_${email.hashCode()}"
                        val sharedPref = getSharedPreferences(prefsName, MODE_PRIVATE)
                        val role = sharedPref.getString("role", "user") ?: "user"
                        val dataSent = sharedPref.getBoolean("data_sent_to_backend", false)

                        if (dataSent) {
                            // Data already sent, go to home directly
                            goToHome(role)
                        } else {
                            // Need to get location first
                            checkLocationAndGet(email, role, sharedPref)
                        }
                    } else {
                        binding.progress.isVisible = false
                        Toast.makeText(this, getString(R.string.check_your_email), Toast.LENGTH_SHORT).show()
                        auth.signOut()
                    }
                } else {
                    Toast.makeText(this, task.exception?.message, Toast.LENGTH_SHORT).show()
                    binding.progress.isVisible = false
                }
            }
    }

    private fun checkLocationAndGet(email: String, role: String, sharedPref: android.content.SharedPreferences) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 200)
            return
        }
        getCurrentLocation(email, role, sharedPref)
    }

    private fun getCurrentLocation(email: String, role: String, sharedPref: android.content.SharedPreferences) {
        if (isGettingLocation) return
        isGettingLocation = true

        Toast.makeText(this, "Getting your location...", Toast.LENGTH_SHORT).show()

        val locationRequest = com.google.android.gms.location.LocationRequest.Builder(
            com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
            5000
        ).build()

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            object : com.google.android.gms.location.LocationCallback() {
                override fun onLocationResult(locationResult: com.google.android.gms.location.LocationResult) {
                    val location = locationResult.lastLocation
                    if (location != null) {
                        fusedLocationClient.removeLocationUpdates(this)
                        isGettingLocation = false
                        saveLocationAndProceed(location, email, role, sharedPref)
                    }
                }
            },
            Looper.getMainLooper()
        )

        Handler(Looper.getMainLooper()).postDelayed({
            if (isGettingLocation) {
                fusedLocationClient.removeLocationUpdates(object : com.google.android.gms.location.LocationCallback() {})
                isGettingLocation = false
                // Proceed without location
                val address = "Location not available"
                sendDataToBackend(email, role, address, sharedPref)
                goToHome(role)
            }
        }, 10000)
    }

    private fun saveLocationAndProceed(location: Location, email: String, role: String, sharedPref: android.content.SharedPreferences) {
        val geocoder = Geocoder(this, Locale.getDefault())
        val addresses = try {
            geocoder.getFromLocation(location.latitude, location.longitude, 1)
        } catch (e: Exception) {
            null
        }

        val address = if (!addresses.isNullOrEmpty()) {
            val city = addresses[0].locality ?: "unknown"
            val country = addresses[0].countryName ?: "unknown"
            "$city, $country"
        } else {
            "Lat: ${location.latitude}, Lon: ${location.longitude}"
        }

        sharedPref.edit().apply {
            putString("latitude", location.latitude.toString())
            putString("longitude", location.longitude.toString())
            putString("address", address)
            apply()
        }

        sendDataToBackend(email, role, address, sharedPref)
        goToHome(role)
    }

    private fun sendDataToBackend(email: String, role: String, address: String, sharedPref: android.content.SharedPreferences) {
        val uid = auth.currentUser?.uid ?: ""
        val name = sharedPref.getString("user_name", "") ?: ""

        val userData = mapOf(
            "UserID" to uid,
            "Email" to email,
            "FirstName" to name,
            "LastName" to name,
            "Address" to address,
            "Role" to role,
            "QRCode" to uid
        )

        RetrofitInstance.api.registerUser(userData).enqueue(object : retrofit2.Callback<ResponseBody> {
            override fun onResponse(call: retrofit2.Call<ResponseBody>, response: retrofit2.Response<ResponseBody>) {
                if (response.isSuccessful) {
                    sharedPref.edit().putBoolean("data_sent_to_backend", true).apply()
                    Toast.makeText(this@LoginActivity, "Data sent successfully", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: retrofit2.Call<ResponseBody>, t: Throwable) {
                Toast.makeText(this@LoginActivity, "Failed to send data: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun goToHome(role: String) {
        if (role == "worker") {
            startActivity(Intent(this, WorkerHomeActivity::class.java))
        } else {
            startActivity(Intent(this, HomeActivity::class.java))
        }
        finish()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 200 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            val email = auth.currentUser?.email ?: return
            val prefsName = "UserData_${email.hashCode()}"
            val sharedPref = getSharedPreferences(prefsName, MODE_PRIVATE)
            val role = sharedPref.getString("role", "user") ?: "user"
            getCurrentLocation(email, role, sharedPref)
        }
    }

    override fun onBackPressed() {
        val exit = Exit()
        exit.isCancelable = false
        exit.show(supportFragmentManager, null)
    }
}