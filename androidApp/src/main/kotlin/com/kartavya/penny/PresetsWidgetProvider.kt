package com.kartavya.penny

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class PresetsWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_presets)

            val symbol = getNotificationPref("widget_currency_symbol", "₹")

            // Preset 1 Setup
            val p1Name = getNotificationPref("widget_preset_1_name", "Tea & Chips")
            val p1Amt = getNotificationPref("widget_preset_1_amount", "20")
            views.setTextViewText(R.id.txt_widget_preset_1, "$p1Name ($symbol$p1Amt)")
            val intent1 = Intent(context, MainActivity::class.java).apply {
                putExtra("widget_action", "log_preset_1")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pending1 = PendingIntent.getActivity(
                context, 401, intent1,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_widget_preset_1, pending1)

            // Preset 2 Setup
            val p2Name = getNotificationPref("widget_preset_2_name", "Metro Ride")
            val p2Amt = getNotificationPref("widget_preset_2_amount", "40")
            views.setTextViewText(R.id.txt_widget_preset_2, "$p2Name ($symbol$p2Amt)")
            val intent2 = Intent(context, MainActivity::class.java).apply {
                putExtra("widget_action", "log_preset_2")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pending2 = PendingIntent.getActivity(
                context, 402, intent2,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_widget_preset_2, pending2)

            // Preset 3 Setup
            val p3Name = getNotificationPref("widget_preset_3_name", "Office Lunch")
            val p3Amt = getNotificationPref("widget_preset_3_amount", "150")
            views.setTextViewText(R.id.txt_widget_preset_3, "$p3Name ($symbol$p3Amt)")
            val intent3 = Intent(context, MainActivity::class.java).apply {
                putExtra("widget_action", "log_preset_3")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pending3 = PendingIntent.getActivity(
                context, 403, intent3,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_widget_preset_3, pending3)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
