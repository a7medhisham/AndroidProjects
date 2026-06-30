package com.example.smartgarbage

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PointsFragment : Fragment() {
    private lateinit var rc: RecyclerView
    private lateinit var adapter: AdapterPoint

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_points, container, false)
        rc = view.findViewById(R.id.rv2)
        rc.layoutManager = LinearLayoutManager(requireContext())

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        loadTransactions(uid)

        return view
    }

    private fun loadTransactions(userId: String) {
        RetrofitInstance.api.getTransactions(userId)
            .enqueue(object : Callback<List<PointItem>> {
                override fun onResponse(
                    call: Call<List<PointItem>>,
                    response: Response<List<PointItem>>
                ) {
                    if (response.isSuccessful) {
                        val transactions = response.body() ?: emptyList()
                        adapter = AdapterPoint(ArrayList(transactions))
                        rc.adapter = adapter
                    } else {
                        Toast.makeText(context, "Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<List<PointItem>>, t: Throwable) {
                    Toast.makeText(context, "Failed: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun addNewPoints(pointsToAdd: Int) {
        val auth = FirebaseAuth.getInstance()

        val email = auth.currentUser?.email ?: ""
        val prefsName = "UserData_${email.hashCode()}"
        val sharedPref = requireContext().getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val currentPoints = sharedPref.getInt("user_points", 0)
        val newPoints = currentPoints + pointsToAdd
        sharedPref.edit().putInt("user_points", newPoints).apply()

        sendPointsUpdateBroadcast(newPoints)

        val userId = auth.currentUser?.uid ?: ""
        if (userId.isNotEmpty()) {
            val pointsData = mapOf("points" to newPoints)
            RetrofitInstance.api.updateUserPoints(userId, pointsData)
                .enqueue(object : Callback<ResponseBody> {
                    override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                        if (response.isSuccessful) {
                            android.util.Log.d("POINTS", "API updated successfully")
                            Toast.makeText(context, "Added $pointsToAdd points", Toast.LENGTH_SHORT).show()
                        } else {
                            android.util.Log.e("POINTS", "API update failed: ${response.code()}")
                            Toast.makeText(context, "Points added locally but API sync failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        android.util.Log.e("POINTS", "API error: ${t.message}")
                        Toast.makeText(context, "Points added locally but API error", Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }

    private fun sendPointsUpdateBroadcast(newPoints: Int) {
        val intent = Intent("POINTS_UPDATED")
        intent.putExtra("new_points", newPoints)
        LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(intent)
    }
}