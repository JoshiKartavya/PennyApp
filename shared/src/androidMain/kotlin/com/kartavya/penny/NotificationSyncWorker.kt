package com.kartavya.penny

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlin.random.Random

class NotificationSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val email = getNotificationPref("logged_in_email", "")
        if (email.isEmpty()) {
            return Result.success()
        }

        // 1. Fetch live DB alerts if notifications are enabled
        val pushEnabled = getNotificationPref("push_alerts_enabled", "true") == "true"
        if (pushEnabled) {
            try {
                val dbNotifications = FirebaseService.fetchNotifications(email)
                val activeNotifications = dbNotifications.filter { it.status == "pending" }
                
                val notifiedIdsStr = getNotificationPref("already_notified_ids", "")
                val notifiedIds = notifiedIdsStr.split(",").filter { it.isNotEmpty() }.toMutableSet()
                
                for (req in activeNotifications) {
                    if (!notifiedIds.contains(req.id)) {
                        // Post dynamic push alert!
                        val title = when (req.type) {
                            "split_request" -> "New Split Expense Request"
                            "split_declined" -> "Split Bill Declined"
                            "connection_request" -> "New Connection Request"
                            else -> "New Penny Notification"
                        }
                        
                        val currencyPref = getNotificationPref("current_currency_symbol", "₹")
                        val formattedAmount = req.amount.toLong()
                        val friendName = extractFirstName(req.fromName)
                        
                        val message = when (req.type) {
                            "split_request" -> "$friendName requested $currencyPref$formattedAmount for \"${req.description}\""
                            "split_declined" -> "$friendName declined your split request for $currencyPref$formattedAmount"
                            "connection_request" -> "$friendName sent you a connection request"
                            else -> "You have a new activity update."
                        }
                        
                        postLocalNotification(title, message, "penny_alerts", req.id.hashCode())
                        notifiedIds.add(req.id)
                    }
                }
                saveNotificationPref("already_notified_ids", notifiedIds.joinToString(","))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 2. Periodic Reminder Notification Engine
        val reminderEnabled = getNotificationPref("periodic_reminders_enabled", "false") == "true"
        if (reminderEnabled) {
            val intervalHours = getNotificationPref("periodic_reminder_interval_hours", "24").toIntOrNull() ?: 24
            val lastReminderTime = getNotificationPref("last_reminder_time", "0").toLongOrNull() ?: 0L
            val currentTime = System.currentTimeMillis()
            
            // Check if reminder interval time has fully passed
            val intervalMs = intervalHours * 60 * 60 * 1000L
            if (currentTime - lastReminderTime >= intervalMs) {
                val reminderMessages = listOf(
                    "Did your wallet just leak some cash? 💸 Log that transaction before you forget!",
                    "Settle the score! ⚖️ Made any shared expenses recently? Add a split and get paid back!",
                    "A penny saved is a penny earned. 🪙 Keep your books perfect—made any transactions lately?",
                    "Quick audit time! 🧐 Swipe, split, or spend anything today? Log it in a flash.",
                    "Don't carry their debt! 🤝 Split that recent bill now and keep your ledger clean.",
                    "Penny for your thoughts... and your transactions! 🧠 Log your recent spendings before they slip away."
                )
                val randomMessage = reminderMessages[Random.nextInt(reminderMessages.size)]
                
                postLocalNotification("Penny Transaction Reminder", randomMessage, "penny_reminders", 99999)
                saveNotificationPref("last_reminder_time", currentTime.toString())
            }
        }

        return Result.success()
    }

    private fun postLocalNotification(title: String, message: String, channelId: String, notificationId: Int) {
        val context = applicationContext
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // built-in info icon
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }
}
