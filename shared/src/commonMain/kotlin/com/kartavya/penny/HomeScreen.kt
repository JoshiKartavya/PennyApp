package com.kartavya.penny

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.window.Dialog

enum class HomeTab {
    Activity,
    Wallet,
    Split
}

// Internal transaction structure matching React Native schema
data class WalletTransaction(
    val id: String,
    val type: String, // "income" or "expense"
    val amount: Double,
    val description: String,
    val date: Long,
    val method: String // "online" or "cash"
)

// Internal Quick Log Presets structure
data class LogPreset(
    val id: String,
    val type: String, // "income" or "expense"
    val amount: Double,
    val description: String,
    val method: String // "online" or "cash"
)

// Internal Split Connection structure for Split ledger tab
data class SplitConnection(
    val id: String,
    val name: String,
    val email: String,
    var balance: Double // Positive = they owe you; Negative = you owe them
)

// Split Expense Item for ledger history
data class SplitHistoryItem(
    val id: String,
    val description: String,
    val amount: Double, // positive = they paid & split; negative = you paid & split
    val isYouSplit: Boolean // true = "You split ₹X", false = "Friend split ₹X"
)

fun extractFirstName(fullName: String): String {
    val name = fullName.trim()
    if (name.contains(" ")) {
        return name.substringBefore(" ").trim()
    }
    val prefix = name.substringBefore("@")
    if (prefix.lowercase().startsWith("snehsoni")) return "Sneh"
    if (prefix.lowercase().contains("kartavya")) return "Kartavya"
    return prefix.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}

// Indian currency pure-Kotlin formatter
fun formatIndian(num: Double): String {
    val isNeg = num < 0
    val absNum = kotlin.math.abs(num)
    val centsValue = kotlin.math.round((absNum - absNum.toLong()) * 100).toLong()
    val cents = if (centsValue >= 100) 99 else centsValue
    val whole = absNum.toLong().toString()
    val dec = if (cents < 10) "0$cents" else cents.toString()
    
    val len = whole.length
    var res = ""
    if (len <= 3) {
        res = whole
    } else {
        res = whole.substring(len - 3)
        var rem = whole.substring(0, len - 3)
        while (rem.length > 2) {
            res = rem.substring(rem.length - 2) + "," + res
            rem = rem.substring(0, rem.length - 2)
        }
        if (rem.isNotEmpty()) {
            res = rem + "," + res
        }
    }
    return (if (isNeg) "-" else "") + res + "." + dec
}

