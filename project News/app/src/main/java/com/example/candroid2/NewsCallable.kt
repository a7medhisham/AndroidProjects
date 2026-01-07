package com.example.candroid2

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface NewsCallable {

    @GET("/v2/top-headlines?apiKey=f3fbe5f07be44be28a372ab241cb9ab1&pageSize=20")
    fun getNews(
        @Query("category")cat:String,
        @Query("country")code:String
    ):Call<News>
}