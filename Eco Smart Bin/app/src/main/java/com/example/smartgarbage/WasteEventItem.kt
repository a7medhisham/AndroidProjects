package com.example.smartgarbage

import com.google.gson.annotations.SerializedName

data class WasteEventItem(
    @SerializedName("wasteEventId")
    val id: Int,

    @SerializedName("userID")
    val userId: String,

    @SerializedName("binID")
    val binId: Int,

    @SerializedName("wasteTypeID")
    val wasteTypeId: Int,

    @SerializedName("weight")
    val weight: Double,

    @SerializedName("pointsAwarded")
    val points: Int,

    @SerializedName("dateTime")
    val date: String
)