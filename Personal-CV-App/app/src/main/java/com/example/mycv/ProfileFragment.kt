package com.example.mycv

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.mycv.databinding.FragmentProfileBinding

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var sharedPreferences: SharedPreferences

    companion object {
        private const val PREFS_NAME = "MyCVPrefs"
        private const val KEY_NAME = "name"
        private const val KEY_TITLE = "title"
        private const val KEY_EMAIL = "email"
        private const val KEY_LOCATION = "location"
        private const val KEY_LINKEDIN = "linkedin"
        private const val KEY_GITHUB = "github"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)

        loadProfileData()
        setupClickListeners()
        setupEditMode()
    }

    private fun loadProfileData() {
        if (!sharedPreferences.contains(KEY_NAME)) {
            setDefaultData()
        }

        binding.textName.text = sharedPreferences.getString(KEY_NAME, "Your Name")
        binding.textTitle.text = sharedPreferences.getString(KEY_TITLE, "Your Title")
        binding.textLocation.text = sharedPreferences.getString(KEY_LOCATION, "Your Location")
        binding.textEmail.text = sharedPreferences.getString(KEY_EMAIL, "your.email@example.com")
    }

    private fun setDefaultData() {
        val editor = sharedPreferences.edit()
        editor.apply {
            putString(KEY_NAME, "Ahmed Hisham Ezzat")
            putString(KEY_TITLE, "Junior Android Developer")
            putString(KEY_EMAIL, "ahmed.dev@gmail.com")
            putString(KEY_LOCATION, "Cairo, Egypt")
            putString(KEY_LINKEDIN, "https://www.linkedin.com/in/ahmed-hisham-8b78832a6/")
            putString(KEY_GITHUB, "https://github.com/a7medhisham")
            apply()
        }
    }

    private fun setupClickListeners() {
        binding.linkedinLayout.setOnClickListener {
            openUrl(sharedPreferences.getString(KEY_LINKEDIN, "https://linkedin.com") ?: "")
        }

        binding.githubLayout.setOnClickListener {
            openUrl(sharedPreferences.getString(KEY_GITHUB, "https://github.com") ?: "")
        }

        binding.textEmail.setOnClickListener {
            val email = sharedPreferences.getString(KEY_EMAIL, "")
            if (!email.isNullOrEmpty()) {
                sendEmail(email)
            }
        }
    }

    private fun setupEditMode() {
        binding.textName.setOnLongClickListener {
            showEditDialog("Name", KEY_NAME, binding.textName.text.toString())
            true
        }

        binding.textTitle.setOnLongClickListener {
            showEditDialog("Title", KEY_TITLE, binding.textTitle.text.toString())
            true
        }

        binding.textLocation.setOnLongClickListener {
            showEditDialog("Location", KEY_LOCATION, binding.textLocation.text.toString())
            true
        }

        binding.textEmail.setOnLongClickListener {
            showEditDialog("Email", KEY_EMAIL, binding.textEmail.text.toString())
            true
        }

        binding.linkedinLayout.setOnLongClickListener {
            val current = sharedPreferences.getString(KEY_LINKEDIN, "https://linkedin.com")
            showEditDialog("LinkedIn URL", KEY_LINKEDIN, current ?: "")
            true
        }

        binding.githubLayout.setOnLongClickListener {
            val current = sharedPreferences.getString(KEY_GITHUB, "https://github.com")
            showEditDialog("GitHub URL", KEY_GITHUB, current ?: "")
            true
        }
    }

    private fun showEditDialog(title: String, key: String, currentValue: String) {
        val input = android.widget.EditText(requireContext())
        input.setText(currentValue)

        android.app.AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.edit, title))
            .setView(input)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val newValue = input.text.toString().trim()
                if (newValue.isNotEmpty()) {
                    sharedPreferences.edit().putString(key, newValue).apply()
                    loadProfileData()
                    Toast.makeText(requireContext(), getString(R.string.saved), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            Toast.makeText(requireContext(),
                getString(R.string.cannot_open_link), Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendEmail(email: String) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:$email")
                putExtra(Intent.EXTRA_SUBJECT, "Contact from MyCV App")
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(),
                getString(R.string.no_email_app_found), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}