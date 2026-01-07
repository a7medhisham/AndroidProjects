package com.example.candroid2

import com.google.gson.annotations.SerializedName

data class News(
    val articles:ArrayList<Article>
)

data class Article(
    val title:String,
    //@SerializedName("url")
    val url:String,
    val urlToImage:String
)