fun trans(key: String, lang: String): String {
    return when (lang) {
        "English" -> {
            when (key) {
                "notification_settings" -> "Notification Settings"
                "manage_alerts_desc" -> "Manage push alerts and logging reminders"
                "push_alerts" -> "Push Alerts"
                "push_alerts_desc" -> "Get live notifications for splits and connections"
                "periodic_reminders" -> "Logging Reminders"
                "periodic_reminders_desc" -> "Receive periodic prompts to record your transactions"
                "reminder_interval" -> "Reminder Interval"
                "send_test_alert" -> "Send Test Alert"
                "notifications" -> "Notifications"
                "help_center" -> "Help Center"
                "clear all data" -> "Clear All Data"
                "widget_settings" -> "Widget Settings"
                "manage_widgets_desc" -> "Manage home screen widgets projection"
                "select_split_connection" -> "Select split friend connection"
                "active_connection" -> "Active Connection (Default)"
                "configure_presets_slot" -> "Configure presets slot"
                "save_widget_settings" -> "Save Widget Settings"
                "widget_settings_saved" -> "Widget Settings saved successfully"
                else -> key
            }
        }
        "हिंदी" -> {
            when (key) {
                "Hello" -> "नमस्ते"
                "Activity" -> "गतिविधि"
                "Wallet" -> "बटुआ"
                "Split" -> "विभाजित"
                "balance" -> "कुल शेष"
                "today" -> "आज"
                "this week" -> "इस सप्ताह"
                "split expenses" -> "खर्च विभाजित करें"
                "BALANCE WITH" -> "के साथ शेष"
                "Owes you" -> "आपको देना है"
                "You owe" -> "आपको चुकाना है"
                "Settle Up" -> "हिसाब चुकता करें"
                "+ add split expense" -> "+ विभाजित खर्च जोड़ें"
                "HISTORY" -> "इतिहास"
                "PENDING REQUESTS" -> "लंबित अनुरोध"
                "settings" -> "सेटिंग्स"
                "logged in as" -> "के रूप में लॉग इन हैं"
                "sign out" -> "साइन आउट"
                "PREFERENCES" -> "प्राथमिकताएं"
                "currency" -> "मुद्रा"
                "language" -> "भाषा"
                "Theme" -> "थीम"
                "font size" -> "फ़ॉन्ट आकार"
                "help_center" -> "सहायता केंद्र"
                "FAQ & Support" -> "अक्सर पूछे जाने वाले प्रश्न"
                "notifications" -> "सूचनाएं"
                "clear all data" -> "सभी डेटा साफ़ करें"
                "Delete Account" -> "खाता हटाएं"
                "activity" -> "गतिविधि"
                "no transactions yet" -> "अभी तक कोई लेन-देने नहीं"
                "YESTERDAY" -> "कल"
                "net positive" -> "सकारात्मक"
                "net spend" -> "कुल खर्च"
                "You have no new notifications." -> "आपके पास कोई नई सूचना नहीं है।"
                "You split" -> "आपने विभाजित किया"
                "split" -> "विभाजित किया"
                "notification_settings" -> "अधिसूचना सेटिंग्स"
                "manage_alerts_desc" -> "पुश अलर्ट और लेनदेन अनुस्मारक प्रबंधित करें"
                "push_alerts" -> "पुश अधिसूचनाएं"
                "push_alerts_desc" -> "नए विभाजन या कनेक्शन आमंत्रणों के लिए अलर्ट प्राप्त करें"
                "periodic_reminders" -> "लॉगिंग अनुस्मारक"
                "periodic_reminders_desc" -> "अपने खर्चों को रिकॉर्ड करने के लिए समय-समय पर अनुस्मारक प्राप्त करें"
                "reminder_interval" -> "अनुस्मारक अंतराल"
                "send_test_alert" -> "परीक्षण अधिसूचना भेजें"
                "Save Preferences" -> "प्राथमिकताएं सहेजें"
                "3 Hours" -> "3 घंटे"
                "6 Hours" -> "6 घंटे"
                "12 Hours" -> "12 घंटे"
                "Daily" -> "दैनिक"
                "Penny Test Alert 🔔" -> "पेनी परीक्षण अलर्ट 🔔"
                "Your notification channels are active and working perfectly!" -> "आपकी अधिसूचना चैनल सक्रिय हैं और पूरी तरह से काम कर रहे हैं!"
                "Preferences saved successfully" -> "प्राथमिकताएं सफलतापूर्वक सहेजी गईं"
                "widget_settings" -> "विजेट सेटिंग्स"
                "manage_widgets_desc" -> "होम स्क्रीन विजेट प्रक्षेपण प्रबंधित करें"
                "select_split_connection" -> "विभाजित मित्र कनेक्शन चुनें"
                "active_connection" -> "सक्रिय कनेक्शन (डिफ़ॉल्ट)"
                "configure_presets_slot" -> "प्रिसेट स्लॉट कॉन्फ़िगर करें"
                "save_widget_settings" -> "विजेट सेटिंग्स सहेजें"
                "widget_settings_saved" -> "विजेट सेटिंग्स सफलतापूर्वक सहेजी गईं"
                else -> key
            }
        }
        "ગુજરાતી" -> {
            when (key) {
                "Hello" -> "નમસ્તે"
                "Activity" -> "પ્રવૃત્તિ"
                "Wallet" -> "વોલેટ"
                "Split" -> "ભાગીદારી"
                "balance" -> "કુલ સિલક"
                "today" -> "આજે"
                "this week" -> "આ અઠવાડિયે"
                "split expenses" -> "ખર્ચની વહેંચણી"
                "BALANCE WITH" -> "સાથેની સિલક"
                "Owes you" -> "તમને ચૂકવવાના છે"
                "You owe" -> "તમારે ચૂકવવાના છે"
                "Settle Up" -> "ચુકવણું કરો"
                "+ add split expense" -> "+ ભાગીદારી ખર્ચ ઉમેરો"
                "HISTORY" -> "ઇતિહાસ"
                "PENDING REQUESTS" -> "બાકી વિનંતીઓ"
                "settings" -> "સેટિંગ્સ"
                "logged in as" -> "તરીકે લોગ ઇન છો"
                "sign out" -> "સાઇન આઉટ"
                "PREFERENCES" -> "પસંદગીઓ"
                "currency" -> "ચલણ"
                "language" -> "ભાષા"
                "Theme" -> "થીમ"
                "font size" -> "ફોન્ટનું કદ"
                "help_center" -> "મદદ કેન્દ્ર"
                "FAQ & Support" -> "પ્રશ્નોત્તરી અને સપોર્ટ"
                "notifications" -> "સૂચનાઓ"
                "clear all data" -> "બધો ડેટા સાફ કરો"
                "Delete Account" -> "ખાતું કાઢી નાખો"
                "activity" -> "પ્રવૃત્તિ"
                "no transactions yet" -> "હજી સુધી કોઈ વ્યવહાર નથી"
                "YESTERDAY" -> "ગઈકાલે"
                "net positive" -> "નફાકારક"
                "net spend" -> "કુલ ખર્ચ"
                "You have no new notifications." -> "તમારી પાસે કોઈ નવી સૂચનાઓ નથી."
                "You split" -> "તમે વહેંચણી કરી"
                "split" -> "વહેંચણી કરી"
                "notification_settings" -> "સૂચના સેટિંગ્સ"
                "manage_alerts_desc" -> "પુશ ચેતવણીઓ અને વ્યવહાર રીમાઇન્ડર્સ મેનેજ કરો"
                "push_alerts" -> "પુશ સૂચનાઓ"
                "push_alerts_desc" -> "નવી ભાગીદારી અથવા જોડાણ આમંત્રણો માટે ચેતવણી મેળવો"
                "periodic_reminders" -> "લોગિંગ રીમાઇન્ડર્સ"
                "periodic_reminders_desc" -> "તમારા ખર્ચ રેકોર્ડ કરવા માટે સમયાંતરે ચેતવણી મેળવો"
                "reminder_interval" -> "રીમાઇન્ડર અંતરાલ"
                "send_test_alert" -> "પરીક્ષણ સૂચના મોકલો"
                "Save Preferences" -> "પસંદગીઓ સાચવો"
                "3 Hours" -> "3 કલાક"
                "6 Hours" -> "6 કલાક"
                "12 Hours" -> "12 કલાક"
                "Daily" -> "દૈનિક"
                "Penny Test Alert 🔔" -> "પેની પરીક્ષણ ચેતવણી 🔔"
                "Your notification channels are active and working perfectly!" -> "તમારી સૂચના ચેનલો સક્રિય છે અને સંપૂર્ણ રીતે કાર્યરત છે!"
                "Preferences saved successfully" -> "પસંદગીઓ સફળતાપૂર્વક સાચવવામાં આવી"
                "widget_settings" -> "વિજેટ સેટિંગ્સ"
                "manage_widgets_desc" -> "હોમ સ્ક્રીન વિજેટ્સ પ્રક્ષેપણ સંચાલિત કરો"
                "select_split_connection" -> "ભાગીદારી મિત્ર કનેક્શન પસંદ કરો"
                "active_connection" -> "સક્રિય કનેક્શન (ડિફૉલ્ટ)"
                "configure_presets_slot" -> "પ્રિસેટ્સ સ્લોટ ગોઠવો"
                "save_widget_settings" -> "વિજેટ સેટિંગ્સ સાચવો"
                "widget_settings_saved" -> "વિજેટ સેટિંગ્સ સફળતાપૂર્વક સાચવવામાં આવી"
                else -> key
            }
        }
        "Español" -> {
            when (key) {
                "Hello" -> "Hola"
                "Activity" -> "Actividad"
                "Wallet" -> "Cartera"
                "Split" -> "Dividir"
                "balance" -> "balance total"
                "today" -> "hoy"
                "this week" -> "esta semana"
                "split expenses" -> "dividir gastos"
                "BALANCE WITH" -> "BALANCE CON"
                "Owes you" -> "Te debe"
                "You owe" -> "Debes"
                "Settle Up" -> "Liquidar"
                "+ add split expense" -> "+ agregar gasto dividido"
                "HISTORY" -> "HISTORIAL"
                "PENDING REQUESTS" -> "SOLICITUDES PENDIENTES"
                "settings" -> "ajustes"
                "logged in as" -> "sesión iniciada como"
                "sign out" -> "cerrar sesión"
                "PREFERENCES" -> "PREFERENCIAS"
                "currency" -> "moneda"
                "language" -> "idioma"
                "Theme" -> "Tema"
                "font size" -> "tamaño de fuente"
                "help_center" -> "centro de ayuda"
                "FAQ & Support" -> "Preguntas frecuentes"
                "notifications" -> "notificaciones"
                "clear all data" -> "borrar todos los datos"
                "Delete Account" -> "Eliminar cuenta"
                "activity" -> "actividad"
                "no transactions yet" -> "no hay transacciones aún"
                "YESTERDAY" -> "AYER"
                "net positive" -> "saldo positivo"
                "net spend" -> "gasto neto"
                "You have no new notifications." -> "No tienes nuevas notificaciones."
                "You split" -> "Dividiste"
                "split" -> "dividió"
                "notification_settings" -> "Ajustes de notificaciones"
                "manage_alerts_desc" -> "Gestionar alertas push y recordatorios"
                "push_alerts" -> "Notificaciones push"
                "push_alerts_desc" -> "Alertas para nuevos splits o conexiones"
                "periodic_reminders" -> "Recordatorios de registro"
                "periodic_reminders_desc" -> "Recibir avisos periódicos para tus gastos"
                "reminder_interval" -> "Intervalo de recordatorio"
                "send_test_alert" -> "Enviar notificación de prueba"
                "Save Preferences" -> "Guardar preferencias"
                "3 Hours" -> "3 horas"
                "6 Hours" -> "6 horas"
                "12 Hours" -> "12 horas"
                "Daily" -> "Diario"
                "Penny Test Alert 🔔" -> "Alerta de prueba de Penny 🔔"
                "Your notification channels are active and working perfectly!" -> "¡Tus canales de notificación están activos y funcionando perfectamente!"
                "Preferences saved successfully" -> "Preferencias guardadas con éxito"
                "widget_settings" -> "Ajustes de widgets"
                "manage_widgets_desc" -> "Gestionar proyección de widgets en pantalla de inicio"
                "select_split_connection" -> "Seleccionar amigo de split"
                "active_connection" -> "Conexión activa (Predeterminado)"
                "configure_presets_slot" -> "Configurar ranura de ajustes preestablecidos"
                "save_widget_settings" -> "Guardar ajustes de widgets"
                "widget_settings_saved" -> "Ajustes de widgets guardados con éxito"
                else -> key
            }
        }
        else -> key
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    userEmail: String,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    
    // Localization
    var currentLanguage by remember { mutableStateOf("English") }
    var currentUserName by remember { mutableStateOf(userEmail.substringBefore("@").replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }) }
    var currentCurrency by remember { mutableStateOf("INR") }
    val currencySymbol = remember(currentCurrency) {
        when (currentCurrency) {
            "USD" -> "$"
            "EUR" -> "€"
            "GBP" -> "£"
            else -> "₹"
        }
    }
    
    // View Overlay toggles
    var activeOverlay by remember { mutableStateOf<String?>(null) } // null, "notifications", "settings"
    var showUpdateDialog by remember { mutableStateOf(false) }

    // Selected Tab backing pager state
    val pagerState = rememberPagerState(
        initialPage = HomeTab.Wallet.ordinal,
        pageCount = { HomeTab.values().size }
    )
    val selectedTab = HomeTab.values()[pagerState.currentPage]

    // Core shared transactional states loaded dynamically from Firestore
    var transactions by remember { mutableStateOf(emptyList<WalletTransaction>()) }
    var presets by remember { mutableStateOf(emptyList<LogPreset>()) }
    var connections by remember { mutableStateOf(emptyList<SplitConnection>()) }
    
    var activeConnectionForSplitDetails by remember { mutableStateOf<SplitConnection?>(null) }
    
    LaunchedEffect(connections) {
        if (activeConnectionForSplitDetails == null && connections.isNotEmpty()) {
            activeConnectionForSplitDetails = connections.first()
        }
    }
    
    var splitDropdownExpanded by remember { mutableStateOf(false) }

    // Custom Split Expense History for the Active Connection
    var splitHistoryList by remember { mutableStateOf(emptyList<SplitHistoryItem>()) }

    var pendingSentRequests by remember { mutableStateOf(emptyList<NotificationItem>()) }
    var pendingReceivedRequests by remember { mutableStateOf(emptyList<NotificationItem>()) }

    // Dynamic Firestore Sync on load or email switch
    LaunchedEffect(userEmail) {
        saveNotificationPref("logged_in_email", userEmail)
        if (userEmail.isNotEmpty()) {
            val reminderEnabled = getNotificationPref("periodic_reminders_enabled", "false") == "true"
            val pushEnabled = getNotificationPref("push_alerts_enabled", "true") == "true"
            val intervalHours = getNotificationPref("periodic_reminder_interval_hours", "24").toIntOrNull() ?: 24
            scheduleWorkManagerReminders(intervalHours, reminderEnabled || pushEnabled)
            
            // Check for Play Store updates if connected to internet
            try {
                val latestVersion = FirebaseService.fetchLatestAppVersion()
                if (latestVersion != null && latestVersion > getPlatform().versionCode) {
                    showUpdateDialog = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            scheduleWorkManagerReminders(24, false)
        }
        var profileName = FirebaseService.fetchUserProfileName(userEmail)
        if (profileName == null || profileName.trim().isEmpty()) {
            val fallbackName = when (userEmail.lowercase().trim()) {
                "joshikartavya78@gmail.com" -> "Kartavya Joshi"
                "snehsoni2006@gmail.com" -> "Sneh Soni"
                else -> userEmail.substringBefore("@").replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
            FirebaseService.saveUserProfile(userEmail, fallbackName)
            profileName = fallbackName
        }
        currentUserName = profileName

        val fetchedTx = FirebaseService.fetchTransactions(userEmail)
        transactions = fetchedTx

        val fetchedPresets = FirebaseService.fetchPresets(userEmail)
        presets = fetchedPresets

        val fetchedConnections = FirebaseService.fetchConnections(userEmail)
        connections = fetchedConnections
        if (fetchedConnections.isNotEmpty()) {
            activeConnectionForSplitDetails = fetchedConnections.first()
        } else {
            activeConnectionForSplitDetails = null
        }

        val fetchedSent = FirebaseService.fetchPendingSentRequests(userEmail)
        pendingSentRequests = fetchedSent

        val fetchedReceived = FirebaseService.fetchNotifications(userEmail)
        pendingReceivedRequests = fetchedReceived
    }

    // Foreground periodic sync loop (runs every 10 seconds)
    LaunchedEffect(userEmail) {
        if (userEmail.isNotEmpty()) {
            while (true) {
                kotlinx.coroutines.delay(10000)
                try {
                    val fetchedTx = FirebaseService.fetchTransactions(userEmail)
                    transactions = fetchedTx

                    val fetchedConnections = FirebaseService.fetchConnections(userEmail)
                    connections = fetchedConnections

                    val fetchedSent = FirebaseService.fetchPendingSentRequests(userEmail)
                    pendingSentRequests = fetchedSent

                    val fetchedReceived = FirebaseService.fetchNotifications(userEmail)
                    
                    // Trigger immediate local notifications in foreground for newly received pending requests
                    val notifiedIdsStr = getNotificationPref("already_notified_ids", "")
                    val notifiedIds = notifiedIdsStr.split(",").filter { it.isNotEmpty() }.toMutableSet()
                    var hasNewNotification = false
                    
                    for (req in fetchedReceived) {
                        if (!notifiedIds.contains(req.id)) {
                            val title = when (req.type) {
                                "split_request" -> trans("New Split Expense Request", currentLanguage)
                                "split_declined" -> trans("Split Bill Declined", currentLanguage)
                                "connection_request" -> trans("New Connection Request", currentLanguage)
                                else -> trans("New Penny Notification", currentLanguage)
                            }
                            
                            val friendName = req.fromName.substringBefore(" ").ifEmpty { req.fromName }
                            val currency = getNotificationPref("current_currency_symbol", "₹")
                            val formattedAmount = req.amount.toLong()
                            
                            val message = when (req.type) {
                                "split_request" -> "$friendName " + trans("requested", currentLanguage) + " $currency$formattedAmount " + trans("for", currentLanguage) + " \"${req.description}\""
                                "split_declined" -> "$friendName " + trans("declined your split request for", currentLanguage) + " $currency$formattedAmount"
                                "connection_request" -> "$friendName " + trans("sent you a connection request", currentLanguage) + ""
                                else -> trans("You have a new activity update.", currentLanguage)
                            }
                            
                            triggerImmediateLocalNotification(title, message)
                            notifiedIds.add(req.id)
                            hasNewNotification = true
                        }
                    }
                    if (hasNewNotification) {
                        saveNotificationPref("already_notified_ids", notifiedIds.joinToString(","))
                    }

                    pendingReceivedRequests = fetchedReceived

                    val activeConn = activeConnectionForSplitDetails
                    if (activeConn != null) {
                        val fetchedHistory = FirebaseService.fetchSplitHistory(activeConn.id, userEmail)
                        splitHistoryList = fetchedHistory
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    LaunchedEffect(currencySymbol) {
        saveNotificationPref("current_currency_symbol", currencySymbol)
        saveNotificationPref("widget_currency_symbol", currencySymbol)
    }

    LaunchedEffect(transactions) {
        val bal = transactions.fold(0.0) { sum, tx -> sum + if (tx.type == "income") tx.amount else -tx.amount }
        saveNotificationPref("widget_total_balance", formatIndian(bal))
        
        val todayMs = System.currentTimeMillis() - 86400000
        val dailyNet = transactions.filter { it.date >= todayMs }.fold(0.0) { sum, tx -> sum + if (tx.type == "income") tx.amount else -tx.amount }
        saveNotificationPref("widget_today_net", formatIndian(dailyNet))
        saveNotificationPref("widget_currency_symbol", currencySymbol)
    }

    LaunchedEffect(connections, activeConnectionForSplitDetails) {
        val selectedId = getNotificationPref("widget_selected_connection_id", "active")
        val activeConn = if (selectedId == "active" || selectedId.isEmpty()) {
            activeConnectionForSplitDetails
        } else {
            connections.find { it.id == selectedId } ?: activeConnectionForSplitDetails
        }

        if (activeConn != null) {
            val status = if (activeConn.balance > 0) "${extractFirstName(activeConn.name)} owes you" else if (activeConn.balance < 0) "You owe ${extractFirstName(activeConn.name)}" else "No owes with ${extractFirstName(activeConn.name)}"
            saveNotificationPref("widget_split_status", status)
            saveNotificationPref("widget_split_amount", formatIndian(kotlin.math.abs(activeConn.balance)))
        } else {
            saveNotificationPref("widget_split_status", "No Active Splits")
            saveNotificationPref("widget_split_amount", "0.00")
        }
        saveNotificationPref("widget_currency_symbol", currencySymbol)
        updateHomeWidgets()
    }



    // Dynamic Split History Sync on active connection switch
    LaunchedEffect(activeConnectionForSplitDetails, userEmail) {
        val activeConn = activeConnectionForSplitDetails
        if (activeConn != null && activeConn.id.isNotEmpty()) {
            val fetchedHistory = FirebaseService.fetchSplitHistory(activeConn.id, userEmail)
            splitHistoryList = fetchedHistory
        } else {
            splitHistoryList = emptyList()
        }
    }

    LaunchedEffect(selectedTab) {
        if (selectedTab == HomeTab.Split) {
            val fetchedSent = FirebaseService.fetchPendingSentRequests(userEmail)
            pendingSentRequests = fetchedSent
            
            val fetchedReceived = FirebaseService.fetchNotifications(userEmail)
            pendingReceivedRequests = fetchedReceived
            
            val fetchedConnections = FirebaseService.fetchConnections(userEmail)
            connections = fetchedConnections
        }
    }
    
    LaunchedEffect(activeOverlay) {
        if (activeOverlay == "notifications") {
            val fetchedReceived = FirebaseService.fetchNotifications(userEmail)
            pendingReceivedRequests = fetchedReceived
        }
    }

    // Modal Overlays States
    var txModalVisible by remember { mutableStateOf(false) }
    var txActionType by remember { mutableStateOf("expense") } // "income" or "expense"
    
    var presetModalVisible by remember { mutableStateOf(false) }
    
    var snapshotVisible by remember { mutableStateOf(false) }
    var reportModalVisible by remember { mutableStateOf(false) }
    
    // Split Overlay States
    var splitModalVisible by remember { mutableStateOf(false) }
    var settleModalVisible by remember { mutableStateOf(false) }
    var addFriendModalVisible by remember { mutableStateOf(false) }
    var friendToRemove by remember { mutableStateOf<SplitConnection?>(null) }

    // Preferences Settings States
    var selectedFontSize by remember { mutableStateOf(getNotificationPref("selected_font_size", "Small")) }

    // Custom Toast Success Notifications
    var toastVisible by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf("") }
    
    fun triggerToast(msg: String) {
        toastMessage = msg
        toastVisible = true
        scope.launch {
            delay(2000)
            toastVisible = false
        }
    }

    LaunchedEffect(WidgetActionTrigger.activeAction) {
        val action = WidgetActionTrigger.activeAction ?: return@LaunchedEffect
        when (action) {
            "create_income" -> {
                txActionType = "income"
                txModalVisible = true
            }
            "create_expense" -> {
                txActionType = "expense"
                txModalVisible = true
            }
            "settle_up" -> {
                if (activeConnectionForSplitDetails != null) {
                    settleModalVisible = true
                } else {
                    triggerToast("No active splits to settle!")
                }
            }
            "log_preset_1" -> {
                val pName = getNotificationPref("widget_preset_1_name", "Tea & Chips")
                val pAmt = getNotificationPref("widget_preset_1_amount", "20.0").toDoubleOrNull() ?: 20.0
                val pType = getNotificationPref("widget_preset_1_type", "expense")
                val pMethod = getNotificationPref("widget_preset_1_method", "cash")
                val newTx = WalletTransaction(
                    id = System.currentTimeMillis().toString(),
                    type = pType,
                    amount = pAmt,
                    description = pName,
                    date = System.currentTimeMillis(),
                    method = pMethod
                )
                transactions = listOf(newTx) + transactions
                triggerToast("Logged $pName (${currencySymbol}${pAmt.toLong()})")
                scope.launch {
                    FirebaseService.saveTransaction(newTx, userEmail)
                }
            }
            "log_preset_2" -> {
                val pName = getNotificationPref("widget_preset_2_name", "Metro Ride")
                val pAmt = getNotificationPref("widget_preset_2_amount", "40.0").toDoubleOrNull() ?: 40.0
                val pType = getNotificationPref("widget_preset_2_type", "expense")
                val pMethod = getNotificationPref("widget_preset_2_method", "online")
                val newTx = WalletTransaction(
                    id = System.currentTimeMillis().toString(),
                    type = pType,
                    amount = pAmt,
                    description = pName,
                    date = System.currentTimeMillis(),
                    method = pMethod
                )
                transactions = listOf(newTx) + transactions
                triggerToast("Logged $pName (${currencySymbol}${pAmt.toLong()})")
                scope.launch {
                    FirebaseService.saveTransaction(newTx, userEmail)
                }
            }
            "log_preset_3" -> {
                val pName = getNotificationPref("widget_preset_3_name", "Office Lunch")
                val pAmt = getNotificationPref("widget_preset_3_amount", "150.0").toDoubleOrNull() ?: 150.0
                val pType = getNotificationPref("widget_preset_3_type", "expense")
                val pMethod = getNotificationPref("widget_preset_3_method", "online")
                val newTx = WalletTransaction(
                    id = System.currentTimeMillis().toString(),
                    type = pType,
                    amount = pAmt,
                    description = pName,
                    date = System.currentTimeMillis(),
                    method = pMethod
                )
                transactions = listOf(newTx) + transactions
                triggerToast("Logged $pName (${currencySymbol}${pAmt.toLong()})")
                scope.launch {
                    FirebaseService.saveTransaction(newTx, userEmail)
                }
            }
        }
        WidgetActionTrigger.activeAction = null
    }

    fun confirmSplitRequest(req: NotificationItem) {
        scope.launch {
            FirebaseService.acceptSplitRequest(req.id)
            
            if (req.type == "split_request") {
                FirebaseService.saveSplitTransaction(
                    connectionId = req.connectionId,
                    fromEmail = req.fromEmail,
                    toEmail = req.toEmail,
                    amount = req.amount,
                    description = req.description
                )
                
                val conn = connections.find { it.id == req.connectionId }
                if (conn != null) {
                    val newBalMap = mapOf(
                        req.toEmail to (conn.balance - req.amount),
                        req.fromEmail to -(conn.balance - req.amount)
                    )
                    FirebaseService.updateConnectionBalances(conn.id, newBalMap)
                }
                
                val newTxA = WalletTransaction(
                    id = System.currentTimeMillis().toString(),
                    type = "expense",
                    amount = req.amount,
                    description = req.description.ifEmpty { "Shared Bill" },
                    date = System.currentTimeMillis(),
                    method = "online"
                )
                FirebaseService.saveTransaction(newTxA, req.fromEmail)
                
                val newTxB = WalletTransaction(
                    id = (System.currentTimeMillis() + 1).toString(),
                    type = "expense",
                    amount = req.amount,
                    description = req.description.ifEmpty { "Shared Bill" },
                    date = System.currentTimeMillis(),
                    method = "online"
                )
                FirebaseService.saveTransaction(newTxB, req.toEmail)
                
                triggerToast("Split confirmed!")
            } else if (req.type == "connection_request") {
                triggerToast("Connection request accepted!")
            } else {
                triggerToast("Confirmed!")
            }
            
            val fetchedTx = FirebaseService.fetchTransactions(userEmail)
            transactions = fetchedTx
            
            val fetchedConnections = FirebaseService.fetchConnections(userEmail)
            connections = fetchedConnections
            
            if (activeConnectionForSplitDetails != null) {
                val activeConn = activeConnectionForSplitDetails!!
                val fetchedHistory = FirebaseService.fetchSplitHistory(activeConn.id, userEmail)
                splitHistoryList = fetchedHistory
            }
            
            val fetchedReceived = FirebaseService.fetchNotifications(userEmail)
            pendingReceivedRequests = fetchedReceived
            
            val fetchedSent = FirebaseService.fetchPendingSentRequests(userEmail)
            pendingSentRequests = fetchedSent
        }
    }
    
    fun declineSplitRequest(req: NotificationItem) {
        scope.launch {
            FirebaseService.declineSplitRequest(req.id)
            
            // Send declined notification alert back to sender
            if (req.type == "split_request") {
                FirebaseService.sendSplitRequest(
                    connectionId = req.connectionId,
                    fromEmail = userEmail,
                    fromName = currentUserName,
                    toEmail = req.fromEmail,
                    amount = req.amount,
                    description = req.description,
                    type = "split_declined"
                )
            }
            
            triggerToast("Request declined!")
            
            val fetchedReceived = FirebaseService.fetchNotifications(userEmail)
            pendingReceivedRequests = fetchedReceived
        }
    }

    // Dynamic aggregates
    val balance = transactions.fold(0.0) { sum, tx -> sum + if (tx.type == "income") tx.amount else -tx.amount }
    val cashBalance = transactions.filter { it.method == "cash" }.fold(0.0) { sum, tx -> sum + if (tx.type == "income") tx.amount else -tx.amount }
    val onlineBalance = transactions.filter { it.method == "online" }.fold(0.0) { sum, tx -> sum + if (tx.type == "income") tx.amount else -tx.amount }

    // Net metrics calculations
    val todayStart = System.currentTimeMillis() - (System.currentTimeMillis() % 86400000)
    val dailyNet = transactions.filter { it.date >= todayStart }.fold(0.0) { sum, tx -> sum + if (tx.type == "income") tx.amount else -tx.amount }
    val isProfit = dailyNet >= 0
    val dailyColor = if (isProfit) PennyTheme.colors.successLight else PennyTheme.colors.danger
    val dailySign = if (isProfit && dailyNet > 0) "+" else ""

    val currentDensity = LocalDensity.current
    val customDensity = remember(currentDensity, selectedFontSize) {
        val scale = when (selectedFontSize) {
            "Small" -> 0.85f
            "Medium" -> 1.00f
            "Large" -> 1.20f
            else -> 1.00f
        }
        object : Density {
            override val density: Float get() = currentDensity.density
            override val fontScale: Float get() = currentDensity.fontScale * scale
        }
    }

    CompositionLocalProvider(LocalDensity provides customDensity) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF0C0C0C)) // Pure pitch-black background from screenshots
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
        ) {
            
            // ================= SCREEN PORT VIEW CONTROL =================
            if (activeOverlay == "notifications") {
                NotificationsOverlay(
                    pendingReceivedRequests = pendingReceivedRequests,
                    onConfirmRequest = { req -> confirmSplitRequest(req) },
                    onDeclineRequest = { req -> declineSplitRequest(req) },
                    onDismiss = { activeOverlay = null },
                    userEmail = userEmail,
                    onDismissDeclined = { req ->
                        scope.launch {
                            FirebaseService.declineSplitRequest(req.id)
                            val fetched = FirebaseService.fetchNotifications(userEmail)
                            pendingReceivedRequests = fetched
                        }
                    },
                    currentLanguage = currentLanguage,
                    currencySymbol = currencySymbol
                )
            } else if (activeOverlay == "settings") {
                SettingsOverlay(
                    userEmail = userEmail,
                    userName = extractFirstName(currentUserName),
                    selectedCurrency = currentCurrency,
                    onShowCurrency = { activeOverlay = "currency" },
                    selectedFontSize = selectedFontSize,
                    onFontSizeChange = { 
                        selectedFontSize = it
                        saveNotificationPref("selected_font_size", it)
                    },
                    onLogout = {
                        activeOverlay = null
                        onLogout()
                    },
                    onDismiss = { activeOverlay = null },
                    onShowNotifications = { activeOverlay = "notifications" },
                    onShowNotificationSettings = { activeOverlay = "notification_settings" },
                    onShowWidgetSettings = { activeOverlay = "widget_settings" },
                    onShowHelpCenter = { activeOverlay = "help_center" },
                    onShowLanguage = { activeOverlay = "language" },
                    selectedLanguage = currentLanguage
                )
            } else if (activeOverlay == "notification_settings") {
                NotificationsSettingsOverlay(
                    onDismiss = { activeOverlay = "settings" },
                    currentLanguage = currentLanguage,
                    triggerToast = { triggerToast(it) }
                )
            } else if (activeOverlay == "widget_settings") {
                WidgetSettingsOverlay(
                    connections = connections,
                    presets = presets,
                    onDismiss = { activeOverlay = "settings" },
                    currentLanguage = currentLanguage,
                    currencySymbol = currencySymbol,
                    triggerToast = { triggerToast(it) }
                )
            } else if (activeOverlay == "help_center") {
                HelpCenterOverlay(
                    userEmail = userEmail,
                    onClearAllData = {
                        scope.launch {
                            transactions.forEach { FirebaseService.deleteTransaction(it.id) }
                            connections.forEach { FirebaseService.deleteConnection(it.id) }
                            presets.forEach { FirebaseService.deletePreset(it.id) }
                            transactions = emptyList()
                            connections = emptyList()
                            presets = emptyList()
                            splitHistoryList = emptyList()
                            activeConnectionForSplitDetails = null
                            triggerToast("All data cleared successfully!")
                        }
                    },
                    onDeleteAccount = {
                        scope.launch {
                            transactions.forEach { FirebaseService.deleteTransaction(it.id) }
                            connections.forEach { FirebaseService.deleteConnection(it.id) }
                            presets.forEach { FirebaseService.deletePreset(it.id) }
                            transactions = emptyList()
                            connections = emptyList()
                            presets = emptyList()
                            splitHistoryList = emptyList()
                            activeConnectionForSplitDetails = null
                            triggerToast("Account deleted.")
                            delay(1000)
                            activeOverlay = null
                            onLogout()
                        }
                    },
                    onDismiss = { activeOverlay = "settings" }
                )
            } else if (activeOverlay == "currency") {
                CurrencyOverlay(
                    selectedCurrency = currentCurrency,
                    onCurrencyChange = { currentCurrency = it },
                    onDismiss = { activeOverlay = "settings" },
                    currentLanguage = currentLanguage
                )
            } else if (activeOverlay == "language") {
                LanguageOverlay(
                    selectedLanguage = currentLanguage,
                    onLanguageChange = { currentLanguage = it },
                    onDismiss = { activeOverlay = "settings" }
                )
            } else {
                // ================= MAIN CORE PAGES CONTENT =================

                // Main Top Header Bar (Image 1/4/5)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val userName = extractFirstName(currentUserName)
                        val helloText = trans("Hello", currentLanguage)
                        Text(
                            text = "$helloText, $userName ",
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "👋",
                            fontSize = 28.sp
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "🔔",
                            fontSize = 28.sp,
                            modifier = Modifier
                                .clickable { activeOverlay = "notifications" }
                                .padding(8.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "⚙️",
                            fontSize = 28.sp,
                            modifier = Modifier
                                .clickable { activeOverlay = "settings" }
                                .padding(8.dp)
                        )
                    }
                }

                // Top Tab Navigation Bar sits exactly like TopTabNavigator in React Native
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HomeTab.values().forEach { tab ->
                        val isActive = selectedTab == tab
                        val activeColor = Color.White
                        val inactiveColor = Color(0xFF666666)
                        
                        Column(
                            modifier = Modifier
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    scope.launch {
                                        pagerState.animateScrollToPage(tab.ordinal)
                                    }
                                }
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = trans(tab.name, currentLanguage), // "Activity", "Wallet", "Split"
                                fontSize = 16.sp,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                color = if (isActive) activeColor else inactiveColor,
                                letterSpacing = 0.3.sp
                            )
                            
                            // High fidelity line indicator under active tab matching Image 1
                            Box(
                                modifier = Modifier
                                    .padding(top = 6.dp)
                                    .height(2.5.dp)
                                    .width(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isActive) Color.White else Color.Transparent)
                            )
                        }
                    }
                }

                // Tab View body
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) { page ->
                    when (HomeTab.values()[page]) {
                        HomeTab.Wallet -> {
                            WalletTabContent(
                                balance = balance,
                                cashBalance = cashBalance,
                                onlineBalance = onlineBalance,
                                dailyNet = dailyNet,
                                isProfit = isProfit,
                                dailyColor = dailyColor,
                                dailySign = dailySign,
                                presets = presets,
                                onLogPreset = { preset ->
                                    val newTx = WalletTransaction(
                                        id = System.currentTimeMillis().toString(),
                                        type = preset.type,
                                        amount = preset.amount,
                                        description = preset.description,
                                        date = System.currentTimeMillis(),
                                        method = preset.method
                                    )
                                    transactions = listOf(newTx) + transactions
                                    triggerToast("Logged ${currencySymbol}${preset.amount}")
                                    scope.launch {
                                        FirebaseService.saveTransaction(newTx, userEmail)
                                    }
                                },
                                onDeletePreset = { preset ->
                                    presets = presets.filter { it.id != preset.id }
                                    triggerToast("Preset deleted")
                                    scope.launch {
                                        FirebaseService.deletePreset(preset.id)
                                    }
                                },
                                onCreatePresetClick = { presetModalVisible = true },
                                onAddIncomeClick = {
                                    txActionType = "income"
                                    txModalVisible = true
                                },
                                onAddExpenseClick = {
                                    txActionType = "expense"
                                    txModalVisible = true
                                },
                                onShowSnapshot = { snapshotVisible = true },
                                currentLanguage = currentLanguage,
                                currencySymbol = currencySymbol
                            )
                        }
                        HomeTab.Activity -> {
                            ActivityTabContent(
                                transactions = transactions,
                                onDeleteTransaction = { tx ->
                                    transactions = transactions.filter { it.id != tx.id }
                                    triggerToast(trans("Transaction deleted", currentLanguage))
                                    scope.launch {
                                        FirebaseService.deleteTransaction(tx.id)
                                    }
                                },
                                currentLanguage = currentLanguage,
                                currencySymbol = currencySymbol
                            )
                        }
                        HomeTab.Split -> {
                            SplitTabContent(
                                connections = connections,
                                activeConnection = activeConnectionForSplitDetails ?: SplitConnection("", "No Connection", "", 0.0),
                                splitDropdownExpanded = splitDropdownExpanded,
                                splitHistoryList = splitHistoryList,
                                pendingSentRequests = pendingSentRequests,
                                pendingReceivedRequests = pendingReceivedRequests,
                                onConfirmRequest = { req -> confirmSplitRequest(req) },
                                onDeclineRequest = { req -> declineSplitRequest(req) },
                                onConnectionChange = { activeConnectionForSplitDetails = it },
                                onDropdownToggle = { splitDropdownExpanded = it },
                                onAddFriendClick = { addFriendModalVisible = true },
                                onSettleUpClick = { settleModalVisible = true },
                                onAddSplitExpenseClick = { splitModalVisible = true },
                                onRemoveFriendClick = { friendToRemove = it },
                                currentLanguage = currentLanguage,
                                currencySymbol = currencySymbol
                            )
                        }
                    }
                }
            }
        }

        // ================= OVERLAY DIALOGS =================

        // 1. Transaction Creator sheet
        if (txModalVisible) {
            TransactionCreatorSheet(
                actionType = txActionType,
                onDismiss = { txModalVisible = false },
                onSaveTransaction = { amt, method, desc ->
                    val newTx = WalletTransaction(
                        id = System.currentTimeMillis().toString(),
                        type = txActionType,
                        amount = amt,
                        description = desc.ifEmpty { if (txActionType == "income") "Income" else "Expense" },
                        date = System.currentTimeMillis(),
                        method = method
                    )
                    transactions = listOf(newTx) + transactions
                    txModalVisible = false
                    triggerToast("Logged ${currencySymbol}${amt}")
                    scope.launch {
                        FirebaseService.saveTransaction(newTx, userEmail)
                    }
                },
                currencySymbol = currencySymbol
            )
        }

        // 2. Preset Creator modal overlay
        if (presetModalVisible) {
            PresetCreatorSheet(
                onDismiss = { presetModalVisible = false },
                onSavePreset = { type, amt, method, desc ->
                    val newPreset = LogPreset(
                        id = System.currentTimeMillis().toString(),
                        type = type,
                        amount = amt,
                        description = desc,
                        method = method
                    )
                    presets = presets + newPreset
                    presetModalVisible = false
                    triggerToast("Preset created")
                    scope.launch {
                        FirebaseService.savePreset(newPreset, userEmail)
                    }
                },
                currencySymbol = currencySymbol
            )
        }

        // 3. Snapshot Card Overlay ("How am I doing?")
        if (snapshotVisible) {
            SnapshotCardOverlay(
                balance = balance,
                dailyNet = dailyNet,
                transactionsCount = transactions.size,
                transactions = transactions,
                onDismiss = { snapshotVisible = false },
                onDownloadReport = {
                    snapshotVisible = false
                    reportModalVisible = true
                },
                currencySymbol = currencySymbol
            )
        }

        // 4. Report PDF duration modal sheet
        if (reportModalVisible) {
            ReportDurationSheet(
                onDismiss = { reportModalVisible = false },
                onDownloadPdf = { days ->
                    reportModalVisible = false
                    val cutoffTime = System.currentTimeMillis() - (days * 24L * 3600L * 1000L)
                    val filteredTx = transactions.filter { it.date >= cutoffTime }
                    val path = saveTransactionsPdf(filteredTx)
                    if (path.startsWith("Error")) {
                        triggerToast("Export failed!")
                    } else {
                        triggerToast("Saved to Downloads!")
                    }
                }
            )
        }

        // 5. Split Bill modal sheet
        if (splitModalVisible && activeConnectionForSplitDetails != null) {
            val activeConn = activeConnectionForSplitDetails!!
            SplitBillSheet(
                connection = activeConn,
                onDismiss = { splitModalVisible = false },
                onSaveSplit = { amt, whoPaid, desc ->
                    splitModalVisible = false
                    triggerToast("Split request sent for approval!")
                    
                    scope.launch {
                        FirebaseService.sendSplitRequest(
                            connectionId = activeConn.id,
                            fromEmail = userEmail,
                            fromName = currentUserName,
                            toEmail = activeConn.email,
                            amount = amt,
                            description = desc.ifEmpty { "Shared Bill" }
                        )
                        
                        val fetchedSent = FirebaseService.fetchPendingSentRequests(userEmail)
                        pendingSentRequests = fetchedSent
                    }
                },
                currencySymbol = currencySymbol
            )
        }

        // 6. Settle Up modal sheet
        if (settleModalVisible && activeConnectionForSplitDetails != null) {
            val activeConn = activeConnectionForSplitDetails!!
            SettleUpSheet(
                connection = activeConn,
                onDismiss = { settleModalVisible = false },
                onSaveSettlement = { amt ->
                    val owedAmt = activeConn.balance
                    val updatedConnections = connections.map { c ->
                        if (c.id == activeConn.id) {
                            val balanceShift = if (owedAmt > 0) -amt else amt
                            c.copy(balance = c.balance + balanceShift)
                        } else c
                    }
                    connections = updatedConnections
                    activeConnectionForSplitDetails = connections.find { it.id == activeConn.id }
                    
                    // Also log a transaction record in Wallet!
                    val isIncome = owedAmt < 0
                    val txRecord = WalletTransaction(
                        id = System.currentTimeMillis().toString(),
                        type = if (isIncome) "income" else "expense",
                        amount = amt,
                        description = "Settled with ${extractFirstName(activeConn.name)}",
                        date = System.currentTimeMillis(),
                        method = "online"
                    )
                    transactions = listOf(txRecord) + transactions

                    // Add settlement log in ledger history
                    val settleHist = SplitHistoryItem(
                        id = System.currentTimeMillis().toString(),
                        description = "Settled Up Debt",
                        amount = if (owedAmt > 0) -amt else amt,
                        isYouSplit = owedAmt < 0
                    )
                    splitHistoryList = listOf(settleHist) + splitHistoryList

                    settleModalVisible = false
                    triggerToast("Debt Settled successfully!")

                    // Background Firestore Sync
                    scope.launch {
                        // 1. Save split transaction record
                        FirebaseService.saveSplitTransaction(
                            connectionId = activeConn.id,
                            fromEmail = if (owedAmt > 0) activeConn.email else userEmail,
                            toEmail = if (owedAmt > 0) userEmail else activeConn.email,
                            amount = amt,
                            description = "Settled Up Debt"
                        )
                        // 2. Update connection balances
                        val balanceShift = if (owedAmt > 0) -amt else amt
                        val newBalMap = mapOf(
                            userEmail to (activeConn.balance + balanceShift),
                            activeConn.email to -(activeConn.balance + balanceShift)
                        )
                        FirebaseService.updateConnectionBalances(activeConn.id, newBalMap)
                        // 3. Save personal transaction
                        FirebaseService.saveTransaction(txRecord, userEmail)
                    }
                },
                currencySymbol = currencySymbol
            )
        }

        // 7. Add Friend Connection modal sheet
        if (addFriendModalVisible) {
            AddFriendConnectionSheet(
                onDismiss = { addFriendModalVisible = false },
                onSaveFriend = { name, email ->
                    val sortedEmails = listOf(userEmail.lowercase(), email.lowercase()).sorted()
                    val connectionId = sortedEmails.joinToString("_")
                    val newConn = SplitConnection(
                        id = connectionId,
                        name = name,
                        email = email,
                        balance = 0.00
                    )
                    connections = connections + newConn
                    activeConnectionForSplitDetails = newConn
                    addFriendModalVisible = false
                    triggerToast("Connection added successfully!")

                    scope.launch {
                        FirebaseService.addConnection(
                            currentUserEmail = userEmail,
                            currentUserCustomName = currentUserName,
                            friendEmail = email,
                            friendCustomName = name
                        )
                        FirebaseService.sendSplitRequest(
                            connectionId = connectionId,
                            fromEmail = userEmail,
                            fromName = currentUserName,
                            toEmail = email,
                            amount = 0.0,
                            description = "wants to connect",
                            type = "connection_request"
                        )
                    }
                }
            )
        }

        // 8. Remove Friend Connection modal overlay
        if (friendToRemove != null) {
            RemoveFriendConnectionSheet(
                connection = friendToRemove!!,
                onDismiss = { friendToRemove = null },
                onConfirmRemove = {
                    val target = friendToRemove!!
                    connections = connections.filter { it.id != target.id }
                    if (activeConnectionForSplitDetails?.id == target.id) {
                        if (connections.isNotEmpty()) {
                            activeConnectionForSplitDetails = connections[0]
                        } else {
                            activeConnectionForSplitDetails = null
                        }
                    }
                    friendToRemove = null
                    triggerToast("Removed ${target.name} successfully")

                    scope.launch {
                        FirebaseService.deleteConnection(target.id)
                    }
                }
            )
        }

        // ================= CUSTOM SUCCESS Toast Notification overlay =================
        AnimatedVisibility(
            visible = toastVisible,
            enter = fadeIn(tween(300)) + slideInVertically(initialOffsetY = { -60 }, animationSpec = tween(300)),
            exit = fadeOut(tween(300)) + slideOutVertically(targetOffsetY = { -60 }, animationSpec = tween(300)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 20.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(30.dp))
                    .background(PennyTheme.colors.successLight)
                    .padding(vertical = 12.dp, horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✓",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = toastMessage,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        if (showUpdateDialog) {
            Dialog(
                onDismissRequest = { showUpdateDialog = false }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color(0xFF161618))
                        .border(1.dp, Color(0xFF2C2C2E), RoundedCornerShape(28.dp))
                        .padding(28.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFF8A2387), Color(0xFFE94057), Color(0xFFF27121))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "✨",
                                fontSize = 32.sp
                            )
                        }

                        Text(
                            text = trans("New Update Available!", currentLanguage),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Text(
                            text = trans("A brand new version of Penny is available on the Play Store with new features, performance improvements, and bug fixes.", currentLanguage),
                            fontSize = 14.sp,
                            color = Color(0xFFAAAAAA),
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { showUpdateDialog = false },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF2C2C2E),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.weight(1f).height(46.dp)
                            ) {
                                Text(
                                    text = trans("Later", currentLanguage),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Button(
                                onClick = {
                                    showUpdateDialog = false
                                    openWebUrl("https://play.google.com/store/apps/details?id=com.kartavya.penny")
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(),
                                modifier = Modifier
                                    .weight(1.5f)
                                    .height(46.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(Color(0xFF8A2387), Color(0xFFE94057), Color(0xFFF27121))
                                        )
                                    )
                            ) {
                                Text(
                                    text = trans("Update Now", currentLanguage),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
}

// ================= MODULAR SUB-COMPOSABLES FOR PORT VIEW OVERLAYS =================

@Composable
fun NotificationsOverlay(
    pendingReceivedRequests: List<NotificationItem>,
    onConfirmRequest: (NotificationItem) -> Unit,
    onDeclineRequest: (NotificationItem) -> Unit,
    onDismiss: () -> Unit,
    userEmail: String,
    onDismissDeclined: (NotificationItem) -> Unit,
    currentLanguage: String = "English",
    currencySymbol: String = "₹",
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = trans("notifications", currentLanguage).lowercase(),
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF888888)
            )
            
            Text(
                text = "✕",
                fontSize = 22.sp,
                color = Color.White,
                fontWeight = FontWeight.Light,
                modifier = Modifier
                    .clickable { onDismiss() }
                    .padding(8.dp)
            )
        }
        
        if (pendingReceivedRequests.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = trans("You have no new notifications.", currentLanguage),
                    fontSize = 15.sp,
                    color = Color(0xFF666666),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                items(pendingReceivedRequests) { req ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 4.dp)
                    ) {
                        Column {
                            if (req.type == "split_declined") {
                                val declinedAlertText = if (currentLanguage == "हिंदी") {
                                    "${extractFirstName(req.fromName)} ने ${currencySymbol}${req.amount.toLong()} का आपका विभाजित अनुरोध अस्वीकार कर दिया"
                                } else if (currentLanguage == "ગુજરાતી") {
                                    "${extractFirstName(req.fromName)} એ ${currencySymbol}${req.amount.toLong()} ની તમારી વિનંતી નકારી કાઢી"
                                } else if (currentLanguage == "Español") {
                                    "${extractFirstName(req.fromName)} rechazó tu solicitud de ${currencySymbol}${req.amount.toLong()}"
                                } else {
                                    "${extractFirstName(req.fromName)} declined your request for ${currencySymbol}${req.amount.toLong()}"
                                }

                                Text(
                                    text = declinedAlertText,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "For: \"${req.description.ifEmpty { "Shared Bill" }}\"",
                                    fontSize = 13.sp,
                                    color = Color(0xFF888888),
                                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                                )
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Button(
                                        onClick = { onDismissDeclined(req) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.White,
                                            contentColor = Color.Black
                                        ),
                                        shape = RoundedCornerShape(16.dp),
                                        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 20.dp)
                                    ) {
                                        val dismissLabel = if (currentLanguage == "हिंदी") "खारिज करें" else if (currentLanguage == "ગુજરાતી") "કાઢી નાખો" else if (currentLanguage == "Español") "Descartar" else "Dismiss"
                                        Text(text = dismissLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else if (req.type == "connection_request") {
                                val connectionAlertText = if (currentLanguage == "हिंदी") {
                                    "${extractFirstName(req.fromName)} ने आपके साथ कनेक्शन जोड़ा है"
                                } else if (currentLanguage == "ગુજરાતી") {
                                    "${extractFirstName(req.fromName)} એ તમારી સાથે જોડાણ કર્યું છે"
                                } else if (currentLanguage == "Español") {
                                    "${extractFirstName(req.fromName)} se conectó contigo"
                                } else {
                                    "${extractFirstName(req.fromName)} added you as a connection"
                                }

                                Text(
                                    text = connectionAlertText,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "wants to connect",
                                    fontSize = 13.sp,
                                    color = Color(0xFF888888),
                                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                                )
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Button(
                                        onClick = { onConfirmRequest(req) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.White,
                                            contentColor = Color.Black
                                        ),
                                        shape = RoundedCornerShape(16.dp),
                                        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 20.dp)
                                    ) {
                                        val confirmLabel = if (currentLanguage == "हिंदी") "पुष्टि करें" else if (currentLanguage == "ગુજરાતી") "મંજૂર કરો" else if (currentLanguage == "Español") "Confirmar" else "Confirm"
                                        Text(text = confirmLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else {
                                val requestAlertText = if (currentLanguage == "हिंदी") {
                                    "${extractFirstName(req.fromName)} ने ${currencySymbol}${req.amount.toLong()} का अनुरोध किया"
                                } else if (currentLanguage == "ગુજરાતી") {
                                    "${extractFirstName(req.fromName)} એ ${currencySymbol}${req.amount.toLong()} ની વિનંતી કરી"
                                } else if (currentLanguage == "Español") {
                                    "${extractFirstName(req.fromName)} solicitó ${currencySymbol}${req.amount.toLong()}"
                                } else {
                                    "${extractFirstName(req.fromName)} requested ${currencySymbol}${req.amount.toLong()}"
                                }

                                Text(
                                    text = requestAlertText,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "For: \"${req.description.ifEmpty { "Shared Bill" }}\"",
                                    fontSize = 13.sp,
                                    color = Color(0xFF888888),
                                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                                )
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Button(
                                        onClick = { onConfirmRequest(req) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.White,
                                            contentColor = Color.Black
                                        ),
                                        shape = RoundedCornerShape(16.dp),
                                        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 20.dp)
                                    ) {
                                        val confirmLabel = if (currentLanguage == "हिंदी") "पुष्टि करें" else if (currentLanguage == "ગુજરાતી") "મંજૂર કરો" else if (currentLanguage == "Español") "Confirmar" else "Confirm"
                                        Text(text = confirmLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    
                                    Button(
                                        onClick = { onDeclineRequest(req) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF2C2C2C),
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(16.dp),
                                        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 20.dp)
                                    ) {
                                        val declineLabel = if (currentLanguage == "हिंदी") "अस्वीकार करें" else if (currentLanguage == "ગુજરાતી") "નકારો" else if (currentLanguage == "Español") "Rechazar" else "Decline"
                                        Text(text = declineLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = Color(0xFF222222), thickness = 1.dp)
                    }
                }
            }
        }
    }
}

@Composable
fun LanguageOverlay(
    selectedLanguage: String,
    onLanguageChange: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0C0C0C))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "choose language",
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
            
            Text(
                text = "done",
                fontSize = 15.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable { onDismiss() }
                    .padding(8.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        val languages = listOf("English", "हिंदी", "ગુજરાતી", "Español")
        
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            languages.forEach { lang ->
                val isSelected = selectedLanguage == lang
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isSelected) Color(0xFF161616) else Color.Transparent)
                        .clickable { onLanguageChange(lang) }
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = lang,
                        fontSize = 16.sp,
                        color = if (isSelected) Color.White else Color(0xFF888888),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                    
                    if (isSelected) {
                        Text(
                            text = "✓",
                            fontSize = 18.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                HorizontalDivider(color = Color(0xFF1E1E1E))
            }
        }
    }
}

@Composable
fun CurrencyOverlay(
    selectedCurrency: String,
    onCurrencyChange: (String) -> Unit,
    onDismiss: () -> Unit,
    currentLanguage: String = "English",
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0C0C0C))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val title = if (currentLanguage == "हिंदी") "मुद्रा चुनें" else if (currentLanguage == "ગુજરાતી") "ચલણ પસંદ કરો" else if (currentLanguage == "Español") "elegir moneda" else "choose currency"
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
            
            val doneLabel = if (currentLanguage == "हिंदी") "सम्पन्न" else if (currentLanguage == "ગુજરાતી") "થઈ ગયું" else if (currentLanguage == "Español") "hecho" else "done"
            Text(
                text = doneLabel,
                fontSize = 15.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable { onDismiss() }
                    .padding(8.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        val currencies = listOf(
            "INR" to ("Indian Rupee" to "₹"),
            "USD" to ("US Dollar" to "$"),
            "EUR" to ("Euro" to "€"),
            "GBP" to ("British Pound" to "£")
        )
        
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            currencies.forEach { (code, info) ->
                val (name, symbol) = info
                val isSelected = selectedCurrency == code
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isSelected) Color(0xFF161616) else Color.Transparent)
                        .clickable { onCurrencyChange(code) }
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = name,
                            fontSize = 16.sp,
                            color = if (isSelected) Color.White else Color(0xFF888888),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                        Text(
                            text = code,
                            fontSize = 12.sp,
                            color = Color(0xFF666666)
                        )
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = symbol,
                            fontSize = 16.sp,
                            color = if (isSelected) Color.White else Color(0xFF888888),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                        if (isSelected) {
                            Text(
                                text = "✓",
                                fontSize = 18.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                HorizontalDivider(color = Color(0xFF1E1E1E))
            }
        }
    }
}

