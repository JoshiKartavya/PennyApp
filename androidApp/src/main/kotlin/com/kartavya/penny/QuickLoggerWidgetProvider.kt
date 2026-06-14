package com.kartavya.penny

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class QuickLoggerWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_quick_logger)

            // Setup intent for + Income click
            val incomeIntent = Intent(context, MainActivity::class.java).apply {
                putExtra("widget_action", "create_income")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val incomePendingIntent = PendingIntent.getActivity(
                context, 101, incomeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_widget_income, incomePendingIntent)

            // Setup intent for - Expense click
            val expenseIntent = Intent(context, MainActivity::class.java).apply {
                putExtra("widget_action", "create_expense")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val expensePendingIntent = PendingIntent.getActivity(
                context, 102, expenseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_widget_expense, expensePendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
