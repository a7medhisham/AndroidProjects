package com.example.smartgarbage

import com.google.gson.annotations.SerializedName

data class UserNotificationItem(
    @SerializedName("notificationID")
    val notificationId: Int,

    @SerializedName("userID")
    val userId: String,

    @SerializedName("transactionID")
    val transactionId: Int,

    @SerializedName("binID")
    val binId: Int,

    @SerializedName("type")
    val type: String,

    @SerializedName("message")
    val message: String,

    @SerializedName("status")
    val status: String,

    @SerializedName("sentAt")
    val sentAt: String
)