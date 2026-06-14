package com.kartavya.penny

import android.os.Build
import android.content.Context
import android.content.SharedPreferences
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingPeriodicWorkPolicy
import java.util.concurrent.TimeUnit

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
    override val versionCode: Int
        get() {
            val context = AndroidContext.appContext ?: return 1
            return try {
                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode.toInt()
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.versionCode
                }
            } catch (e: Exception) {
                1
            }
        }
}

actual fun getPlatform(): Platform = AndroidPlatform()

actual fun openWebUrl(url: String) {
    val context = AndroidContext.appContext ?: return
    try {
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)).apply {
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

actual fun saveTransactionsPdf(transactions: List<WalletTransaction>): String {
    val pdfDocument = android.graphics.pdf.PdfDocument()
    
    // Create page info (A4 size: 595 x 842 points)
    val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create()
    val page = pdfDocument.startPage(pageInfo)
    val canvas: android.graphics.Canvas = page.canvas
    
    val paint = android.graphics.Paint()
    
    // 1. Draw Header Title
    paint.color = android.graphics.Color.BLACK
    paint.textSize = 20f
    paint.isFakeBoldText = true
    canvas.drawText("Penny Expense Tracker", 40f, 60f, paint)
    
    paint.textSize = 14f
    paint.isFakeBoldText = false
    paint.color = android.graphics.Color.GRAY
    canvas.drawText("Transaction Ledger Report", 40f, 85f, paint)
    
    // 2. Draw Date
    val sdf = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault())
    val dateStr = sdf.format(java.util.Date())
    paint.textSize = 10f
    canvas.drawText("Generated on: $dateStr", 400f, 60f, paint)
    
    // 3. Draw Divider
    paint.color = android.graphics.Color.LTGRAY
    canvas.drawLine(40f, 110f, 555f, 110f, paint)
    
    // 4. Draw Columns Header
    paint.color = android.graphics.Color.DKGRAY
    paint.textSize = 11f
    paint.isFakeBoldText = true
    canvas.drawText("Description", 45f, 135f, paint)
    canvas.drawText("Date", 230f, 135f, paint)
    canvas.drawText("Method", 350f, 135f, paint)
    canvas.drawText("Type", 440f, 135f, paint)
    canvas.drawText("Amount", 500f, 135f, paint)
    
    paint.color = android.graphics.Color.LTGRAY
    canvas.drawLine(40f, 145f, 555f, 145f, paint)
    
    // 5. Draw Transactions
    var yPos = 170f
    paint.isFakeBoldText = false
    paint.color = android.graphics.Color.BLACK
    
    val dateSdf = java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.getDefault())
    
    for (tx in transactions) {
        if (yPos > 800f) {
            break
        }
        
        val desc = if (tx.description.length > 20) tx.description.substring(0, 18) + ".." else tx.description
        canvas.drawText(desc, 45f, yPos, paint)
        
        val txDate = dateSdf.format(java.util.Date(tx.date))
        canvas.drawText(txDate, 230f, yPos, paint)
        
        canvas.drawText(tx.method.uppercase(), 350f, yPos, paint)
        
        val typeStr = tx.type.uppercase()
        paint.color = if (tx.type == "income") android.graphics.Color.parseColor("#4CAF50") else android.graphics.Color.parseColor("#F44336")
        canvas.drawText(typeStr, 440f, yPos, paint)
        
        paint.color = android.graphics.Color.BLACK
        canvas.drawText("Rs. ${tx.amount.toLong()}", 500f, yPos, paint)
        
        yPos += 28f
    }
    
    pdfDocument.finishPage(page)
    
    // 6. Write PDF to Downloads directory
    val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
    val file = java.io.File(downloadsDir, "Penny_Transactions.pdf")
    
    return try {
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
        }
        val fos = java.io.FileOutputStream(file)
        pdfDocument.writeTo(fos)
        pdfDocument.close()
        fos.close()
        file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        pdfDocument.close()
        "Error: ${e.message}"
    }
}

