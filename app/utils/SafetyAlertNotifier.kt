package com.smarttour360.app.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.smarttour360.app.R
import com.smarttour360.app.ui.common.DestinationSummary
import com.smarttour360.app.ui.state.AppStateStore

object SafetyAlertNotifier {
    const val CHANNEL_ID = "smarttour_safety"

    fun checkAndNotify(context: Context, destinations: List<DestinationSummary>) {
        createChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val savedNames = AppStateStore.tripEntries.value.map { it.title }.toSet()
        destinations
            .filter { it.name in savedNames && it.flag == "RED" }
            .forEach { dest ->
                val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_trips)
                    .setContentTitle("Safety Alert - ${dest.name}")
                    .setContentText("This destination in your plan is now RED flagged.")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .build()

                NotificationManagerCompat.from(context)
                    .notify(dest.name.hashCode(), notification)
            }
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "SmartTour Safety",
            NotificationManager.IMPORTANCE_HIGH
        )
        manager.createNotificationChannel(channel)
    }
}
