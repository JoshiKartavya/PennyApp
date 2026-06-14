package com.kartavya.penny

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

class PennyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidContext.appContext = this
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Channel 1: Activity & Split Alerts (High Priority)
            val alertsChannel = NotificationChannel(
                CHANNEL_ALERTS_ID,
                "Activity & Splits",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for new split bills, connection requests, and settlement confirmations."
                enableLights(true)
                enableVibration(true)
            }

            // Channel 2: Periodic Reminders (Default Priority)
            val remindersChannel = NotificationChannel(
                CHANNEL_REMINDERS_ID,
                "Logging Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Periodic reminders to log your daily expenses and shared splits."
            }

            notificationManager.createNotificationChannel(alertsChannel)
            notificationManager.createNotificationChannel(remindersChannel)
        }
    }

    companion object {
        const val CHANNEL_ALERTS_ID = "penny_alerts"
        const val CHANNEL_REMINDERS_ID = "penny_reminders"
    }
}
