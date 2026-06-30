package com.example.smartgarbage

import com.google.gson.annotations.SerializedName

data class AlertItem(
    @SerializedName("alertID")
    val alertId: Int,

    @SerializedName("binID")
    val binId: Int,

    @SerializedName("wasteTypeID")
    val wasteTypeId: Int,

    @SerializedName("status")
    val status: String,

    @SerializedName("createdAt")
    val createdAt: String,

    @SerializedName("resolvedAt")
    val resolvedAt: String? = null,

    @SerializedName("resolvedBy")
    val resolvedBy: String? = null
)