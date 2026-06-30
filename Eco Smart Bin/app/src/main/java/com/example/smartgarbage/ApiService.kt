package com.example.smartgarbage

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface ApiService {

    data class FillLevelRequest(
        val binID: Int,
        val fillLevel: Int
    )

    @PUT("api/Bins/fill-level")
    fun updateBinFillLevel(@Body data: FillLevelRequest): Call<ResponseBody>

    data class ResolveRequest(
        val alertID: Int,
        val userID: String
    )

    @PUT("api/Bins/alert/resolve")
    fun resolveAlert(@Body data: ResolveRequest): Call<ResponseBody>

    @POST("api/Users")
    fun registerUser(@Body data: Map<String, String>): Call<ResponseBody>

    @GET("api/Bins")
    fun getAllBins(): Call<List<BinItem>>

    @GET("api/Bins/alerts")
    fun getAlerts(@Query("userId") userId: String): Call<List<AlertItem>>

    @GET("api/Notifications/{userId}")
    fun getUserNotifications(@Path("userId") userId: String): Call<List<UserNotificationItem>>

    @PUT("api/Notifications/mark-as-read")
    fun markNotificationAsRead(
        @Query("userId") userId: String,
        @Query("notificationId") notificationId: Int
    ): Call<ResponseBody>

    @GET("api/Bins/nearby")
    fun getNearbyBins(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double
    ): Call<List<BinItem>>

    @GET("api/Users/{id}/points")
    fun getUserPoints(@Path("id") userId: String): Call<ResponseBody>

    @GET("api/Users/{id}/transactions")
    fun getTransactions(@Path("id") userId: String): Call<List<PointItem>>

    @GET("api/Users/{id}/waste-events")
    fun getWasteEvents(@Path("id") userId: String): Call<List<WasteEventItem>>

    @PUT("api/Users/{id}/add-points")
    fun addPoints(
        @Path("id") userId: String,
        @Body data: Map<String, Int>
    ): Call<ResponseBody>

    @PUT("api/Users/{id}/update-points")
    fun updateUserPoints(
        @Path("id") userId: String,
        @Body data: Map<String, Int>
    ): Call<ResponseBody>
}