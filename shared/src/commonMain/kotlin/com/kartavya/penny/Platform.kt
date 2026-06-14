package com.kartavya.penny

interface Platform {
    val name: String
    val versionCode: Int
}

expect fun getPlatform(): Platform

expect fun openWebUrl(url: String)

expect fun saveTransactionsPdf(transactions: List<WalletTransaction>): String

expect fun saveNotificationPref(key: String, value: String)
expect fun getNotificationPref(key: String, defaultValue: String): String
expect fun scheduleWorkManagerReminders(intervalHours: Int, enabled: Boolean)
expect fun triggerImmediateLocalNotification(title: String, message: String)
expect fun isNotificationPermissionGranted(): Boolean
expect fun requestNotificationPermission()
expect fun updateHomeWidgets()
expect fun requestPinWidget()
expect fun triggerGoogleSignIn(onSuccess: (email: String, name: String, idToken: String?) -> Unit, onFailure: (error: String) -> Unit)