@Composable
fun HelpCenterOverlay(
    userEmail: String,
    onClearAllData: () -> Unit,
    onDeleteAccount: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showClearConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0C0C0C))
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "help_center",
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF888888)
            )
            
            Text(
                text = "✕",
                fontSize = 22.sp,
                color = Color.White,
                fontWeight = FontWeight.Light,
                modifier = Modifier
                    .clickable { onDismiss() }
                    .padding(8.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "FAQ",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF666666),
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        val faqs = listOf(
            "How do I split an expense?" to "Navigate to the Split tab, ensure you have connected with a friend using their email, and tap \"Add Split Expense\". You can enter the amount and description there.",
            "How do I settle up?" to "In the Split tab, next to your balance, there is a \"Settle Up\" button. Use this when you have paid a friend in cash or via external apps to reset your balance.",
            "How do I change the currency?" to "Go back to Settings > Preferences > Currency to select from a list of global currencies."
        )
        
        faqs.forEach { (question, answer) ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF161616))
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        text = question,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = answer,
                        fontSize = 13.sp,
                        color = Color(0xFF888888),
                        lineHeight = 18.sp
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "CONTACT SUPPORT",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF666666),
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF161616))
                .padding(20.dp)
        ) {
            Column {
                Text(
                    text = "If you're experiencing bugs or need further assistance, please reach out to our team.",
                    fontSize = 13.sp,
                    color = Color(0xFF888888),
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Text(
                    text = userEmail,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "DANGER ZONE",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF666666),
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFFFF7C7C))
                .clickable {
                    if (showClearConfirm) {
                        onClearAllData()
                        showClearConfirm = false
                    } else {
                        showClearConfirm = true
                    }
                }
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (showClearConfirm) "Confirm Clear All Data" else "clear all data",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 40.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(width = 1.dp, color = Color(0xFFFF7C7C), shape = RoundedCornerShape(20.dp))
                .clickable {
                    if (showDeleteConfirm) {
                        onDeleteAccount()
                        showDeleteConfirm = false
                    } else {
                        showDeleteConfirm = true
                    }
                }
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (showDeleteConfirm) "Confirm Delete Account" else "Delete Account",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF7C7C)
            )
        }
    }
}

