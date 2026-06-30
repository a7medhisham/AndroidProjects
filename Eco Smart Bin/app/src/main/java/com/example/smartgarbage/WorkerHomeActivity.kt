package com.example.smartgarbage

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.smartgarbage.databinding.ActivityWorkerHomeBinding
import com.google.firebase.auth.FirebaseAuth

class WorkerHomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityWorkerHomeBinding
    private lateinit var auth: FirebaseAuth
    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWorkerHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = FirebaseAuth.getInstance()
        val user = auth.currentUser

        val userEmail = user?.email
        binding.txtEmail.text = userEmail ?: getString(R.string.unknown)

        loadUserDataFromPrefs(userEmail)
        loadProfileImage(userEmail)

        binding.image.setOnClickListener { showImageOptionsDialog() }

        binding.logoutBtn.setOnClickListener { logoutUser() }

        binding.noteBtn.setOnClickListener {
            val i = Intent(this, NoteStaffActivity::class.java)
            // ✅ منع إغلاق الصفحة الحالية
            i.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            startActivity(i)
            // ✅ ممنوع استخدام finish() هنا
        }
    }

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                imageUri = uri
                try {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: SecurityException) {
                    e.printStackTrace()
                }
                binding.image.setImageURI(uri)
                saveProfileImage(uri)
            }
        }

    private fun loadUserDataFromPrefs(currentEmail: String?) {
        val prefsName = if (currentEmail != null) "UserData_${currentEmail.hashCode()}" else "UserData"
        val userData = getSharedPreferences(prefsName, Context.MODE_PRIVATE)

        val savedName = userData.getString("user_name", null)
        val email = userData.getString("user_email", currentEmail)
        val latitude = userData.getString("latitude", null)
        val longitude = userData.getString("longitude", null)
        val city = userData.getString("city", null)
        val country = userData.getString("country", null)
        val address = userData.getString("address", null)

        binding.txtName.text = savedName ?: getString(R.string.unknown_user)

        binding.txtAddress.text = when {
            !address.isNullOrEmpty() -> address
            city != null && country != null -> getString(R.string.city_country_format, city, country)
            latitude != null && longitude != null -> getString(R.string.lat_lon_format, latitude, longitude)
            else -> getString(R.string.location_not_available)
        }

        binding.txtEmail.text = email ?: getString(R.string.unknown)
    }

    private fun saveProfileImage(uri: Uri) {
        val email = auth.currentUser?.email
        val prefsName = if (email != null) "UserPrefs_${email.hashCode()}" else "UserPrefs"
        val sharedPref = getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("profileImageUri", uri.toString())
            apply()
        }
    }

    private fun loadProfileImage(email: String?) {
        val prefsName = if (email != null) "UserPrefs_${email.hashCode()}" else "UserPrefs"
        val sharedPref = getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val savedUri = sharedPref.getString("profileImageUri", null)

        if (savedUri != null) {
            binding.image.setImageURI(Uri.parse(savedUri))
        } else {
            binding.image.setImageResource(R.drawable.ic_personal)
        }
    }

    private fun deleteProfileImage() {
        val email = auth.currentUser?.email
        val prefsName = if (email != null) "UserPrefs_${email.hashCode()}" else "UserPrefs"
        val sharedPref = getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            remove("profileImageUri")
            apply()
        }

        binding.image.setImageResource(R.drawable.ic_personal)
        Toast.makeText(this, getString(R.string.profile_image_deleted), Toast.LENGTH_SHORT).show()
    }

    private fun showImageOptionsDialog() {
        val builder = AlertDialog.Builder(this)
        val options = arrayOf(
            getString(R.string.choose_new_image),
            getString(R.string.delete_current_image)
        )

        builder.setTitle(getString(R.string.profile_image_options))
        builder.setItems(options) { dialog, which ->
            when (which) {
                0 -> pickImageLauncher.launch(arrayOf("image/*"))
                1 -> deleteProfileImage()
            }
            dialog.dismiss()
        }
        builder.show()
    }

    private fun logoutUser() {
        auth.signOut()
        Toast.makeText(this, getString(R.string.logged_out_successfully), Toast.LENGTH_SHORT).show()
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}