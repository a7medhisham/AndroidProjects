package com.example.smartgarbage

import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.vectordrawable.graphics.drawable.ArgbEvaluator
import com.example.smartgarbage.databinding.ActivityMainBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Handler
import android.os.Looper
import android.widget.ScrollView
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.util.Locale
import okhttp3.ResponseBody
import retrofit2.Callback

class MainActivity : AppCompatActivity() {
    private var numRows = 0
    private var numCols = 8
    private val squares = mutableListOf<View>()
    private var userRole = "user"
    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var themeToggleButton: Button
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 9001
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val allowedWorkerEmails = listOf(
        "hm9136428@gmail.com",
        "ddemo6945@gmail.com",
        "mh4034467@gmail.com"
    )

    private fun isAllowedWorker(email: String): Boolean {
        return allowedWorkerEmails.contains(email.lowercase())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val emailFromLogin = intent.getStringExtra("email")
        val nameFromLogin = intent.getStringExtra("name")
        val fromLogin = intent.getBooleanExtra("fromLogin", false)

        if (fromLogin && !emailFromLogin.isNullOrEmpty()) {
            binding.editEmail.setText(emailFromLogin)
            if (!nameFromLogin.isNullOrEmpty()) {
                binding.editName.setText(nameFromLogin)
            }
            val prefsName = "UserData_${emailFromLogin.hashCode()}"
            val sharedPref = getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            val savedRole = sharedPref.getString("role", "user")
            if (savedRole == "worker") {
                binding.radioWorker.isChecked = true
                userRole = "worker"
            } else {
                binding.radioUser.isChecked = true
                userRole = "user"
            }
            Handler(Looper.getMainLooper()).postDelayed({
                getUserLocationAndProceed(nameFromLogin ?: "", emailFromLogin)
            }, 500)
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val grid = findViewById<GridLayout>(R.id.backgroundGrid)
        val root = findViewById<ScrollView>(R.id.main)

        val typedValue = TypedValue()
        theme.resolveAttribute(android.R.attr.windowBackground, typedValue, true)
        val baseColor = if (typedValue.resourceId != 0)
            ContextCompat.getColor(this, typedValue.resourceId)
        else
            typedValue.data

        grid.viewTreeObserver.addOnGlobalLayoutListener {
            if (grid.width > 0 && grid.height > 0 && grid.childCount == 0) {
                val squareSize = grid.width / numCols
                numRows = (grid.height / squareSize) + 1
                createGrid(grid, squareSize, baseColor)
            }
        }

        root.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
                handleTouch(event.x, event.y, grid, baseColor)
            }
            false
        }

        showAnimatedToast(getString(R.string.this_application_supports_english_arabic_german_and_french_languages), R.raw.recycle, 7000L)
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
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Firebase Auth
        auth = Firebase.auth
        val radioUser = findViewById<android.widget.RadioButton>(R.id.radioUser)
        val radioWorker = findViewById<android.widget.RadioButton>(R.id.radioWorker)

        radioUser.setOnClickListener { userRole = "user" }
        radioWorker.setOnClickListener { userRole = "worker" }