@Composable
fun SettingsOverlay(
    userEmail: String,
    userName: String,
    selectedCurrency: String,
    onShowCurrency: () -> Unit,
    selectedFontSize: String,
    onFontSizeChange: (String) -> Unit,
    onLogout: () -> Unit,
    onDismiss: () -> Unit,
    onShowNotifications: () -> Unit,
    onShowNotificationSettings: () -> Unit,
    onShowWidgetSettings: () -> Unit,
    onShowHelpCenter: () -> Unit,
    onShowLanguage: () -> Unit,
    selectedLanguage: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Header Row of Settings
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Hello, $userName ",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "👋",
                    fontSize = 28.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "🔔",
                    fontSize = 28.sp,
                    modifier = Modifier
                        .clickable { onShowNotifications() }
                        .padding(8.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "✕",
                    fontSize = 28.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Light,
                    modifier = Modifier
                        .clickable { onDismiss() }
                        .padding(8.dp)
                )
            }
        }

        Text(
            text = "setting",
            fontSize = 18.sp,
            color = Color(0xFF666666),
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // ACCOUNT SECTION
        Text(
            text = "ACCOUNT",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF666666),
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "logged in as", fontSize = 15.sp, color = Color.White)
            val uName = userName
            Text(text = uName, fontSize = 15.sp, color = Color(0xFF888888))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // PREFERENCES SECTION
        Text(
            text = "PREFERENCES",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF666666),
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Currency Settings
        val currSymbol = when (selectedCurrency) {
            "USD" -> "$"
            "EUR" -> "€"
            "GBP" -> "£"
            else -> "₹"
        }
        val currName = when (selectedCurrency) {
            "USD" -> "US Dollar"
            "EUR" -> "Euro"
            "GBP" -> "British Pound"
            else -> "Indian Rupee"
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onShowCurrency() }
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = trans("currency", selectedLanguage), fontSize = 15.sp, color = Color.White)
                Text(text = currName, fontSize = 12.sp, color = Color(0xFF888888))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E1E1E)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = currSymbol, fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "〉", fontSize = 12.sp, color = Color(0xFF666666))
            }
        }

        HorizontalDivider(color = Color(0xFF1E1E1E))

        // Language Settings
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onShowLanguage() }
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = trans("language", selectedLanguage), fontSize = 15.sp, color = Color.White)
                Text(text = selectedLanguage, fontSize = 12.sp, color = Color(0xFF888888))
            }
            Text(text = "〉", fontSize = 12.sp, color = Color(0xFF666666))
        }

        HorizontalDivider(color = Color(0xFF1E1E1E))

        // Theme Settings
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "Theme", fontSize = 15.sp, color = Color.White)
                Text(text = "Dark", fontSize = 12.sp, color = Color(0xFF888888))
            }
            Text(text = "〉", fontSize = 12.sp, color = Color(0xFF666666))
        }

        HorizontalDivider(color = Color(0xFF1E1E1E))

        // Notification Settings preferences row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onShowNotificationSettings() }
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = trans("notification_settings", selectedLanguage), fontSize = 15.sp, color = Color.White)
            Text(text = "〉", fontSize = 12.sp, color = Color(0xFF666666))
        }

        HorizontalDivider(color = Color(0xFF1E1E1E))

        // Widget Settings preferences row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onShowWidgetSettings() }
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = trans("widget_settings", selectedLanguage), fontSize = 15.sp, color = Color.White)
            Text(text = "〉", fontSize = 12.sp, color = Color(0xFF666666))
        }

        HorizontalDivider(color = Color(0xFF1E1E1E))

        // Font Size Settings Selector
        Column(modifier = Modifier.padding(vertical = 16.dp)) {
            Text(
                text = "font size",
                fontSize = 15.sp,
                color = Color.White,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf("Small", "Medium", "Large").forEach { size ->
                    val active = selectedFontSize == size
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (active) Color.White else Color.Transparent)
                            .border(width = 1.dp, color = if (active) Color.White else Color(0xFF333333), shape = RoundedCornerShape(20.dp))
                            .clickable { onFontSizeChange(size) }
                            .padding(vertical = 8.dp, horizontal = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = size,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (active) Color.Black else Color.White
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ABOUT SECTION
        Text(
            text = "ABOUT",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF666666),
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onShowHelpCenter() }
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = trans("help_center", selectedLanguage), fontSize = 15.sp, color = Color.White)
                Text(text = trans("FAQ & Support", selectedLanguage), fontSize = 12.sp, color = Color(0xFF888888))
            }
            Text(text = "〉", fontSize = 12.sp, color = Color(0xFF666666))
        }

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .padding(vertical = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFFF7C7C)) // Precise pinkish red background
                .clickable { onLogout() }
                .padding(vertical = 12.dp, horizontal = 28.dp)
        ) {
            Text(
                text = "sign out",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Tagline footers matching Image 3
        Text(
            text = "no clutter, just clarity.",
            fontSize = 12.sp,
            color = Color(0xFF555555),
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "track what matters, skip what doesn't.",
            fontSize = 12.sp,
            color = Color(0xFF555555),
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 40.dp)
        )
    }
}

