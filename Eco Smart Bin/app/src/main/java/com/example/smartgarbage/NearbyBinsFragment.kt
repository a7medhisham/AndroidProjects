package com.example.smartgarbage

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.LocationServices
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class NearbyBinsFragment : Fragment() {

    private lateinit var rc: RecyclerView
    private lateinit var adapter: NearbyBinsAdapter
    private val LOCATION_PERMISSION_REQUEST = 1001
    private var binsList = mutableListOf<BinItem>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_nearby_bins, container, false)
        rc = view.findViewById(R.id.rvNearbyBins)
        rc.layoutManager = LinearLayoutManager(context)

        checkLocationPermission()

        return view
    }

    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED -> {
                getCurrentLocation()
            }
            else -> {
                requestLocationPermission()
            }
        }
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST
        )
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation()
            } else {
                Toast.makeText(context, getString(R.string.location_permission_denied), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getCurrentLocation() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                loadNearbyBins(location.latitude, location.longitude)
            } else {
                Toast.makeText(context, getString(R.string.unable_to_get_location), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadNearbyBins(latitude: Double, longitude: Double) {
        RetrofitInstance.api.getNearbyBins(latitude, longitude)
            .enqueue(object : Callback<List<BinItem>> {
                override fun onResponse(call: Call<List<BinItem>>, response: Response<List<BinItem>>) {
                    if (response.isSuccessful) {
                        binsList = response.body()?.toMutableList() ?: mutableListOf()
                        adapter = NearbyBinsAdapter(binsList)
                        rc.adapter = adapter

                        if (binsList.isEmpty()) {
                            Toast.makeText(context, getString(R.string.no_nearby_bins), Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, getString(R.string.error_message, response.code()), Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<List<BinItem>>, t: Throwable) {
                    Toast.makeText(context, getString(R.string.failed_message, t.message), Toast.LENGTH_SHORT).show()
                }
            })
    }
}