actual fun saveNotificationPref(key: String, value: String) {
    val context = AndroidContext.appContext ?: return
    val prefs = context.getSharedPreferences("penny_notification_prefs", Context.MODE_PRIVATE)
    prefs.edit().putString(key, value).apply()
}

actual fun getNotificationPref(key: String, defaultValue: String): String {
    val context = AndroidContext.appContext ?: return defaultValue
    val prefs = context.getSharedPreferences("penny_notification_prefs", Context.MODE_PRIVATE)
    return prefs.getString(key, defaultValue) ?: defaultValue
}

actual fun scheduleWorkManagerReminders(intervalHours: Int, enabled: Boolean) {
    val context = AndroidContext.appContext ?: return
    val workManager = WorkManager.getInstance(context)
    
    val uniqueWorkName = "penny_notification_sync_work"
    
    if (!enabled) {
        workManager.cancelUniqueWork(uniqueWorkName)
        return
    }

    val prefs = context.getSharedPreferences("penny_notification_prefs", Context.MODE_PRIVATE)
    val pushEnabled = prefs.getString("push_alerts_enabled", "true") == "true"

    val (interval, timeUnit) = if (pushEnabled) {
        15L to TimeUnit.MINUTES
    } else {
        intervalHours.toLong() to TimeUnit.HOURS
    }

    val request = PeriodicWorkRequestBuilder<NotificationSyncWorker>(
        interval, timeUnit
    ).build()

    workManager.enqueueUniquePeriodicWork(
        uniqueWorkName,
        ExistingPeriodicWorkPolicy.UPDATE,
        request
    )
}

actual fun triggerImmediateLocalNotification(title: String, message: String) {
    val context = AndroidContext.appContext ?: return
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
    
    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    val pendingIntent = android.app.PendingIntent.getActivity(
        context, 0, intent,
        android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
    )

    val notification = androidx.core.app.NotificationCompat.Builder(context, "penny_alerts")
        .setSmallIcon(android.R.drawable.ic_dialog_info) // Standard built-in info icon
        .setContentTitle(title)
        .setContentText(message)
        .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .build()

    notificationManager.notify(System.currentTimeMillis().toInt(), notification)
}

actual fun updateHomeWidgets() {
    val context = AndroidContext.appContext ?: return
    val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(context)
    
    val providers = listOf(
        "com.kartavya.penny.SnapshotWidgetProvider",
        "com.kartavya.penny.SplitLedgerWidgetProvider",
        "com.kartavya.penny.PresetsWidgetProvider",
        "com.kartavya.penny.QuickLoggerWidgetProvider"
    )
    
    for (providerClassName in providers) {
        try {
            val componentName = android.content.ComponentName(context.packageName, providerClassName)
            val intent = android.content.Intent().apply {
                action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
                component = componentName
                val ids = appWidgetManager.getAppWidgetIds(componentName)
                putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

actual fun requestPinWidget() {
    val context = AndroidContext.appContext ?: return
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
        val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(context)
        val myProvider = android.content.ComponentName(context.packageName, "com.kartavya.penny.QuickLoggerWidgetProvider")
        if (appWidgetManager.isRequestPinAppWidgetSupported) {
            appWidgetManager.requestPinAppWidget(myProvider, null, null)
        }
    }
}

actual fun isNotificationPermissionGranted(): Boolean {
    val context = AndroidContext.appContext ?: return true
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        val permission = android.Manifest.permission.POST_NOTIFICATIONS
        return androidx.core.content.ContextCompat.checkSelfPermission(context, permission) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }
    return true
}

actual fun requestNotificationPermission() {
    val context = AndroidContext.appContext ?: return
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        val intent = android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            val fallbackIntent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.fromParts("package", context.packageName, null)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(fallbackIntent)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }
}

actual fun triggerGoogleSignIn(onSuccess: (email: String, name: String, idToken: String?) -> Unit, onFailure: (error: String) -> Unit) {
    val context = AndroidContext.appContext ?: return
    GoogleSignInActivity.onSuccessCallback = onSuccess
    GoogleSignInActivity.onFailureCallback = onFailure
    
    val intent = android.content.Intent(context, GoogleSignInActivity::class.java).apply {
        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}