// ================= MODULAR TAB CONTENTS =================

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WalletTabContent(
    balance: Double,
    cashBalance: Double,
    onlineBalance: Double,
    dailyNet: Double,
    isProfit: Boolean,
    dailyColor: Color,
    dailySign: String,
    presets: List<LogPreset>,
    onLogPreset: (LogPreset) -> Unit,
    onDeletePreset: (LogPreset) -> Unit,
    onCreatePresetClick: () -> Unit,
    onAddIncomeClick: () -> Unit,
    onAddExpenseClick: () -> Unit,
    onShowSnapshot: () -> Unit,
    currentLanguage: String,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Balance aggregates
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            Text(
                text = trans("balance", currentLanguage) + "  ",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF666666),
                letterSpacing = 0.3.sp
            )
            Text(
                text = "$currencySymbol ${formatIndian(balance)}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF888888)
            )
        }

        // Sub cash/online meters
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 36.dp)
        ) {
            Text(
                text = "CASH  ",
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF666666),
                letterSpacing = 0.5.sp
            )
            Text(
                text = "$currencySymbol ${formatIndian(cashBalance)}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF888888)
            )
            Text(
                text = "   •   ONLINE  ",
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF666666),
                letterSpacing = 0.5.sp
            )
            Text(
                text = "$currencySymbol ${formatIndian(onlineBalance)}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF888888)
            )
        }

        // Hero section showing daily spent metrics
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = trans("today", currentLanguage).uppercase(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF666666),
                letterSpacing = 1.2.sp
            )
            Text(
                text = "$dailySign${formatIndian(dailyNet)}",
                fontSize = 46.sp,
                fontWeight = FontWeight.ExtraBold,
                color = dailyColor,
                letterSpacing = (-1.5).sp,
                lineHeight = 52.sp
            )
            Text(
                text = trans(if (isProfit) "net positive" else "net spend", currentLanguage),
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF666666),
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Quick Log Presets Container
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp)
        ) {
            Text(
                text = "QUICK LOG",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF666666),
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(end = 20.dp)
            ) {
                items(presets) { preset ->
                    var showDeleteConfirm by remember { mutableStateOf(false) }
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFF1E1E1E))
                            .border(width = 1.dp, color = Color(0xFF2C2C2C), shape = RoundedCornerShape(24.dp))
                            .combinedClickable(
                                onClick = { onLogPreset(preset) },
                                onLongClick = { showDeleteConfirm = true }
                            )
                            .padding(vertical = 12.dp, horizontal = 16.dp)
                            .widthIn(min = 130.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val iconBg = if (preset.type == "income") PennyTheme.colors.successLight.copy(alpha = 0.15f) else PennyTheme.colors.danger.copy(alpha = 0.15f)
                            val iconColor = if (preset.type == "income") PennyTheme.colors.successLight else PennyTheme.colors.danger
                            
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(iconBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (preset.type == "income") "+" else "−",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = iconColor
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            Column {
                                Text(
                                    text = preset.description,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "$currencySymbol${preset.amount.toLong()}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF888888)
                                )
                            }
                        }
                    }

                    if (showDeleteConfirm) {
                        AlertDialog(
                            onDismissRequest = { showDeleteConfirm = false },
                            title = { Text("Delete Preset", fontWeight = FontWeight.Bold, color = Color.White) },
                            text = { Text("Are you sure you want to remove \"${preset.description}\"?", color = Color.White) },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        onDeletePreset(preset)
                                        showDeleteConfirm = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = PennyTheme.colors.danger)
                                ) {
                                    Text("Delete", color = Color.White)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteConfirm = false }) {
                                    Text("Cancel", color = Color.White)
                                }
                            },
                            containerColor = Color(0xFF1E1E1E),
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }

                item {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1A1A1A))
                            .clickable { onCreatePresetClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Light,
                            color = Color(0xFF888888)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Wallet Control FAB cluster
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 30.dp),
            horizontalAlignment = Alignment.End
        ) {
            Box(
                modifier = Modifier
                    .size(74.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable { onAddIncomeClick() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Light,
                    color = Color.Black
                )
            }
            
            Spacer(modifier = Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    modifier = Modifier
                        .size(82.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable { onShowSnapshot() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "↗",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.Black
                    )
                }
                
                Box(
                    modifier = Modifier
                        .size(82.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable { onAddExpenseClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "−",
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Light,
                        color = Color.Black
                    )
                }
            }
        }
    }
}

