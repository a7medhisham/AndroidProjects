package com.example.smartgarbage

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.smartgarbage.databinding.ActivityHomeBinding
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import android.Manifest
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
            }
        }

        showAnimatedToast("Welcome user 🎉", R.raw.recycle, 7000L)

        binding.root.alpha = 0f
        binding.root.animate().alpha(1f).setDuration(1500).start()

        auth = FirebaseAuth.getInstance()

        setupViewPagerWithTabs()
    }

    private fun setupViewPagerWithTabs() {
        val adapter = ViewPagerAdapter(this)
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            when (position) {
                0 -> tab.icon = getDrawable(R.drawable.icon_notification)
                1 -> tab.icon = getDrawable(R.drawable.ic_alert)
                2 -> tab.icon = getDrawable(R.drawable.icon_nearby)
                3 -> tab.icon = getDrawable(R.drawable.icon_points)
                4 -> tab.icon = getDrawable(R.drawable.icon_history)
                5 -> tab.icon = getDrawable(R.drawable.icon_extra)
                6 -> tab.icon = getDrawable(R.drawable.ic_personal)
            }
        }.attach()

        binding.tabLayout.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab) { }
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab) { }
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab) { }
        })

        updateSettingsTabIcon()
    }

    fun updateSettingsTabIcon() {
        val tab = binding.tabLayout.getTabAt(6) ?: return
        val customView = layoutInflater.inflate(R.layout.tab_profile, null)
        val profileImage = customView.findViewById<ImageView>(R.id.profileTabImage)

        val userEmail = FirebaseAuth.getInstance().currentUser?.email
        val prefsName = if (userEmail != null) "UserPrefs_${userEmail.hashCode()}" else "UserPrefs"

        val sharedPref = getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val savedUri = sharedPref.getString("profileImageUri", null)

        if (savedUri != null && savedUri.isNotEmpty()) {
            try {
                Glide.with(this)
                    .load(Uri.parse(savedUri))
                    .apply(RequestOptions.bitmapTransform(CircleCrop()))
                    .placeholder(R.drawable.ic_personal)
                    .error(R.drawable.ic_personal)
                    .into(profileImage)
            } catch (e: Exception) {
                profileImage.setImageResource(R.drawable.ic_personal)
            }
        } else {
            profileImage.setImageResource(R.drawable.ic_personal)
        }

        tab.customView = customView
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