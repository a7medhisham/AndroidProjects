package com.example.mycv

import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.GravityCompat
import com.example.mycv.databinding.ActivityMainBinding
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.navView.setNavigationItemSelectedListener(this)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ProfileFragment())
                .commit()
            binding.navView.setCheckedItem(R.id.nav_profile)
        }

        Toast.makeText(this, getString(R.string.welcome), Toast.LENGTH_SHORT).show()

        val sharedPref = getSharedPreferences("settings", MODE_PRIVATE)
        val darkMode = sharedPref.getBoolean("dark_mode", false)
        if (darkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        updateThemeButton()

        binding.themeToggleButton.setOnClickListener {
            toggleTheme()
        }

        binding.root.alpha = 0f
        binding.root.animate().alpha(1f).setDuration(1500).start()
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

    private fun updateThemeButton() {
        val sharedPref = getSharedPreferences("settings", MODE_PRIVATE)
        val darkMode = sharedPref.getBoolean("dark_mode", false)

        if (darkMode) {
            binding.themeToggleButton.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.sun, 0, 0, 0
            )
        } else {
            binding.themeToggleButton.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.iosmoon, 0, 0, 0
            )
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    binding.drawerLayout.openDrawer(GravityCompat.START)
                }
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_profile -> {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, ProfileFragment())
                    .commit()
            }
            R.id.nav_summary -> {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, SummaryFragment())
                    .commit()
            }
            R.id.nav_education -> {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, EducationFragment())
                    .commit()
            }
            R.id.nav_skills -> {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, SkillsFragment())
                    .commit()
            }
            R.id.nav_projects -> {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, ProjectsFragment())
                    .commit()
            }
            R.id.nav_courses -> {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, CoursesFragment())
                    .commit()
            }
            R.id.nav_soft_skills -> {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, SoftSkillsFragment())
                    .commit()
            }
            R.id.nav_languages -> {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, LanguagesFragment())
                    .commit()
            }
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        binding.navView.setCheckedItem(item.itemId)
        return true
    }
}