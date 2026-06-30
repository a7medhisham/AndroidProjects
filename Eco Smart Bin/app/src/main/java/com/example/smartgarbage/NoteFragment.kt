package com.example.smartgarbage

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class NoteFragment : Fragment() {
    private lateinit var adapter: UserNotificationAdapter
    private lateinit var rc: RecyclerView
    private var notificationsList = mutableListOf<UserNotificationItem>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_note, container, false)
        rc = view.findViewById(R.id.rv)
        rc.layoutManager = LinearLayoutManager(context)
        return view
    }

    override fun onResume() {
        super.onResume()
        requestNotificationPermission()
        loadData()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    100
                )
            }
        }
    }

    private fun loadData() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        if (uid.isNotEmpty()) {
            loadUserNotifications(uid)
        } else {
            Toast.makeText(context, getString(R.string.user_not_logged_in), Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadUserNotifications(userId: String) {
        RetrofitInstance.api.getUserNotifications(userId)
            .enqueue(object : Callback<List<UserNotificationItem>> {
                override fun onResponse(
                    call: Call<List<UserNotificationItem>>,
                    response: Response<List<UserNotificationItem>>
                ) {
                    if (response.isSuccessful) {
                        val data = response.body() ?: emptyList()
                        notificationsList = data.toMutableList()
                        setupAdapter()
                        showSystemNotifications(data)

                        if (notificationsList.isEmpty()) {
                            Toast.makeText(context, getString(R.string.no_notifications), Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, getString(R.string.error_message_num, response.code()), Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<List<UserNotificationItem>>, t: Throwable) {
                    Toast.makeText(context, getString(R.string.failed_message_text, t.message), Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun showSystemNotifications(notifications: List<UserNotificationItem>) {
        val unseen = notifications.filter { it.status == "Not Seen" }
        if (unseen.isEmpty()) return

        val channelId = "smartgarbage_channel"
        val notificationManager = requireContext()
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        unseen.forEach { item ->
            val intent = Intent(requireContext(), HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                requireContext(),
                item.notificationId,
                intent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(requireContext(), channelId)
                .setSmallIcon(R.drawable.icon_notification)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(item.message)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            notificationManager.notify(item.notificationId, notification)
        }
    }

    private fun setupAdapter() {
        adapter = UserNotificationAdapter(notificationsList) { notification, position ->
            markAsRead(notification, position)
        }
        rc.adapter = adapter
    }

    private fun markAsRead(notification: UserNotificationItem, position: Int) {
        adapter.updateItemStatus(position, "Seen")

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        RetrofitInstance.api.markNotificationAsRead(userId, notification.notificationId)
            .enqueue(object : Callback<ResponseBody> {
                override fun onResponse(
                    call: Call<ResponseBody>,
                    response: Response<ResponseBody>
                ) {
                    if (response.isSuccessful) {
                        Toast.makeText(context, getString(R.string.marked_as_read), Toast.LENGTH_SHORT).show()
                    } else {
                        adapter.updateItemStatus(position, "Not Seen")
                        Toast.makeText(context, getString(R.string.failed_message_code, response.code()), Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    adapter.updateItemStatus(position, "Not Seen")
                    Toast.makeText(context, getString(R.string.error_message_text, t.message), Toast.LENGTH_SHORT).show()
                }
            })
    }
}