package com.example.movies

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.movies.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
class ProfileFragment : Fragment() {

        private var _binding: FragmentProfileBinding? = null
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

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
            _binding = FragmentProfileBinding.inflate(inflater, container, false)
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

            binding.image.setOnClickListener { showImageOptionsDialog() }

            binding.logoutBtn.setOnClickListener {
                logoutUser()
            }

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
        }

        private fun loadUserDataFromPrefs(currentEmail: String?) {
            val prefsName = if (currentEmail != null) "UserData_${currentEmail.hashCode()}" else "UserData"
            val userData = requireContext().getSharedPreferences(prefsName, Context.MODE_PRIVATE)

            val savedName = userData.getString("user_name", null)
            val email = userData.getString("user_email", currentEmail)
            binding.txtName.text = savedName ?: "Unknown User"
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
            Toast.makeText(requireContext(),
                getString(R.string.profile_image_deleted), Toast.LENGTH_SHORT).show()
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
            Toast.makeText(requireContext(),
                getString(R.string.logged_out_successfully), Toast.LENGTH_SHORT).show()
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
