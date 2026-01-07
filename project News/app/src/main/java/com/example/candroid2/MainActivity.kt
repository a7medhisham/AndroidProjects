package com.example.candroid2

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.example.candroid2.databinding.ActivityMainBinding
import com.google.android.gms.ads.AdRequest
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class MainActivity : AppCompatActivity() {
    @SuppressLint("WrongViewCast")
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //
        //للتاكد من لينك نذهب الي موقع json formatter
        loadNews()
        binding.swipeRefresh.setOnRefreshListener {
            loadNews()
        }
        val adRequest = AdRequest.Builder().build()
        binding.bannerAdView.loadAd(adRequest)
    }

    private fun loadNews() {
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val code = prefs.getString("code", "us")!!
        val cat = intent.getStringExtra("cat")!!
        val logging = HttpLoggingInterceptor()
        logging.setLevel(HttpLoggingInterceptor.Level.BASIC)
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
        val retrofit = Retrofit
            .Builder()
            .baseUrl("https://newsapi.org")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val c = retrofit.create(NewsCallable::class.java)
        c.getNews(cat, code).enqueue(object : Callback<News> {
            override fun onResponse(p0: Call<News>, p1: Response<News>) {
                val news = p1.body()
                val articles = news?.articles!!
                articles.removeAll {
                    it.title == "[Removed]"
                }
                Log.d("trace", "Article: $articles")
                showNews(articles)
                binding.progress.isVisible = false
                binding.swipeRefresh.isRefreshing = false
            }

            override fun onFailure(p0: Call<News>, p1: Throwable) {
                Log.d("trace", "Error: ${p1.message}")
                binding.progress.isVisible = false
                binding.swipeRefresh.isRefreshing = false
            }
        })
    }

    private fun showNews(articles: ArrayList<Article>) {
        val adpater = NewsAdpater(this, articles)
        binding.newsList.adapter = adpater

    }
}