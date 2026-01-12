package com.example.candroid2

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.candroid2.databinding.ActivityHomeBinding
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import retrofit2.http.Tag

class homeActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private var mInterstitialAd: InterstitialAd? = null
    private final val TAG = "MainActivity"
    private var category = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        auth = Firebase.auth
        MobileAds.initialize(this){}
        loadAD()
        binding.sportsBtn.setOnClickListener {
            category = "sports"
            showAD()
        }
        binding.techBtn.setOnClickListener {
            category = "technology"
            showAD()
        }
        binding.scienceBtn.setOnClickListener {
            category = "science"
           showAD()
        }
        binding.healthBtn.setOnClickListener {
            category = "health"
            showAD()
        }
        binding.entertainmentBtn.setOnClickListener {
            category = "entertainment"
            showAD()
        }
        binding.businessBtn.setOnClickListener {
            category = "business"
            showAD()
        }
        binding.LogoutBtn.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this,loginActivity::class.java))
            finish()
        }

    }

    private fun openNewsPage() {
        val i = Intent(this, MainActivity::class.java)
        i.putExtra("cat", category)
        startActivity(i)
    }
    private fun loadAD(){

        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(this,"ca-app-pub-3940256099942544/1033173712", adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    //Log.d(TAG, adError?.toString())
                    mInterstitialAd = null
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    //Log.d(TAG, 'Ad was loaded.')
                    mInterstitialAd = interstitialAd
                }
            })
    }
    private fun showAD(){
        if (mInterstitialAd != null) {
            mInterstitialAd?.show(this)
            mInterstitialAd?.fullScreenContentCallback = object :FullScreenContentCallback(){
                override fun onAdDismissedFullScreenContent() {
                    //called when ad is dismissed.
                    Log.d(TAG,"Ad dismissed fullscreen content." )
                    mInterstitialAd =null
                    openNewsPage()
                }

                override fun onAdFailedToShowFullScreenContent(adError:AdError) {
                    //called when ad fails to show.
                    Log.d(TAG,"Ad failed to show fullscreen content." )
                    mInterstitialAd=null
                    openNewsPage()
                }
            }

        } else {
            openNewsPage()
           // Log.d("TAG", "The interstitial ad wasn't ready yet.")
        }
    }

}