@Composable
fun ActivityTabContent(
    transactions: List<WalletTransaction>,
    onDeleteTransaction: (WalletTransaction) -> Unit,
    currentLanguage: String,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = trans("Activity", currentLanguage).lowercase(),
            fontSize = 16.sp,
            color = Color(0xFF666666),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = trans("no transactions yet", currentLanguage),
                    fontSize = 15.sp,
                    color = Color(0xFF666666)
                )
            }
        } else {
            // Group transactions by date for beautiful sectioning
            val grouped = transactions.groupBy { tx ->
                val todayStartMs = System.currentTimeMillis() - (System.currentTimeMillis() % 86400000)
                val yesterdayStartMs = todayStartMs - 86400000
                when {
                    tx.date >= todayStartMs -> trans("today", currentLanguage).uppercase()
                    tx.date >= yesterdayStartMs -> trans("YESTERDAY", currentLanguage).uppercase()
                    else -> "26 MAY 2026" // Replicate identical label from screenshots!
                }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                grouped.forEach { (sectionDate, items) ->
                    item {
                        Text(
                            text = sectionDate,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF666666),
                            modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                        )
                    }

                    items(items) { tx ->
                        var showDeleteConfirm by remember { mutableStateOf(false) }

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showDeleteConfirm = true }
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    val sign = if (tx.type == "income") "+" else "−"
                                    val amtColor = if (tx.type == "income") PennyTheme.colors.successLight else PennyTheme.colors.danger
                                    
                                    Text(
                                        text = "$sign $currencySymbol${formatIndian(tx.amount)}",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = amtColor,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                    
                                    Text(
                                        text = tx.description,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    
                                    Text(
                                        text = "20:44  •  ${tx.method.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }}",
                                        fontSize = 12.sp,
                                        color = Color(0xFF666666),
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }

                                // Elegant deletion cross
                                Text(
                                    text = "✕",
                                    fontSize = 15.sp,
                                    color = Color(0xFF444444),
                                    modifier = Modifier
                                        .clickable { showDeleteConfirm = true }
                                        .padding(12.dp)
                                )
                            }
                            
                            HorizontalDivider(color = Color(0xFF1A1A1A))
                        }

                        if (showDeleteConfirm) {
                            AlertDialog(
                                onDismissRequest = { showDeleteConfirm = false },
                                title = { Text("Delete Transaction", fontWeight = FontWeight.Bold, color = Color.White) },
                                text = { Text("Remove \"${tx.description}\" of $currencySymbol${formatIndian(tx.amount)}?", color = Color.White) },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            onDeleteTransaction(tx)
                                            showDeleteConfirm = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = PennyTheme.colors.danger)
                                    ) {
                                        Text("Delete", color = Color.White)
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showDeleteConfirm = false }) {
                                        Text("Cancel", color = Color.White)
                                    }
                                },
                                containerColor = Color(0xFF1E1E1E),
                                shape = RoundedCornerShape(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SplitTabContent(
    connections: List<SplitConnection>,
    activeConnection: SplitConnection,
    splitDropdownExpanded: Boolean,
    splitHistoryList: List<SplitHistoryItem>,
    pendingSentRequests: List<NotificationItem>,
    pendingReceivedRequests: List<NotificationItem>,
    onConfirmRequest: (NotificationItem) -> Unit,
    onDeclineRequest: (NotificationItem) -> Unit,
    onConnectionChange: (SplitConnection) -> Unit,
    onDropdownToggle: (Boolean) -> Unit,
    onAddFriendClick: () -> Unit,
    onSettleUpClick: () -> Unit,
    onAddSplitExpenseClick: () -> Unit,
    onRemoveFriendClick: (SplitConnection) -> Unit,
    currentLanguage: String,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = trans("split expenses", currentLanguage),
                fontSize = 16.sp,
                color = Color(0xFF666666)
            )

            // Dropdown Pill matching Image 1
            Box {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF222222))
                        .clickable { onDropdownToggle(!splitDropdownExpanded) }
                        .padding(vertical = 8.dp, horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${extractFirstName(activeConnection.name)} ",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "▼",
                        fontSize = 10.sp,
                        color = Color.White
                    )
                }

                // Custom overlay list for dropdown menu wrapped in Popup for a true float
                if (splitDropdownExpanded) {
                    androidx.compose.ui.window.Popup(
                        alignment = Alignment.TopEnd,
                        onDismissRequest = { onDropdownToggle(false) }
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 44.dp)
                                .width(180.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF1E1E1E))
                                .border(width = 1.dp, color = Color(0xFF333333), shape = RoundedCornerShape(16.dp))
                        ) {
                            Column {
                                connections.forEach { conn ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                onConnectionChange(conn)
                                                onDropdownToggle(false)
                                            }
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = conn.name,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.weight(1f)
                                        )
                                        
                                        // 3-dots with click trigger to pop up friend removal dialog
                                        Text(
                                            text = "⋮",
                                            fontSize = 14.sp,
                                            color = Color(0xFF666666),
                                            modifier = Modifier
                                                .clickable {
                                                    onDropdownToggle(false)
                                                    onRemoveFriendClick(conn)
                                                }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    HorizontalDivider(color = Color(0xFF333333))
                                }
                                
                                // Add friend connection trigger
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onDropdownToggle(false)
                                            onAddFriendClick()
                                        }
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "+ Add a Friend",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PennyTheme.colors.successLight
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Centered Main balance card matching Image 1
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFF161616)) // Dark grey card background
                .border(width = 1.dp, color = Color(0xFF222222), shape = RoundedCornerShape(28.dp))
                .padding(horizontal = 24.dp, vertical = 28.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val activeConnFirstName = extractFirstName(activeConnection.name)
                Text(
                    text = "${trans("BALANCE WITH", currentLanguage)} ${activeConnFirstName.uppercase()}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF666666),
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                val activeBal = activeConnection.balance
                val owesText = if (activeBal >= 0) {
                    "${trans("Owes you", currentLanguage)} ${currencySymbol}${activeBal.toLong()}"
                } else {
                    "${trans("You owe", currentLanguage)} ${currencySymbol}${kotlin.math.abs(activeBal).toLong()}"
                }
                val activeBalColor = if (activeBal >= 0) PennyTheme.colors.successLight else PennyTheme.colors.danger
                
                Text(
                    text = owesText,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = activeBalColor
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Settle / Split Expense Buttons Row inside Card
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Settle Up button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFF262626))
                            .clickable { onSettleUpClick() }
                            .padding(vertical = 12.dp, horizontal = 24.dp)
                    ) {
                        Text(
                            text = trans("Settle Up", currentLanguage),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // + add split expense button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.White)
                            .clickable { onAddSplitExpenseClick() }
                            .padding(vertical = 12.dp, horizontal = 24.dp)
                    ) {
                        Text(
                            text = trans("+ add split expense", currentLanguage),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
            }
        }

        val pendingSent = pendingSentRequests.filter { it.connectionId == activeConnection.id }
        val pendingReceived = pendingReceivedRequests.filter { it.connectionId == activeConnection.id }

        if (pendingSent.isNotEmpty() || pendingReceived.isNotEmpty()) {
            Text(
                text = trans("PENDING REQUESTS", currentLanguage),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = PennyTheme.colors.successLight,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                // Received requests first
                pendingReceived.forEach { req ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF1E1414))
                            .border(width = 1.dp, color = Color(0xFF3A1C1C), shape = RoundedCornerShape(20.dp))
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            val fromNameFirst = extractFirstName(req.fromName)
                            val requestedText = if (req.type == "connection_request") {
                                if (currentLanguage == "हिंदी") {
                                    "${fromNameFirst} ने आपके साथ कनेक्शन जोड़ा है"
                                } else if (currentLanguage == "ગુજરાતી") {
                                    "${fromNameFirst} એ તમારી સાથે જોડાણ કર્યું છે"
                                } else if (currentLanguage == "Español") {
                                    "${fromNameFirst} se conectó contigo"
                                } else {
                                    "${fromNameFirst} added you as a connection"
                                }
                            } else {
                                if (currentLanguage == "हिंदी") {
                                    "${fromNameFirst} ने ${currencySymbol}${req.amount.toLong()} का अनुरोध किया"
                                } else if (currentLanguage == "ગુજરાતી") {
                                    "${fromNameFirst} એ ${currencySymbol}${req.amount.toLong()} ની વિનંતી કરી"
                                } else if (currentLanguage == "Español") {
                                    "${fromNameFirst} solicitó ${currencySymbol}${req.amount.toLong()}"
                                } else {
                                    "${fromNameFirst} requested ${currencySymbol}${req.amount.toLong()}"
                                }
                            }

                            Text(
                                text = requestedText,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = req.description,
                                fontSize = 12.sp,
                                color = Color(0xFF888888),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (req.type != "connection_request") {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0xFF2E1717))
                                        .clickable { onDeclineRequest(req) }
                                        .padding(vertical = 6.dp, horizontal = 12.dp)
                                ) {
                                    Text(
                                        text = if (currentLanguage == "हिंदी") "अस्वीकार करें" else if (currentLanguage == "ગુજરાતી") "નકારો" else if (currentLanguage == "Español") "Rechazar" else "Decline",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PennyTheme.colors.danger
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.White)
                                    .clickable { onConfirmRequest(req) }
                                    .padding(vertical = 6.dp, horizontal = 12.dp)
                            ) {
                                Text(
                                    text = if (currentLanguage == "हिंदी") "पुष्टि करें" else if (currentLanguage == "ગુજરાતી") "મંજૂર કરો" else if (currentLanguage == "Español") "Confirmar" else "Confirm",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }

                // Sent requests
                pendingSent.forEach { req ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF141A1E))
                            .border(width = 1.dp, color = Color(0xFF1C2A34), shape = RoundedCornerShape(20.dp))
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            val friendName = extractFirstName(activeConnection.name)
                            val waitingText = if (currentLanguage == "हिंदी") {
                                "$friendName द्वारा ${currencySymbol}${req.amount.toLong()} की पुष्टि करने की प्रतीक्षा है"
                            } else if (currentLanguage == "ગુજરાતી") {
                                "$friendName દ્વારા ${currencySymbol}${req.amount.toLong()} ની મંજૂરી મેળવવાની રાહ છે"
                            } else if (currentLanguage == "Español") {
                                "Esperando que $friendName confirme ${currencySymbol}${req.amount.toLong()}"
                            } else {
                                "Waiting for $friendName to confirm ${currencySymbol}${req.amount.toLong()}"
                            }

                            Text(
                                text = waitingText,
                                fontSize = 13.sp,
                                fontStyle = FontStyle.Italic,
                                color = Color(0xFFCCCCCC)
                            )
                            if (req.description.isNotEmpty()) {
                                Text(
                                    text = req.description,
                                    fontSize = 11.sp,
                                    color = Color(0xFF666666),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // History section
        Text(
            text = trans("HISTORY", currentLanguage),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF666666),
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            items(splitHistoryList) { hist ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF161616))
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = hist.description,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        
                        val splitVerb = if (hist.isYouSplit) trans("You split", currentLanguage) else "${extractFirstName(activeConnection.name)} ${trans("split", currentLanguage)}"
                        val subLabel = "$splitVerb $currencySymbol${kotlin.math.abs(hist.amount).toLong()}"
                        Text(
                            text = subLabel,
                            fontSize = 12.sp,
                            color = Color(0xFF666666),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    val sign = if (hist.amount >= 0) "+" else "−"
                    val amtColor = if (hist.amount >= 0) PennyTheme.colors.successLight else PennyTheme.colors.danger
                    
                    Text(
                        text = "$sign$currencySymbol${kotlin.math.abs(hist.amount).toLong()}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = amtColor
                    )
                }
            }
        }
    }
}

// ================= MODULAR SUB-COMPOSABLES FOR OVERLAY DIALOGS =================

@Composable
fun TransactionCreatorSheet(
    actionType: String,
    onDismiss: () -> Unit,
    onSaveTransaction: (Double, String, String) -> Unit,
    currencySymbol: String = "₹",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PennyTheme.colors.overlay)
            .clickable { onDismiss() }
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(Color(0xFF161616))
                .border(width = 1.dp, color = Color(0xFF222222), shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .clickable(enabled = true, onClick = {}) // swallow clicks
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 28.dp, vertical = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF333333))
                    .padding(bottom = 24.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (actionType == "income") "add income" else "add expense",
                fontSize = 22.sp,
                fontWeight = FontWeight.Light,
                color = Color(0xFF888888),
                modifier = Modifier.padding(bottom = 24.dp)
            )

            var txAmountText by remember { mutableStateOf("") }
            var txMethod by remember { mutableStateOf("online") }
            var txDescText by remember { mutableStateOf("") }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Text(
                    text = currencySymbol,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(end = 8.dp)
                )
                BasicTextField(
                    value = txAmountText,
                    onValueChange = { input -> 
                        if (input.all { it.isDigit() || it == '.' }) {
                            txAmountText = input
                        }
                    },
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { innerTextField ->
                        if (txAmountText.isEmpty()) {
                            Text(
                                text = "0.00",
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF444444)
                            )
                        }
                        innerTextField()
                    }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1E1E1E))
                    .padding(4.dp)
                    .height(44.dp)
            ) {
                val onlineActive = txMethod == "online"
                val cashActive = txMethod == "cash"
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (onlineActive) Color(0xFF2C2C2C) else Color.Transparent)
                        .clickable { txMethod = "online" },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Online",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (onlineActive) Color.White else Color(0xFF888888)
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (cashActive) Color(0xFF2C2C2C) else Color.Transparent)
                        .clickable { txMethod = "cash" },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Cash",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (cashActive) Color.White else Color(0xFF888888)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1E1E1E))
                    .padding(vertical = 8.dp, horizontal = 16.dp)
            ) {
                Text(
                    text = "🗓️ Today, 28 May, 12:30 PM",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            BasicTextField(
                value = txDescText,
                onValueChange = { txDescText = it },
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 16.sp,
                    color = Color.White
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        val amt = txAmountText.toDoubleOrNull()
                        if (amt != null && amt > 0) {
                            onSaveTransaction(amt, txMethod, txDescText)
                        }
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                decorationBox = { innerTextField ->
                    Column {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            if (txDescText.isEmpty()) {
                                Text(
                                    text = "what was it for?",
                                    fontSize = 16.sp,
                                    color = Color(0xFF666666)
                                )
                            }
                            innerTextField()
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color(0xFF333333))
                                .padding(top = 4.dp)
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "cancel",
                    fontSize = 16.sp,
                    color = Color(0xFF888888),
                    modifier = Modifier
                        .clickable { onDismiss() }
                        .padding(vertical = 14.dp, horizontal = 24.dp)
                )

                val amt = txAmountText.toDoubleOrNull() ?: 0.0
                val isSaveEnabled = amt > 0.0

                Button(
                    onClick = { onSaveTransaction(amt, txMethod, txDescText) },
                    enabled = isSaveEnabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSaveEnabled) Color.White else Color(0xFF333333),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(30.dp),
                    contentPadding = PaddingValues(vertical = 14.dp, horizontal = 40.dp)
                ) {
                    Text(
                        text = "save",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun PresetCreatorSheet(
    onDismiss: () -> Unit,
    onSavePreset: (String, Double, String, String) -> Unit,
    currencySymbol: String = "₹",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PennyTheme.colors.overlay)
            .clickable { onDismiss() }
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(Color(0xFF161616))
                .border(width = 1.dp, color = Color(0xFF222222), shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .clickable(enabled = true, onClick = {})
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 28.dp, vertical = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF333333))
                    .padding(bottom = 24.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "create preset",
                fontSize = 22.sp,
                fontWeight = FontWeight.Light,
                color = Color(0xFF888888),
                modifier = Modifier.padding(bottom = 24.dp)
            )

            var prType by remember { mutableStateOf("expense") }
            var prAmountText by remember { mutableStateOf("") }
            var prMethod by remember { mutableStateOf("cash") }
            var prDescText by remember { mutableStateOf("") }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1E1E1E))
                    .padding(4.dp)
                    .height(44.dp)
            ) {
                val expenseActive = prType == "expense"
                val incomeActive = prType == "income"
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (expenseActive) Color(0xFF2C2C2C) else Color.Transparent)
                        .clickable { prType = "expense" },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Expense",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (expenseActive) Color.White else Color(0xFF888888)
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (incomeActive) Color(0xFF2C2C2C) else Color.Transparent)
                        .clickable { prType = "income" },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Income",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (incomeActive) Color.White else Color(0xFF888888)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Text(
                    text = currencySymbol,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(end = 8.dp)
                )
                BasicTextField(
                    value = prAmountText,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() || it == '.' }) {
                            prAmountText = input
                        }
                    },
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { innerTextField ->
                        if (prAmountText.isEmpty()) {
                            Text(
                                text = "0.00",
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF444444)
                            )
                        }
                        innerTextField()
                    }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1E1E1E))
                    .padding(4.dp)
                    .height(44.dp)
            ) {
                val onlineActive = prMethod == "online"
                val cashActive = prMethod == "cash"
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (onlineActive) Color(0xFF2C2C2C) else Color.Transparent)
                        .clickable { prMethod = "online" },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Online",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (onlineActive) Color.White else Color(0xFF888888)
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (cashActive) Color(0xFF2C2C2C) else Color.Transparent)
                        .clickable { prMethod = "cash" },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Cash",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (cashActive) Color.White else Color(0xFF888888)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            BasicTextField(
                value = prDescText,
                onValueChange = { prDescText = it },
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 16.sp,
                    color = Color.White
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                decorationBox = { innerTextField ->
                    Column {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            if (prDescText.isEmpty()) {
                                Text(
                                    text = "preset name (e.g. Tea & Chips)",
                                    fontSize = 16.sp,
                                    color = Color(0xFF666666)
                                )
                            }
                            innerTextField()
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color(0xFF333333))
                                .padding(top = 4.dp)
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "cancel",
                    fontSize = 16.sp,
                    color = Color(0xFF888888),
                    modifier = Modifier
                        .clickable { onDismiss() }
                        .padding(vertical = 14.dp, horizontal = 24.dp)
                )

                val amt = prAmountText.toDoubleOrNull() ?: 0.0
                val isSaveEnabled = amt > 0.0 && prDescText.trim().isNotEmpty()

                Button(
                    onClick = { onSavePreset(prType, amt, prMethod, prDescText) },
                    enabled = isSaveEnabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSaveEnabled) Color.White else Color(0xFF333333),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(30.dp),
                    contentPadding = PaddingValues(vertical = 14.dp, horizontal = 40.dp)
                ) {
                    Text(
                        text = "save preset",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun SnapshotCardOverlay(
    balance: Double,
    dailyNet: Double,
    transactionsCount: Int,
    transactions: List<WalletTransaction>,
    onDismiss: () -> Unit,
    onDownloadReport: () -> Unit,
    currencySymbol: String = "₹",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PennyTheme.colors.overlay)
            .clickable { onDismiss() }
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFF161616))
                .border(width = 1.dp, color = Color(0xFF222222), shape = RoundedCornerShape(28.dp))
                .clickable(enabled = true, onClick = {})
                .padding(32.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 28.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "how am i doing?",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF888888),
                    letterSpacing = 1.2.sp
                )
                
                Text(
                    text = "📄",
                    fontSize = 20.sp,
                    modifier = Modifier
                        .clickable { onDownloadReport() }
                        .padding(4.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "total balance", fontSize = 15.sp, color = Color(0xFF888888))
                Text(text = "${currencySymbol} ${formatIndian(balance)}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            HorizontalDivider(color = Color(0xFF222222), modifier = Modifier.padding(vertical = 8.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "today", fontSize = 15.sp, color = Color(0xFF888888))
                val sign = if (dailyNet >= 0) "+" else ""
                Text(
                    text = "${sign}${currencySymbol} ${formatIndian(dailyNet)}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (dailyNet >= 0) Color.White else Color(0xFF888888)
                )
            }

            val weekAgo = System.currentTimeMillis() - 604800000
            val weeklyNet = transactions.filter { it.date >= weekAgo }.fold(0.0) { sum, tx -> sum + if (tx.type == "income") tx.amount else -tx.amount }
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "this week", fontSize = 15.sp, color = Color(0xFF888888))
                val wSign = if (weeklyNet >= 0) "+" else ""
                Text(
                    text = "${wSign}${currencySymbol} ${formatIndian(weeklyNet)}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (weeklyNet >= 0) Color.White else Color(0xFF888888)
                )
            }

            HorizontalDivider(color = Color(0xFF222222), modifier = Modifier.padding(vertical = 8.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "transactions logged", fontSize = 15.sp, color = Color(0xFF888888))
                Text(text = "$transactionsCount", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Text(
                text = "tap anywhere to close",
                fontSize = 11.sp,
                color = Color(0xFF444444),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
            )
        }
    }
}