        binding.btnSignUp.setOnClickListener {
            val email = binding.editEmail.text.toString()
            val pass = binding.editPassword.text.toString()
            val name = binding.editName.text.toString()

            if (!binding.radioUser.isChecked && !binding.radioWorker.isChecked) {
                Toast.makeText(
                    this,
                    getString(R.string.please_select_the_account_type_first_user_or_worker),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            when {
                email.isBlank() || pass.isBlank() || name.isBlank() ->
                    Toast.makeText(this, getString(R.string.missing_fields), Toast.LENGTH_SHORT).show()

                pass.length < 6 ->
                    Toast.makeText(this, getString(R.string.password_too_short), Toast.LENGTH_SHORT).show()

                else -> {
                    userRole = if (binding.radioWorker.isChecked) "worker" else "user"
                    binding.progress.isVisible = true
                    createUser(email, pass)
                }
            }
        }

        binding.alreadyUser.setOnClickListener {
            val selectedRole = when {
                binding.radioWorker.isChecked -> "worker"
                binding.radioUser.isChecked -> "user"
                else -> {
                    Toast.makeText(
                        this,
                        getString(R.string.please_select_the_account_type_first_user_or_worker),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }
            }

            val email = binding.editEmail.text.toString()
            if (email.isNotBlank()) {
                val prefsName = "UserData_${email.hashCode()}"
                val sharedPref = getSharedPreferences(prefsName, Context.MODE_PRIVATE)
                sharedPref.edit().putString("role", selectedRole).apply()
            }

            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        binding.btnGoogleSignIn.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }
    }

    private fun createGrid(grid: GridLayout, squareSize: Int, baseColor: Int) {
        for (row in 0 until numRows) {
            for (col in 0 until numCols) {
                val square = View(this)
                val params = GridLayout.LayoutParams().apply {
                    width = squareSize
                    height = squareSize
                    setMargins(2, 2, 2, 2)
                }

                val shape = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 8f
                    setColor(baseColor)
                }

                square.background = shape
                square.layoutParams = params
                grid.addView(square)
                squares.add(square)
            }
        }
    }

    private fun handleTouch(x: Float, y: Float, grid: GridLayout, baseColor: Int) {
        if (squares.isEmpty()) return

        val squareSize = (grid.getChildAt(0).layoutParams as GridLayout.LayoutParams).width + 4
        val col = (x / squareSize).toInt().coerceIn(0, numCols - 1)
        val row = (y / squareSize).toInt().coerceIn(0, numRows - 1)
        val index = row * numCols + col

        if (index in squares.indices) animateSquare(squares[index], baseColor)
    }

    private fun animateSquare(view: View, baseColor: Int) {
        val green = Color.parseColor("#00FF00")

        val animator = ValueAnimator.ofObject(ArgbEvaluator(), baseColor, green, baseColor)
        animator.duration = 500
        animator.addUpdateListener {
            (view.background as GradientDrawable).setColor(it.animatedValue as Int)
        }
        animator.start()
    }

    private fun toggleTheme() {
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 200) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                val email = binding.editEmail.text.toString()
                val name = binding.editName.text.toString()
                if (email.isNotEmpty() && name.isNotEmpty()) {
                    getUserLocationAndProceed(name, email)
                }
            } else {
                Toast.makeText(this,
                    getString(R.string.you_need_to_allow_the_website_access_to_complete_the_registration),
                    Toast.LENGTH_LONG).show()
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 200)
            }
        }
    }

    private fun createUser(email: String, pass: String) {
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val name = binding.editName.text.toString()
                    val selectedRole = userRole

                    if (selectedRole == "worker" && !isAllowedWorker(email)) {
                        Toast.makeText(
                            this,
                            getString(R.string.you_are_not_authorized_to_register_as_a_worker_only_specific_emails_are_allowed),
                            Toast.LENGTH_LONG
                        ).show()
                        auth.currentUser?.delete()
                        binding.progress.isVisible = false
                        return@addOnCompleteListener
                    }

                    val prefsName = "UserData_${email.hashCode()}"
                    val sharedPref = getSharedPreferences(prefsName, Context.MODE_PRIVATE)
                    sharedPref.edit().apply {
                        putString("user_name", name)
                        putString("user_email", email)
                        putString("role", selectedRole)
                        putBoolean("data_sent_to_backend", false)
                        apply()
                    }

                    sendEmailVerification()

                    Toast.makeText(
                        this,
                        getString(R.string.registration_successful_please_verify_your_email_before_logging_in),
                        Toast.LENGTH_LONG
                    ).show()

                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()

                } else {
                    Toast.makeText(this, task.exception?.message, Toast.LENGTH_SHORT).show()
                    binding.progress.isVisible = false
                }
            }
    }

    private fun sendEmailVerification() {
        val user = Firebase.auth.currentUser
        user?.sendEmailVerification()?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Verification email sent to ${user.email}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendCompleteUserDataToBackend(
        uid: String,
        email: String,
        name: String,
        address: String,
        role: String
    ) {
        val userData = mapOf(
            "UserID" to uid,
            "Email" to email,
            "FirstName" to name,
            "LastName" to name,
            "Address" to address,
            "Role" to role,
            "QRCode" to uid
        )

        RetrofitInstance.api.registerUser(userData)
            .enqueue(object : Callback<ResponseBody> {
                override fun onResponse(
                    call: retrofit2.Call<ResponseBody>,
                    response: retrofit2.Response<ResponseBody>
                ) {
                    if (response.isSuccessful) {
                        val responseBody = response.body()?.string()
                        android.util.Log.d("API_SUCCESS", "Response: $responseBody")

                        val prefsName = "UserData_${email.hashCode()}"
                        val sharedPref = getSharedPreferences(prefsName, Context.MODE_PRIVATE)
                        sharedPref.edit().putBoolean("data_sent_to_backend", true).apply()

                        Toast.makeText(
                            this@MainActivity,
                            getString(R.string.data_sent_successfully),
                            Toast.LENGTH_SHORT
                        ).show()

                        auth.signOut()
                        Toast.makeText(
                            this@MainActivity,
                            "Please verify your email before logging in. Verification email sent to $email",
                            Toast.LENGTH_LONG
                        ).show()

                        startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                        finish()
                    } else {
                        val errorBody = response.errorBody()?.string()
                        android.util.Log.e("API_ERROR", "Error: ${response.code()} - $errorBody")

                        Toast.makeText(
                            this@MainActivity,
                            "Error: ${response.code()} - $errorBody",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    binding.progress.isVisible = false
                }

                override fun onFailure(call: retrofit2.Call<ResponseBody>, t: Throwable) {
                    android.util.Log.e("API_FAILURE", "Failure: ${t.message}")
                    Toast.makeText(
                        this@MainActivity,
                        "Failed: ${t.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    binding.progress.isVisible = false
                }
            })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Toast.makeText(this, getString(R.string.google_sign_in_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                val account = GoogleSignIn.getLastSignedInAccount(this)
                val name = account?.displayName ?: ""
                val email = account?.email ?: ""

                val prefsName = "UserData_${email.hashCode()}"
                val sharedPref = getSharedPreferences(prefsName, Context.MODE_PRIVATE)

                AlertDialog.Builder(this)
                    .setTitle(getString(R.string.select_account_type))
                    .setMessage(getString(R.string.are_you_worker_or_user))
                    .setPositiveButton(getString(R.string.worker)) { _, _ ->
                        if (!isAllowedWorker(email)) {
                            Toast.makeText(
                                this,
                                getString(R.string.you_are_not_authorized_to_login_as_a_worker_only_specific_emails_are_allowed),
                                Toast.LENGTH_LONG
                            ).show()
                            auth.signOut()
                            return@setPositiveButton
                        }
                        sharedPref.edit().putString("role", "worker").apply()
                        getUserLocationAndProceed(name, email)
                    }
                    .setNegativeButton(getString(R.string.user)) { _, _ ->
                        sharedPref.edit().putString("role", "user").apply()
                        getUserLocationAndProceed(name, email)
                    }
                    .setCancelable(false)
                    .show()

                showAnimatedToast(getString(R.string.logged_in_successfully), R.raw.recycle)

                Handler(Looper.getMainLooper()).postDelayed({
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                }, 3000)
            } else {
                Toast.makeText(this, getString(R.string.authentication_failed), Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun getUserLocationAndProceed(name: String? = null, email: String? = null) {
        if (email != null) {
            val prefsName = "UserData_${email.hashCode()}"
            val sharedPref = getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            if (sharedPref.getBoolean("data_sent_to_backend", false)) {
                proceedToHomeScreen(email)
                return
            }
        }

        val dialog = AlertDialog.Builder(this)
            .setCancelable(false)
            .setTitle(getString(R.string.locate_you))
            .setMessage(getString(R.string.we_are_currently_determining_your_precise_location_please_wait))
            .create()
        dialog.show()

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                200
            )
            dialog.dismiss()
            return
        }

        val locationRequest = com.google.android.gms.location.LocationRequest.Builder(
            com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
            5000
        ).apply {
            setMinUpdateIntervalMillis(2000)
            setMaxUpdateDelayMillis(10000)
        }.build()

        var locationReceived = false

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            object : com.google.android.gms.location.LocationCallback() {
                override fun onLocationResult(locationResult: com.google.android.gms.location.LocationResult) {
                    if (locationReceived) return

                    val location = locationResult.lastLocation
                    if (location != null && email != null && name != null) {
                        locationReceived = true
                        fusedLocationClient.removeLocationUpdates(this)
                        dialog.dismiss()

                        val geocoder = Geocoder(this@MainActivity, Locale.getDefault())
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
                            "Latitude: ${location.latitude}, Longitude: ${location.longitude}"
                        }

                        val uid = Firebase.auth.currentUser?.uid ?: ""

                        val prefsName = "UserData_${email.hashCode()}"
                        val sharedPref = getSharedPreferences(prefsName, Context.MODE_PRIVATE)
                        val role = sharedPref.getString("role", "user") ?: "user"

                        sharedPref.edit().apply {
                            putString("latitude", location.latitude.toString())
                            putString("longitude", location.longitude.toString())
                            putString("address", address)
                            apply()
                        }

                        sendCompleteUserDataToBackend(uid, email, name, address, role)
                        proceedToHomeScreen(email)
                    }
                }
            },
            Looper.getMainLooper()
        )

        // ✅ Timeout after 12 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            if (!locationReceived) {
                try {
                    fusedLocationClient.removeLocationUpdates(
                        object : com.google.android.gms.location.LocationCallback() {}
                    )
                    dialog.dismiss()

                    // Try last known location as fallback
                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                        if (location != null && email != null && name != null) {
                            val address = "Lat: ${location.latitude}, Lon: ${location.longitude}"
                            val uid = Firebase.auth.currentUser?.uid ?: ""
                            val prefsName = "UserData_${email.hashCode()}"
                            val sharedPref = getSharedPreferences(prefsName, Context.MODE_PRIVATE)
                            val role = sharedPref.getString("role", "user") ?: "user"

                            sharedPref.edit().apply {
                                putString("latitude", location.latitude.toString())
                                putString("longitude", location.longitude.toString())
                                putString("address", address)
                                apply()
                            }

                            sendCompleteUserDataToBackend(uid, email, name, address, role)
                            proceedToHomeScreen(email)
                        } else if (email != null && name != null) {
                            // No location at all - proceed without it
                            val address = "Location not available"
                            val uid = Firebase.auth.currentUser?.uid ?: ""
                            val prefsName = "UserData_${email.hashCode()}"
                            val sharedPref = getSharedPreferences(prefsName, Context.MODE_PRIVATE)
                            val role = sharedPref.getString("role", "user") ?: "user"

                            sendCompleteUserDataToBackend(uid, email, name, address, role)
                            proceedToHomeScreen(email)
                        }
                    }
                } catch (e: Exception) {
                    dialog.dismiss()
                    if (email != null && name != null) {
                        val address = "Location not available"
                        val uid = Firebase.auth.currentUser?.uid ?: ""
                        val prefsName = "UserData_${email.hashCode()}"
                        val sharedPref = getSharedPreferences(prefsName, Context.MODE_PRIVATE)
                        val role = sharedPref.getString("role", "user") ?: "user"

                        sendCompleteUserDataToBackend(uid, email, name, address, role)
                        proceedToHomeScreen(email)
                    }
                }
            }
        }, 12000)
    }

    private fun proceedToHomeScreen(email: String) {
        val prefsName = "UserData_${email.hashCode()}"
        val sharedPref = getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val role = sharedPref.getString("role", "user") ?: "user"

        if (role == "worker" || role == "staff") {
            if (!isAllowedWorker(email)) {
                Toast.makeText(
                    this,
                    getString(R.string.you_are_not_authorized_to_access_worker_panel_please_contact_support),
                    Toast.LENGTH_LONG
                ).show()
                auth.signOut()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
                return
            }
            startActivity(Intent(this, WorkerHomeActivity::class.java))
        } else {
            startActivity(Intent(this, HomeActivity::class.java))
        }
        finish()
    }

    override fun onStart() {
        super.onStart()
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val email = currentUser.email ?: ""
            val prefsName = "UserData_${email.hashCode()}"
            val sharedPref = getSharedPreferences(prefsName, Context.MODE_PRIVATE)

            if (!sharedPref.getBoolean("data_sent_to_backend", false) && email.isNotEmpty()) {
                val uid = currentUser.uid
                val name = sharedPref.getString("user_name", "") ?: ""
                val address = sharedPref.getString("address", "") ?: ""
                val role = sharedPref.getString("role", "user") ?: "user"

                if (name.isNotEmpty() && address.isNotEmpty()) {
                    sendCompleteUserDataToBackend(uid, email, name, address, role)
                }
            }
            proceedToHomeScreen(email)
        }
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

    override fun onBackPressed() {
        val exit = Exit()
        exit.isCancelable = false
        exit.show(supportFragmentManager, null)
    }
}
