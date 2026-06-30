package com.example.smartgarbage

import com.google.gson.annotations.SerializedName

data class BinItem(
    @SerializedName("binID")
    val id: Int,

    @SerializedName("location")
    val location: String,

    @SerializedName("fillLevel")
    val fillLevel: Int,

    @SerializedName("status")
    val status: String,

    @SerializedName("lastUpdated")
    val lastUpdated: String? = null
)