@Composable
fun ReportDurationSheet(
    onDismiss: () -> Unit,
    onDownloadPdf: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PennyTheme.colors.overlay)
            .clickable { onDismiss() }
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(Color(0xFF161616))
                .border(width = 1.dp, color = Color(0xFF222222), shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .clickable(enabled = true, onClick = {})
                .navigationBarsPadding()
                .padding(horizontal = 28.dp, vertical = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF333333))
                    .padding(bottom = 24.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "download report",
                fontSize = 22.sp,
                fontWeight = FontWeight.Light,
                color = Color(0xFF888888),
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Text(
                text = "SELECT DURATION",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF888888),
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            var reportDays by remember { mutableStateOf(7) }

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf(
                    7 to "1 Week",
                    14 to "2 Weeks",
                    30 to "1 Month"
                ).forEach { (days, label) ->
                    val active = reportDays == days
                    val optionBg = if (active) PennyTheme.colors.successLight.copy(alpha = 0.15f) else Color(0xFF0C0C0C)
                    val optionBorder = if (active) PennyTheme.colors.successLight else Color(0xFF222222)
                    val optionTextColor = if (active) PennyTheme.colors.successLight else Color(0xFF888888)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(optionBg)
                            .border(width = 1.dp, color = optionBorder, shape = RoundedCornerShape(16.dp))
                            .clickable { reportDays = days }
                            .padding(vertical = 16.dp, horizontal = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 16.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                            color = optionTextColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { onDownloadPdf(reportDays) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = PennyTheme.colors.successLight,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(30.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = "Download PDF",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
fun SplitBillSheet(
    connection: SplitConnection,
    onDismiss: () -> Unit,
    onSaveSplit: (Double, String, String) -> Unit,
    currencySymbol: String = "₹",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PennyTheme.colors.overlay)
            .clickable { onDismiss() }
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(Color(0xFF161616))
                .border(width = 1.dp, color = Color(0xFF222222), shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .clickable(enabled = true, onClick = {})
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 28.dp, vertical = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF333333))
                    .padding(bottom = 24.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "split expense with ${extractFirstName(connection.name)}",
                fontSize = 20.sp,
                fontWeight = FontWeight.Light,
                color = Color(0xFF888888),
                modifier = Modifier.padding(bottom = 24.dp)
            )

            var splitAmountText by remember { mutableStateOf("") }
            var splitDescText by remember { mutableStateOf("") }
            var splitWhoPaid by remember { mutableStateOf("me") }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Text(
                    text = currencySymbol,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(end = 8.dp)
                )
                BasicTextField(
                    value = splitAmountText,
                    onValueChange = { input -> 
                        if (input.all { it.isDigit() || it == '.' }) {
                            splitAmountText = input
                        }
                    },
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { innerTextField ->
                        if (splitAmountText.isEmpty()) {
                            Text(
                                text = "0.00",
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF444444)
                            )
                        }
                        innerTextField()
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            BasicTextField(
                value = splitDescText,
                onValueChange = { splitDescText = it },
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 16.sp,
                    color = Color.White
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                decorationBox = { innerTextField ->
                    Column {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            if (splitDescText.isEmpty()) {
                                Text(
                                    text = "what was this bill for?",
                                    fontSize = 16.sp,
                                    color = Color(0xFF666666)
                                )
                            }
                            innerTextField()
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color(0xFF333333))
                                .padding(top = 4.dp)
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "cancel",
                    fontSize = 16.sp,
                    color = Color(0xFF888888),
                    modifier = Modifier
                        .clickable { onDismiss() }
                        .padding(vertical = 14.dp, horizontal = 24.dp)
                )

                val amt = splitAmountText.toDoubleOrNull() ?: 0.0
                val isSaveEnabled = amt > 0.0

                Button(
                    onClick = { onSaveSplit(amt, splitWhoPaid, splitDescText) },
                    enabled = isSaveEnabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSaveEnabled) Color.White else Color(0xFF333333),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(30.dp),
                    contentPadding = PaddingValues(vertical = 14.dp, horizontal = 40.dp)
                ) {
                    Text(
                        text = "Split Expense",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun SettleUpSheet(
    connection: SplitConnection,
    onDismiss: () -> Unit,
    onSaveSettlement: (Double) -> Unit,
    currencySymbol: String = "₹",
    modifier: Modifier = Modifier
) {
    val owedAmt = connection.balance
    val absoluteDebt = kotlin.math.abs(owedAmt)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PennyTheme.colors.overlay)
            .clickable { onDismiss() }
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(Color(0xFF161616))
                .border(width = 1.dp, color = Color(0xFF222222), shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .clickable(enabled = true, onClick = {})
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 28.dp, vertical = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF333333))
                    .padding(bottom = 24.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            val settleTextTitle = if (owedAmt > 0) "Record payment from ${extractFirstName(connection.name)}" else "Settle up debt with ${extractFirstName(connection.name)}"
            Text(
                text = settleTextTitle,
                fontSize = 20.sp,
                fontWeight = FontWeight.Light,
                color = Color(0xFF888888),
                modifier = Modifier.padding(bottom = 24.dp)
            )

            var settleAmountText by remember { mutableStateOf(absoluteDebt.toString()) }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Text(
                    text = currencySymbol,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(end = 8.dp)
                )
                BasicTextField(
                    value = settleAmountText,
                    onValueChange = { input -> 
                        if (input.all { it.isDigit() || it == '.' }) {
                            settleAmountText = input
                        }
                    },
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { innerTextField ->
                        if (settleAmountText.isEmpty()) {
                            Text(
                                text = "0.00",
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF444444)
                            )
                        }
                        innerTextField()
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (owedAmt > 0) "Recording this will reduce what ${extractFirstName(connection.name)} owes you." else "Recording this will settle what you owe ${extractFirstName(connection.name)}.",
                fontSize = 13.sp,
                color = Color(0xFF888888),
                modifier = Modifier.padding(bottom = 32.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "cancel",
                    fontSize = 16.sp,
                    color = Color(0xFF888888),
                    modifier = Modifier
                        .clickable { onDismiss() }
                        .padding(vertical = 14.dp, horizontal = 24.dp)
                )

                val amt = settleAmountText.toDoubleOrNull() ?: 0.0
                val isSaveEnabled = amt > 0.0

                Button(
                    onClick = { onSaveSettlement(amt) },
                    enabled = isSaveEnabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSaveEnabled) PennyTheme.colors.successLight else Color(0xFF333333),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(30.dp),
                    contentPadding = PaddingValues(vertical = 14.dp, horizontal = 40.dp)
                ) {
                    Text(
                        text = "Record Settlement",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun AddFriendConnectionSheet(
    onDismiss: () -> Unit,
    onSaveFriend: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PennyTheme.colors.overlay)
            .clickable { onDismiss() }
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(Color(0xFF161616))
                .border(width = 1.dp, color = Color(0xFF222222), shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .clickable(enabled = true, onClick = {})
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 28.dp, vertical = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF333333))
                    .padding(bottom = 24.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "add split connection",
                fontSize = 20.sp,
                fontWeight = FontWeight.Light,
                color = Color(0xFF888888),
                modifier = Modifier.padding(bottom = 24.dp)
            )

            var friendNameText by remember { mutableStateOf("") }
            var friendEmailText by remember { mutableStateOf("") }

            // Name Underline Input
            BasicTextField(
                value = friendNameText,
                onValueChange = { friendNameText = it },
                textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, color = Color.White),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                decorationBox = { innerTextField ->
                    Column {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            if (friendNameText.isEmpty()) {
                                Text(text = "friend's name (e.g. Rahul)", fontSize = 16.sp, color = Color(0xFF666666))
                            }
                            innerTextField()
                        }
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF333333)).padding(top = 4.dp))
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Email Underline Input
            BasicTextField(
                value = friendEmailText,
                onValueChange = { friendEmailText = it },
                textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, color = Color.White),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                decorationBox = { innerTextField ->
                    Column {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            if (friendEmailText.isEmpty()) {
                                Text(text = "friend's email address", fontSize = 16.sp, color = Color(0xFF666666))
                            }
                            innerTextField()
                        }
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF333333)).padding(top = 4.dp))
                    }
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "cancel",
                    fontSize = 16.sp,
                    color = Color(0xFF888888),
                    modifier = Modifier
                        .clickable { onDismiss() }
                        .padding(vertical = 14.dp, horizontal = 24.dp)
                )

                val isSaveEnabled = friendNameText.trim().isNotEmpty() && friendEmailText.trim().isNotEmpty()

                Button(
                    onClick = { onSaveFriend(friendNameText.trim(), friendEmailText.trim().lowercase()) },
                    enabled = isSaveEnabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSaveEnabled) Color.White else Color(0xFF333333),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(30.dp),
                    contentPadding = PaddingValues(vertical = 14.dp, horizontal = 40.dp)
                ) {
                    Text(
                        text = "Add Friend",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun RemoveFriendConnectionSheet(
    connection: SplitConnection,
    onDismiss: () -> Unit,
    onConfirmRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PennyTheme.colors.overlay) // Beautiful semi-transparent backdrop
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFF161616)) // Dark card background matching the screenshot
                .border(width = 1.dp, color = Color(0xFF222222), shape = RoundedCornerShape(28.dp))
                .clickable(enabled = true, onClick = {}) // swallow clicks
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Remove ${connection.name}?",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "They will be removed from your split section, but no past transaction data will be deleted.",
                fontSize = 13.sp,
                color = Color(0xFF888888),
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            // remove Coral Red button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFFFF7C7C)) // Coral pink-red button matching screenshot and settings page
                    .clickable { onConfirmRemove() }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "remove",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Cancel grey button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF222222)) // Dark charcoal grey background matching screenshot
                    .clickable { onDismiss() }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Cancel",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun NotificationsSettingsOverlay(
    onDismiss: () -> Unit,
    currentLanguage: String = "English",
    triggerToast: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Local copy of states for the Save flow
    var localPushEnabled by remember { mutableStateOf(getNotificationPref("push_alerts_enabled", "true") == "true") }
    var localReminderEnabled by remember { mutableStateOf(getNotificationPref("periodic_reminders_enabled", "false") == "true") }
    var localReminderInterval by remember { mutableStateOf(getNotificationPref("periodic_reminder_interval_hours", "24").toIntOrNull() ?: 24) }
    var hasPermission by remember { mutableStateOf(isNotificationPermissionGranted()) }

    LaunchedEffect(Unit) {
        hasPermission = isNotificationPermissionGranted()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = trans("notification_settings", currentLanguage),
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF888888)
            )
            
            Text(
                text = "✕",
                fontSize = 22.sp,
                color = Color.White,
                fontWeight = FontWeight.Light,
                modifier = Modifier
                    .clickable { onDismiss() }
                    .padding(8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Preference 1: Push Notifications
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = trans("push_alerts", currentLanguage),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = trans("push_alerts_desc", currentLanguage),
                    fontSize = 12.sp,
                    color = Color(0xFF888888)
                )
            }
            
            Switch(
                checked = localPushEnabled,
                onCheckedChange = { localPushEnabled = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF4CAF50),
                    uncheckedThumbColor = Color(0xFF888888),
                    uncheckedTrackColor = Color(0xFF222222)
                )
            )
        }

        AnimatedVisibility(
            visible = localPushEnabled && !hasPermission,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF2C1A1A))
                    .border(1.dp, Color(0xFFD32F2F).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = trans("Notification Permission Required ⚠️", currentLanguage),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF8A8A)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = trans("System notification permission is disabled. You must enable it in system settings to receive split and connection alerts.", currentLanguage),
                    fontSize = 12.sp,
                    color = Color(0xFFCCCCCC)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        requestNotificationPermission()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD32F2F),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = trans("Enable in Settings", currentLanguage),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        HorizontalDivider(color = Color(0xFF1E1E1E))

        // Preference 2: Logging Reminders
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = trans("periodic_reminders", currentLanguage),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = trans("periodic_reminders_desc", currentLanguage),
                    fontSize = 12.sp,
                    color = Color(0xFF888888)
                )
            }
            
            Switch(
                checked = localReminderEnabled,
                onCheckedChange = { localReminderEnabled = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF4CAF50),
                    uncheckedThumbColor = Color(0xFF888888),
                    uncheckedTrackColor = Color(0xFF222222)
                )
            )
        }

        // Reminder Interval Selector (Visible only when reminders are enabled)
        AnimatedVisibility(
            visible = localReminderEnabled,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                Text(
                    text = trans("reminder_interval", currentLanguage),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val intervals = listOf(
                        3 to trans("3 Hours", currentLanguage),
                        6 to trans("6 Hours", currentLanguage),
                        12 to trans("12 Hours", currentLanguage),
                        24 to trans("Daily", currentLanguage)
                    )
                    
                    intervals.forEach { (hours, label) ->
                        val active = localReminderInterval == hours
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (active) Color.White else Color.Transparent)
                                .border(
                                    width = 1.dp,
                                    color = if (active) Color.White else Color(0xFF333333),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { localReminderInterval = hours }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (active) Color.Black else Color.White
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        HorizontalDivider(color = Color(0xFF1E1E1E))

        Spacer(modifier = Modifier.weight(1f))

        // Stacked Action Buttons
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Button 1: Save Preferences (Primary)
            Button(
                onClick = {
                    saveNotificationPref("push_alerts_enabled", localPushEnabled.toString())
                    saveNotificationPref("periodic_reminders_enabled", localReminderEnabled.toString())
                    saveNotificationPref("periodic_reminder_interval_hours", localReminderInterval.toString())
                    
                    // Reschedule background work manager based on new settings (enabled if push alerts OR reminders are enabled)
                    scheduleWorkManagerReminders(localReminderInterval, localPushEnabled || localReminderEnabled)
                    
                    // Show success feedback
                    triggerToast(trans("Preferences saved successfully", currentLanguage))
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = trans("Save Preferences", currentLanguage),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Button 2: Send Test Alert (Secondary)
            Button(
                onClick = {
                    triggerImmediateLocalNotification(
                        trans("Penny Test Alert 🔔", currentLanguage),
                        trans("Your notification channels are active and working perfectly!", currentLanguage)
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1C1C1E),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFF2C2C2E)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = trans("Send Test Alert", currentLanguage),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun WidgetSettingsOverlay(
    connections: List<SplitConnection>,
    presets: List<LogPreset>,
    onDismiss: () -> Unit,
    currentLanguage: String = "English",
    currencySymbol: String = "₹",
    triggerToast: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // SharedPreferences local variables
    var localSelectedConnectionId by remember { 
        mutableStateOf(getNotificationPref("widget_selected_connection_id", "active")) 
    }
    
    // Slot Selection State
    var currentPresetSlot by remember { mutableStateOf(1) } // 1, 2, or 3
    
    // Slot dynamic mappings: local copies
    var localPreset1Name by remember { mutableStateOf(getNotificationPref("widget_preset_1_name", "Tea & Chips")) }
    var localPreset1Amt by remember { mutableStateOf(getNotificationPref("widget_preset_1_amount", "20")) }
    var localPreset1Type by remember { mutableStateOf(getNotificationPref("widget_preset_1_type", "expense")) }
    var localPreset1Method by remember { mutableStateOf(getNotificationPref("widget_preset_1_method", "cash")) }

    var localPreset2Name by remember { mutableStateOf(getNotificationPref("widget_preset_2_name", "Metro Ride")) }
    var localPreset2Amt by remember { mutableStateOf(getNotificationPref("widget_preset_2_amount", "40")) }
    var localPreset2Type by remember { mutableStateOf(getNotificationPref("widget_preset_2_type", "expense")) }
    var localPreset2Method by remember { mutableStateOf(getNotificationPref("widget_preset_2_method", "online")) }

    var localPreset3Name by remember { mutableStateOf(getNotificationPref("widget_preset_3_name", "Office Lunch")) }
    var localPreset3Amt by remember { mutableStateOf(getNotificationPref("widget_preset_3_amount", "150")) }
    var localPreset3Type by remember { mutableStateOf(getNotificationPref("widget_preset_3_type", "expense")) }
    var localPreset3Method by remember { mutableStateOf(getNotificationPref("widget_preset_3_method", "online")) }

    // Helper properties to get active slot selection
    val activeName = when (currentPresetSlot) {
        1 -> localPreset1Name
        2 -> localPreset2Name
        else -> localPreset3Name
    }
    val activeAmt = when (currentPresetSlot) {
        1 -> localPreset1Amt
        2 -> localPreset2Amt
        else -> localPreset3Amt
    }
    val activeType = when (currentPresetSlot) {
        1 -> localPreset1Type
        2 -> localPreset2Type
        else -> localPreset3Type
    }
    val activeMethod = when (currentPresetSlot) {
        1 -> localPreset1Method
        2 -> localPreset2Method
        else -> localPreset3Method
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = trans("widget_settings", currentLanguage),
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF888888)
            )
            Text(
                text = "✕",
                fontSize = 22.sp,
                color = Color.White,
                fontWeight = FontWeight.Light,
                modifier = Modifier
                    .clickable { onDismiss() }
                    .padding(8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- SPLIT STATUS WIDGET FRIEND SELECTOR ---
        Text(
            text = trans("select_split_connection", currentLanguage).uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF666666),
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Option 1: Active Connection (Default)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (localSelectedConnectionId == "active") Color(0xFF161616) else Color.Transparent)
                .clickable { localSelectedConnectionId = "active" }
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = trans("active_connection", currentLanguage),
                    fontSize = 15.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Fallback to active connection in Split tab",
                    fontSize = 12.sp,
                    color = Color(0xFF888888)
                )
            }
            if (localSelectedConnectionId == "active") {
                Text(text = "✓", fontSize = 18.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        HorizontalDivider(color = Color(0xFF1E1E1E), modifier = Modifier.padding(vertical = 8.dp))

        // Custom connections list
        connections.forEach { conn ->
            val isSelected = localSelectedConnectionId == conn.id
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) Color(0xFF161616) else Color.Transparent)
                    .clickable { localSelectedConnectionId = conn.id }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = conn.name,
                        fontSize = 15.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = conn.email,
                        fontSize = 12.sp,
                        color = Color(0xFF888888)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val sign = if (conn.balance > 0) "+" else ""
                    val signColor = if (conn.balance > 0) Color(0xFF4CAF50) else if (conn.balance < 0) Color(0xFFF44336) else Color(0xFF888888)
                    Text(
                        text = "$sign$currencySymbol${formatIndian(conn.balance)}",
                        fontSize = 14.sp,
                        color = signColor,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    if (isSelected) {
                        Text(text = "✓", fontSize = 18.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
            HorizontalDivider(color = Color(0xFF1E1E1E), modifier = Modifier.padding(vertical = 4.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- QUICK PRESETS WIDGET SLOT SELECTOR ---
        Text(
            text = trans("configure_presets_slot", currentLanguage).uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF666666),
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Slot segmented pill selector
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(1, 2, 3).forEach { slotNum ->
                val active = currentPresetSlot == slotNum
                val boundName = when (slotNum) {
                    1 -> localPreset1Name
                    2 -> localPreset2Name
                    else -> localPreset3Name
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (active) Color.White else Color(0xFF161616))
                        .border(
                            width = 1.dp,
                            color = if (active) Color.White else Color(0xFF222222),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { currentPresetSlot = slotNum }
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Slot $slotNum",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (active) Color.Black else Color.White
                        )
                        Text(
                            text = boundName,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (active) Color(0xFF444444) else Color(0xFF888888),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }

        // --- CUSTOM SLOT EDITING FORM ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF161616))
                .padding(16.dp)
        ) {
            Text(
                text = "CUSTOMIZE SLOT $currentPresetSlot",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF888888),
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Preset Label Input
            Text(
                text = "PRESET LABEL",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF666666),
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            BasicTextField(
                value = activeName,
                onValueChange = { newVal ->
                    when (currentPresetSlot) {
                        1 -> localPreset1Name = newVal
                        2 -> localPreset2Name = newVal
                        3 -> localPreset3Name = newVal
                    }
                },
                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, color = Color.White),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .border(1.dp, Color(0xFF222222), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            )

            // Preset Amount Input
            Text(
                text = "AMOUNT (${currencySymbol})",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF666666),
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            BasicTextField(
                value = activeAmt,
                onValueChange = { newVal ->
                    if (newVal.all { it.isDigit() || it == '.' }) {
                        when (currentPresetSlot) {
                            1 -> localPreset1Amt = newVal
                            2 -> localPreset2Amt = newVal
                            3 -> localPreset3Amt = newVal
                        }
                    }
                },
                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, color = Color.White),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .border(1.dp, Color(0xFF222222), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Preset Type Segmented Control
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "TYPE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF666666),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .border(1.dp, Color(0xFF222222), RoundedCornerShape(18.dp))
                            .clip(RoundedCornerShape(18.dp)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("expense", "income").forEach { type ->
                            val isSelected = activeType == type
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(if (isSelected) Color.White else Color.Transparent)
                                    .clickable {
                                        when (currentPresetSlot) {
                                            1 -> localPreset1Type = type
                                            2 -> localPreset2Type = type
                                            3 -> localPreset3Type = type
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = type.uppercase(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.Black else Color.White
                                )
                            }
                        }
                    }
                }

                // Preset Method Segmented Control
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "METHOD",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF666666),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .border(1.dp, Color(0xFF222222), RoundedCornerShape(18.dp))
                            .clip(RoundedCornerShape(18.dp)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("online", "cash").forEach { method ->
                            val isSelected = activeMethod == method
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(if (isSelected) Color.White else Color.Transparent)
                                    .clickable {
                                        when (currentPresetSlot) {
                                            1 -> localPreset1Method = method
                                            2 -> localPreset2Method = method
                                            3 -> localPreset3Method = method
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = method.uppercase(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.Black else Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "TAP PRESET TO BIND TO SLOT $currentPresetSlot:",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF888888),
            modifier = Modifier.padding(bottom = 10.dp)
        )

        // Default Presets choices
        val systemDefaults = listOf(
            LogPreset("def_1", "expense", 20.0, "Tea & Chips", "cash"),
            LogPreset("def_2", "expense", 40.0, "Metro Ride", "online"),
            LogPreset("def_3", "expense", 150.0, "Office Lunch", "online")
        )

        val allAvailablePresets = systemDefaults + presets

        allAvailablePresets.forEach { preset ->
            val isCurrentBound = activeName == preset.description && activeAmt == preset.amount.toString()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isCurrentBound) Color(0xFF1E1E1E) else Color.Transparent)
                    .clickable {
                        val nameVal = preset.description
                        val amtVal = preset.amount.toString()
                        val typeVal = preset.type
                        val methodVal = preset.method
                        
                        when (currentPresetSlot) {
                            1 -> {
                                localPreset1Name = nameVal
                                localPreset1Amt = amtVal
                                localPreset1Type = typeVal
                                localPreset1Method = methodVal
                            }
                            2 -> {
                                localPreset2Name = nameVal
                                localPreset2Amt = amtVal
                                localPreset2Type = typeVal
                                localPreset2Method = methodVal
                            }
                            3 -> {
                                localPreset3Name = nameVal
                                localPreset3Amt = amtVal
                                localPreset3Type = typeVal
                                localPreset3Method = methodVal
                            }
                        }
                    }
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(if (preset.type == "income") Color(0xFF1B3D23) else Color(0xFF3D1C1C)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (preset.type == "income") "+" else "-",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (preset.type == "income") Color(0xFF4CAF50) else Color(0xFFF44336)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = preset.description,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${preset.method.uppercase()} • ${preset.type.uppercase()}",
                            fontSize = 11.sp,
                            color = Color(0xFF666666)
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$currencySymbol ${preset.amount.toLong()}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    if (isCurrentBound) {
                        Text(text = "✓", fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
            HorizontalDivider(color = Color(0xFF161616), modifier = Modifier.padding(vertical = 2.dp))
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Save Button
        Button(
            onClick = {
                // Persistent save
                saveNotificationPref("widget_selected_connection_id", localSelectedConnectionId)
                
                saveNotificationPref("widget_preset_1_name", localPreset1Name)
                saveNotificationPref("widget_preset_1_amount", localPreset1Amt)
                saveNotificationPref("widget_preset_1_type", localPreset1Type)
                saveNotificationPref("widget_preset_1_method", localPreset1Method)

                saveNotificationPref("widget_preset_2_name", localPreset2Name)
                saveNotificationPref("widget_preset_2_amount", localPreset2Amt)
                saveNotificationPref("widget_preset_2_type", localPreset2Type)
                saveNotificationPref("widget_preset_2_method", localPreset2Method)

                saveNotificationPref("widget_preset_3_name", localPreset3Name)
                saveNotificationPref("widget_preset_3_amount", localPreset3Amt)
                saveNotificationPref("widget_preset_3_type", localPreset3Type)
                saveNotificationPref("widget_preset_3_method", localPreset3Method)

                // Sync Split widget details immediately
                val activeConn = if (localSelectedConnectionId == "active" || localSelectedConnectionId.isEmpty()) {
                    if (connections.isNotEmpty()) connections.first() else null
                } else {
                    connections.find { it.id == localSelectedConnectionId }
                }

                if (activeConn != null) {
                    val status = if (activeConn.balance > 0) "${extractFirstName(activeConn.name)} owes you" else if (activeConn.balance < 0) "You owe ${extractFirstName(activeConn.name)}" else "No owes with ${extractFirstName(activeConn.name)}"
                    saveNotificationPref("widget_split_status", status)
                    saveNotificationPref("widget_split_amount", formatIndian(kotlin.math.abs(activeConn.balance)))
                } else {
                    saveNotificationPref("widget_split_status", "No Active Splits")
                    saveNotificationPref("widget_split_amount", "0.00")
                }

                // Broadcast updates to widgets
                updateHomeWidgets()

                triggerToast(trans("widget_settings_saved", currentLanguage))
                onDismiss()
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(
                text = trans("save_widget_settings", currentLanguage),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}
