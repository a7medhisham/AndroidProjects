package com.example.movies

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.NavHostFragment
import com.example.movies.databinding.ActivityHomeBinding
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.shape.CornerFamily
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private lateinit var auth: FirebaseAuth
    private val TAB_ICON_SIZE_DP = 24

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

        Toast.makeText(this, getString(R.string.welcome), Toast.LENGTH_SHORT).show()
        binding.root.alpha = 0f
        binding.root.animate().alpha(1f).setDuration(1500).start()

        auth = FirebaseAuth.getInstance()
        setupViewPagerWithTabs()
    }

    private fun setupViewPagerWithTabs() {
        val adapter = SliderAdapter(this)
        binding.viewPager.adapter = adapter
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            when (position) {
                0 -> tab.setIcon(R.drawable.video_player)
                1 -> tab.setIcon(R.drawable.favorites)
                2 -> tab.setIcon(R.drawable.ic_personal)
            }
        }.attach()
        updateSettingsTabIcon()
    }
    private fun com.google.android.material.tabs.TabLayout.Tab.setIcon(iconResId: Int) {
        val icon = getDrawable(iconResId)
        icon?.let {
            val sizeInPixels = (TAB_ICON_SIZE_DP * resources.displayMetrics.density).toInt()
            it.setBounds(0, 0, sizeInPixels, sizeInPixels)
            this.icon = it
        }
    }

    fun navigateToMovieDetails(movieId: Int) {
        binding.viewPager.visibility = View.GONE
        binding.topBar.visibility = View.GONE

        binding.navHostFragment.visibility = View.VISIBLE

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
            ?: return

        val navController = navHostFragment.navController

        if (navController.graph == null || navController.graph.id == 0) {
            navController.setGraph(R.navigation.nav_graph)
        }

        val bundle = Bundle().apply {
            putInt("movie_id", movieId)
        }
        navController.navigate(R.id.movieDetailsFragment, bundle)
    }

    fun navigateBackToHome() {
        binding.navHostFragment.visibility = View.GONE

        binding.viewPager.visibility = View.VISIBLE
        binding.topBar.visibility = View.VISIBLE

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
        navHostFragment?.navController?.popBackStack(R.id.movieDetailsFragment, true)
    }

    override fun onBackPressed() {
        if (binding.navHostFragment.visibility == View.VISIBLE) {
            navigateBackToHome()
        } else {
            val exit = Exit()
            exit.isCancelable = false
            exit.show(supportFragmentManager, null)
        }
    }

    fun updateSettingsTabIcon() {
        val tab = binding.tabLayout.getTabAt(2) ?: return
        val profileBitmap = getProfileBitmap()
        val sizeInPixels = (TAB_ICON_SIZE_DP * resources.displayMetrics.density).toInt()

        val imageView = ShapeableImageView(this)
        imageView.layoutParams = ViewGroup.LayoutParams(sizeInPixels, sizeInPixels)
        imageView.scaleType = ImageView.ScaleType.CENTER_CROP
        imageView.shapeAppearanceModel = imageView.shapeAppearanceModel.toBuilder()
            .setAllCorners(CornerFamily.ROUNDED, sizeInPixels / 2f)
            .build()

        if (profileBitmap != null) {
            val roundedBitmap = getRoundedBitmap(profileBitmap)
            val scaledBitmap = Bitmap.createScaledBitmap(roundedBitmap, sizeInPixels, sizeInPixels, true)
            imageView.setImageBitmap(scaledBitmap)
        } else {
            imageView.setImageResource(R.drawable.ic_personal)
        }

        tab.customView = imageView
    }

    private fun getProfileBitmap(): Bitmap? {
        val userEmail = FirebaseAuth.getInstance().currentUser?.email
        val prefsName = if (userEmail != null) "UserPrefs_${userEmail.hashCode()}" else "UserPrefs"
        val sharedPref = getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val savedUri = sharedPref.getString("profileImageUri", null)

        if (savedUri != null) {
            return try {
                val inputStream = contentResolver.openInputStream(Uri.parse(savedUri))
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                bitmap
            } catch (e: Exception) {
                null
            }
        }
        return null
    }

    private fun getRoundedBitmap(bitmap: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
        }

        val rect = Rect(0, 0, bitmap.width, bitmap.height)
        val radius = bitmap.width / 2f

        canvas.drawARGB(0, 0, 0, 0)
        canvas.drawCircle(radius, radius, radius, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)

        return output
    }

    fun updateFavorites() {
        val currentFragment = supportFragmentManager.fragments.find {
            it is FavoritesFragment && it.isVisible
        }

        if (currentFragment is FavoritesFragment) {
            currentFragment.loadFavorites()
        }
    }

    fun switchToTab(tabIndex: Int) {
        if (tabIndex in 0..2) {
            binding.viewPager.currentItem = tabIndex
        }
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        binding.tabLayout.postDelayed({
        }, 100)
    }
}