package com.kartavya.penny

import platform.UIKit.UIDevice

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
    override val versionCode: Int = 1
}

actual fun getPlatform(): Platform = IOSPlatform()

actual fun openWebUrl(url: String) {
    val nsUrl = platform.Foundation.NSURL.URLWithString(url)
    if (nsUrl != null) {
        platform.UIKit.UIApplication.sharedApplication.openURL(nsUrl)
    }
}

actual fun saveTransactionsPdf(transactions: List<WalletTransaction>): String {
    return "iOS PDF output success stub"
}

actual fun saveNotificationPref(key: String, value: String) {}
actual fun getNotificationPref(key: String, defaultValue: String): String = defaultValue
actual fun scheduleWorkManagerReminders(intervalHours: Int, enabled: Boolean) {}
actual fun triggerImmediateLocalNotification(title: String, message: String) {}
actual fun updateHomeWidgets() {}
actual fun requestPinWidget() {}
actual fun isNotificationPermissionGranted(): Boolean = true
actual fun requestNotificationPermission() {}

actual fun triggerGoogleSignIn(onSuccess: (email: String, name: String, idToken: String?) -> Unit, onFailure: (error: String) -> Unit) {
    onSuccess("joshikartavya78@gmail.com", "Kartavya Joshi", null)
}