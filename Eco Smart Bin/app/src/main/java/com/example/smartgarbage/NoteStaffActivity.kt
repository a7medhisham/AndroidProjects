package com.example.smartgarbage

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class NoteStaffActivity : AppCompatActivity() {
    private lateinit var adapter: AdapterStaff
    private lateinit var rc: RecyclerView
    private var allItems = mutableListOf<NoteStaff>()
    private var originalAlerts = mutableListOf<AlertItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        val currentNightMode = AppCompatDelegate.getDefaultNightMode()
        super.onCreate(savedInstanceState)

        if (AppCompatDelegate.getDefaultNightMode() != currentNightMode) {
            AppCompatDelegate.setDefaultNightMode(currentNightMode)
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_note_staff)

        rc = findViewById(R.id.rv)
        rc.layoutManager = LinearLayoutManager(this)

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        if (userId.isNotEmpty()) {
            loadAllData(userId)
        } else {
            Toast.makeText(this, getString(R.string.user_not_logged_in), Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadAllData(userId: String) {
        allItems.clear()

        RetrofitInstance.api.getAlerts(userId)
            .enqueue(object : Callback<List<AlertItem>> {
                override fun onResponse(call: Call<List<AlertItem>>, response: Response<List<AlertItem>>) {
                    if (response.isSuccessful) {
                        originalAlerts = response.body()?.toMutableList() ?: mutableListOf()
                        val alertNotes = originalAlerts.map { alert ->
                            NoteStaff(
                                notification_id = alert.alertId,
                                message = getString(R.string.alert_message, alert.alertId, alert.binId, getWasteTypeName(alert.wasteTypeId)),
                                status = alert.status,
                                sent_at = alert.createdAt.split("T")[0]
                            )
                        }
                        allItems.addAll(alertNotes)
                    }
                    loadBinStatus()
                }

                override fun onFailure(call: Call<List<AlertItem>>, t: Throwable) {
                    loadBinStatus()
                }
            })
    }

    private fun loadBinStatus() {
        RetrofitInstance.api.getAllBins()
            .enqueue(object : Callback<List<BinItem>> {
                override fun onResponse(call: Call<List<BinItem>>, response: Response<List<BinItem>>) {
                    if (response.isSuccessful) {
                        val bins = response.body() ?: emptyList()
                        val binNotes = bins.map { bin ->
                            val statusText = if (bin.fillLevel >= 80) {
                                getString(R.string.needs_emptying)
                            } else {
                                getString(R.string.ok_status)
                            }
                            NoteStaff(
                                notification_id = bin.id,
                                message = getString(R.string.bin_status_message, bin.id, bin.fillLevel),
                                status = statusText,
                                sent_at = bin.lastUpdated ?: getString(R.string.unknown)
                            )
                        }
                        allItems.addAll(binNotes)
                    }
                    setupAdapter()
                }

                override fun onFailure(call: Call<List<BinItem>>, t: Throwable) {
                    setupAdapter()
                }
            })
    }

    private fun setupAdapter() {
        adapter = AdapterStaff(ArrayList(allItems)) { item, position ->
            updateItemStatus(item, position)
        }
        rc.adapter = adapter

        if (allItems.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_data_available), Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateItemStatus(item: NoteStaff, position: Int) {
        val isAlert = originalAlerts.any { it.alertId == item.notification_id }

        if (isAlert) {
            when (item.status) {
                "Active", "Pending" -> {
                    adapter.updateItem(position, "Completed")
                    val alert = originalAlerts.find { it.alertId == item.notification_id }
                    if (alert != null) sendStatusToServer(alert)
                }
                "Completed" -> {
                    Toast.makeText(this, getString(R.string.already_completed), Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            showFillLevelDialog(item)
        }
    }

    private fun sendStatusToServer(alert: AlertItem) {
        if (alert.alertId <= 0) return

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val request = ApiService.ResolveRequest(
            alertID = alert.alertId,
            userID = userId
        )

        RetrofitInstance.api.resolveAlert(request)
            .enqueue(object : Callback<ResponseBody> {
                override fun onResponse(
                    call: Call<ResponseBody>,
                    response: Response<ResponseBody>
                ) {
                    when (response.code()) {
                        200 -> {
                            Toast.makeText(
                                this@NoteStaffActivity,
                                getString(R.string.alert_synced_with_server),
                                Toast.LENGTH_SHORT
                            ).show()
                            loadAllData(userId)
                        }
                        409 -> {
                            Toast.makeText(
                                this@NoteStaffActivity,
                                getString(R.string.already_resolved),
                                Toast.LENGTH_SHORT
                            ).show()
                            loadAllData(userId)
                        }
                        else -> {
                            Toast.makeText(
                                this@NoteStaffActivity,
                                getString(R.string.local_only_server_error, response.code()),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Toast.makeText(
                        this@NoteStaffActivity,
                        getString(R.string.local_only_no_connection),
                        Toast.LENGTH_SHORT
                    ).show()
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

    private fun showFillLevelDialog(bin: NoteStaff) {
        val input = android.widget.EditText(this).apply {
            hint = getString(R.string.enter_fill_level)
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }

        android.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.update_bin_title, bin.notification_id))
            .setView(input)
            .setPositiveButton(getString(R.string.update)) { _, _ ->
                val level = input.text.toString().toIntOrNull()
                if (level == null || level < 0 || level > 100) {
                    Toast.makeText(this, getString(R.string.enter_valid_number), Toast.LENGTH_SHORT).show()
                } else {
                    updateFillLevel(bin.notification_id, level)
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun updateFillLevel(binId: Int, fillLevel: Int) {
        val request = ApiService.FillLevelRequest(binID = binId, fillLevel = fillLevel)

        RetrofitInstance.api.updateBinFillLevel(request)
            .enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@NoteStaffActivity, getString(R.string.fill_level_updated), Toast.LENGTH_SHORT).show()
                        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                        loadAllData(userId)
                    } else {
                        Toast.makeText(this@NoteStaffActivity, getString(R.string.error_message, response.code()), Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Toast.makeText(this@NoteStaffActivity, getString(R.string.failed_message, t.message), Toast.LENGTH_SHORT).show()
                }
            })
    }
}