package com.example.smartgarbage

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HistoryFragment : Fragment() {

    private lateinit var tableLayout: TableLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_history, container, false)

        tableLayout = view.findViewById(R.id.tableLayoutHistory)

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        if (uid.isNotEmpty()) {
            loadWasteEvents(uid)
        } else {
            Toast.makeText(context, getString(R.string.user_not_logged_in), Toast.LENGTH_SHORT).show()
        }

        return view
    }

    private fun loadWasteEvents(userId: String) {
        RetrofitInstance.api.getWasteEvents(userId)
            .enqueue(object : Callback<List<WasteEventItem>> {
                override fun onResponse(
                    call: Call<List<WasteEventItem>>,
                    response: Response<List<WasteEventItem>>
                ) {
                    if (response.isSuccessful) {
                        val events = response.body() ?: emptyList()

                        while (tableLayout.childCount > 1) {
                            tableLayout.removeViewAt(tableLayout.childCount - 1)
                        }

                        for (event in events) {
                            val tableRow = TableRow(requireContext())

                            val dateTv = TextView(requireContext()).apply {
                                text = event.date.split("T")[0]
                                setPadding(8, 8, 8, 8)
                            }
                            val typeTv = TextView(requireContext()).apply {
                                text = getWasteTypeName(event.wasteTypeId)
                                setPadding(8, 8, 8, 8)
                            }
                            val weightTv = TextView(requireContext()).apply {
                                text = "${event.weight} kg"
                                setPadding(8, 8, 8, 8)
                            }
                            val pointsTv = TextView(requireContext()).apply {
                                text = event.points.toString()
                                setPadding(8, 8, 8, 8)
                            }

                            tableRow.addView(dateTv)
                            tableRow.addView(typeTv)
                            tableRow.addView(weightTv)
                            tableRow.addView(pointsTv)

                            tableLayout.addView(tableRow)
                        }

                        if (events.isEmpty()) {
                            Toast.makeText(context,
                                getString(R.string.no_data_available), Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<List<WasteEventItem>>, t: Throwable) {
                    Toast.makeText(context, "Failed: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun getWasteTypeName(typeId: Int): String {
        return when (typeId) {
            1 -> getString(R.string.glass)
            2 -> getString(R.string.metal)
            3 -> getString(R.string.paper)
            4 -> getString(R.string.plastic)
            5 -> getString(R.string.trach)
            else -> getString(R.string.unknown)
        }
    }
}