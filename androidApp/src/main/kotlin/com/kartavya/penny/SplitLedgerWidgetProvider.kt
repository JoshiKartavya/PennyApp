package com.kartavya.penny

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class SplitLedgerWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_split_ledger)

            // Dynamic load split ledger overview from preferences!
            val status = getNotificationPref("widget_split_status", "No Active Splits")
            val amount = getNotificationPref("widget_split_amount", "0.00")
            val symbol = getNotificationPref("widget_currency_symbol", "₹")

            views.setTextViewText(R.id.widget_txt_split_status, status)
            views.setTextViewText(R.id.widget_txt_split_amount, "$symbol $amount")

            // Setup intent for "Settle Up Now" button click
            val settleIntent = Intent(context, MainActivity::class.java).apply {
                putExtra("widget_action", "settle_up")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val settlePendingIntent = PendingIntent.getActivity(
                context, 301, settleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_widget_settle, settlePendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
