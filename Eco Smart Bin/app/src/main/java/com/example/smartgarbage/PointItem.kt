package com.example.smartgarbage

import com.google.gson.annotations.SerializedName

data class PointItem(
    @SerializedName("transactionID")
    val id: Int,

    @SerializedName("userID")
    val userId: String,

    @SerializedName("binID")
    val binId: Int,

    @SerializedName("pointsRedeemed")
    val pointsRedeemed: Int,

    @SerializedName("dateTime")
    val date: String,

    val amount: Double = pointsRedeemed * 0.25
)