package com.example.smartgarbage

import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.smartgarbage.databinding.FragmentSettingBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import okhttp3.ResponseBody


class SettingFragment : Fragment() {

    private var _binding: FragmentSettingBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private var imageUri: Uri? = null

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                imageUri = uri
                try {
                    requireContext().contentResolver.takePersistableUriPermission(
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

    private val scanLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            val scannedData = result.contents
            Toast.makeText(requireContext(), "Scanned: $scannedData", Toast.LENGTH_LONG).show()

            try {
                var url = scannedData
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    url = "https://$url"
                }

                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Cannot open link: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(requireContext(), getString(R.string.scan_cancelled), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        val user = auth.currentUser

        val userEmail = user?.email
        binding.txtEmail.text = userEmail ?: getString(R.string.unknown)

        loadUserDataFromPrefs(userEmail)
        loadProfileImage(userEmail)
        loadPointsFromPrefs()
        loadPointsFromServer()

        binding.image.setOnClickListener { showImageOptionsDialog() }

        binding.logoutBtn.setOnClickListener { logoutUser() }

        generateUserQrAndShow()

        binding.deleteBtn.setOnClickListener {
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle(getString(R.string.delete_account))
            builder.setMessage(getString(R.string.are_you_sure_you_want_to_delete_your_account_this_action_cannot_be_undone))
            builder.setPositiveButton(getString(R.string.delete)) { dialog, _ ->
                deleteUserAccount()
                dialog.dismiss()
            }
            builder.setNegativeButton(getString(R.string.cancel)) { dialog, _ -> dialog.dismiss() }
            builder.show()
        }

        binding.scanQrBtn.setOnClickListener {
            startQrScan()
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == "POINTS_UPDATED") {
                    val newPoints = intent.getIntExtra("new_points", 0)
                    binding.txtPoints.text = newPoints.toString()
                    savePointsToPrefs(newPoints)
                }
            }
        }

        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
            receiver, IntentFilter("POINTS_UPDATED")
        )
    }

    private fun startQrScan() {
        val options = ScanOptions()
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
        options.setPrompt(getString(R.string.scan_qr_prompt))
        options.setCameraId(0)
        options.setBeepEnabled(true)
        options.setBarcodeImageEnabled(true)
        options.setOrientationLocked(false)
        scanLauncher.launch(options)
    }

    override fun onResume() {
        super.onResume()
        loadPointsFromPrefs()
    }

    private fun loadPointsFromPrefs() {
        val email = auth.currentUser?.email
        val prefsName = if (email != null) "UserData_${email.hashCode()}" else "UserData"
        val sharedPref = requireContext().getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val points = sharedPref.getInt("user_points", 0)
        binding.txtPoints.text = points.toString()
    }

    private fun savePointsToPrefs(points: Int) {
        val email = auth.currentUser?.email
        val prefsName = if (email != null) "UserData_${email.hashCode()}" else "UserData"
        val sharedPref = requireContext().getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putInt("user_points", points)
            apply()
        }
    }

    private fun generateUserQrAndShow() {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrEmpty()) {
            binding.imgQr.setImageResource(R.drawable.ic_personal)
            return
        }

        try {
            val barcodeEncoder = BarcodeEncoder()
            val bitmap = barcodeEncoder.encodeBitmap(uid, BarcodeFormat.QR_CODE, 400, 400)
            binding.imgQr.setImageBitmap(bitmap)
            saveQrInPrefs(uid)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), getString(R.string.qr_generation_error, e.message), Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveQrInPrefs(qr: String) {
        val email = auth.currentUser?.email
        val prefsName = if (email != null) "UserPrefs_${email.hashCode()}" else "UserPrefs"
        val sharedPref = requireContext().getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("user_qr", qr)
            apply()
        }
    }

    private fun loadUserDataFromPrefs(currentEmail: String?) {
        val prefsName = if (currentEmail != null) "UserData_${currentEmail.hashCode()}" else "UserData"
        val userData = requireContext().getSharedPreferences(prefsName, Context.MODE_PRIVATE)

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
        val sharedPref = requireContext().getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("profileImageUri", uri.toString())
            apply()
        }
        (activity as? HomeActivity)?.updateSettingsTabIcon()
    }

    private fun loadProfileImage(email: String?) {
        val prefsName = if (email != null) "UserPrefs_${email.hashCode()}" else "UserPrefs"
        val sharedPref = requireContext().getSharedPreferences(prefsName, Context.MODE_PRIVATE)
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
        val sharedPref = requireContext().getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            remove("profileImageUri")
            apply()
        }

        binding.image.setImageResource(R.drawable.ic_personal)
        (activity as? HomeActivity)?.updateSettingsTabIcon()
        Toast.makeText(requireContext(), getString(R.string.profile_image_deleted), Toast.LENGTH_SHORT).show()
    }

    private fun showImageOptionsDialog() {
        val builder = AlertDialog.Builder(requireContext())
        val options = arrayOf(getString(R.string.choose_new_image), getString(R.string.delete_current_image))

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
        Toast.makeText(requireContext(), getString(R.string.logged_out_successfully), Toast.LENGTH_SHORT).show()
        val intent = Intent(requireContext(), MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    private fun deleteUserAccount() {
        val user = auth.currentUser
        if (user != null) {
            user.delete().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val email = user.email
                    val prefsName = if (email != null) "UserData_${email.hashCode()}" else "UserData"
                    val sharedPref = requireContext().getSharedPreferences(prefsName, Context.MODE_PRIVATE)
                    sharedPref.edit().clear().apply()

                    val profilePrefsName = if (email != null) "UserPrefs_${email.hashCode()}" else "UserPrefs"
                    val profilePref = requireContext().getSharedPreferences(profilePrefsName, Context.MODE_PRIVATE)
                    profilePref.edit().clear().apply()

                    Toast.makeText(requireContext(),
                        getString(R.string.account_deleted_successfully), Toast.LENGTH_SHORT).show()

                    val intent = Intent(requireContext(), MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    requireActivity().finish()
                } else {
                    Toast.makeText(requireContext(),
                        getString(R.string.failed_to_delete_account, task.exception?.message), Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(requireContext(),
                getString(R.string.no_user_logged_in), Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadPointsFromServer() {
        val userId = auth.currentUser?.uid ?: return
        val email = auth.currentUser?.email ?: return
        val prefsName = "UserData_${email.hashCode()}"
        val sharedPref = requireContext().getSharedPreferences(prefsName, Context.MODE_PRIVATE)

        RetrofitInstance.api.getUserPoints(userId)
            .enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if (response.isSuccessful) {
                        val rawResponse = response.body()?.string() ?: return
                        val pointsFromServer = org.json.JSONObject(rawResponse).getInt("points")

                        val lastServerPoints = sharedPref.getInt("last_server_points", -1)
                        val localPoints = sharedPref.getInt("user_points", 0)

                        if (lastServerPoints == -1) {
                            sharedPref.edit()
                                .putInt("user_points", pointsFromServer)
                                .putInt("last_server_points", pointsFromServer)
                                .apply()
                            binding.txtPoints.text = pointsFromServer.toString()
                        } else if (pointsFromServer > lastServerPoints) {
                            val diff = pointsFromServer - lastServerPoints
                            val newLocal = localPoints + diff
                            sharedPref.edit()
                                .putInt("user_points", newLocal)
                                .putInt("last_server_points", pointsFromServer)
                                .apply()
                            binding.txtPoints.text = newLocal.toString()
                        }
                    }
                }
                override fun onFailure(call: Call<ResponseBody>, t: Throwable) { }
            })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}