package com.example.smartgarbage

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AlertsFragment : Fragment() {

    private lateinit var rc: RecyclerView
    private lateinit var adapter: AlertsAdapter
    private var alertsList = mutableListOf<AlertItem>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_alerts, container, false)
        rc = view.findViewById(R.id.rvAlerts)
        rc.layoutManager = LinearLayoutManager(requireContext())

        loadAlerts()

        return view
    }

    private fun loadAlerts() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        if (userId.isEmpty()) return

        RetrofitInstance.api.getAlerts(userId)
            .enqueue(object : Callback<List<AlertItem>> {
                override fun onResponse(call: Call<List<AlertItem>>, response: Response<List<AlertItem>>) {
                    if (response.isSuccessful) {
                        alertsList = response.body()?.toMutableList() ?: mutableListOf()
                        adapter = AlertsAdapter(alertsList)
                        rc.adapter = adapter
                    } else {
                        Toast.makeText(requireContext(), getString(R.string.error_message, response.code()), Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<List<AlertItem>>, t: Throwable) {
                    Toast.makeText(requireContext(), getString(R.string.failed_message, t.message), Toast.LENGTH_SHORT).show()
                }
            })
    }
}