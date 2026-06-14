package com.kartavya.penny

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class SnapshotWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_snapshot)

            // Dynamic load balances from shared preferences!
            val balance = getNotificationPref("widget_total_balance", "0.00")
            val net = getNotificationPref("widget_today_net", "0.00")
            val symbol = getNotificationPref("widget_currency_symbol", "₹")

            views.setTextViewText(R.id.widget_txt_balance, "$symbol $balance")
            
            val netSign = if (net.startsWith("-") || net.startsWith("+")) "" else "+"
            val displayNet = if (net == "0.00" || net == "0") "today: $symbol 0.00" else "today: $netSign$symbol $net"
            views.setTextViewText(R.id.widget_txt_today_net, displayNet)

            // Make clicking the widget launch the main app
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                context, 201, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_txt_balance, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
