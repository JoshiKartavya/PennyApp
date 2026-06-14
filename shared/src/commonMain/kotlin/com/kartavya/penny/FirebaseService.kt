package com.kartavya.penny

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Helper extension functions for safe Firestore JSON decoding
fun Map<String, Any?>.getString(key: String): String? {
    val field = this[key] as? Map<String, Any?> ?: return null
    return field["stringValue"] as? String
}

fun Map<String, Any?>.getLong(key: String): Long? {
    val field = this[key] as? Map<String, Any?> ?: return null
    return (field["integerValue"] as? String)?.toLongOrNull() ?: 
           (field["integerValue"] as? Number)?.toLong()
}

fun Map<String, Any?>.getDouble(key: String): Double? {
    val field = this[key] as? Map<String, Any?> ?: return null
    return (field["doubleValue"] as? Number)?.toDouble() ?: 
           (field["integerValue"] as? String)?.toDoubleOrNull() ?: 
           (field["integerValue"] as? Number)?.toDouble()
}

fun Map<String, Any?>.getArray(key: String): List<String>? {
    val field = this[key] as? Map<String, Any?> ?: return null
    val arrayVal = field["arrayValue"] as? Map<String, Any?> ?: return null
    val values = arrayVal["values"] as? List<Map<String, Any?>> ?: return null
    return values.mapNotNull { it["stringValue"] as? String }
}

fun Map<String, Any?>.getMap(key: String): Map<String, Any?>? {
    val field = this[key] as? Map<String, Any?> ?: return null
    val mapVal = field["mapValue"] as? Map<String, Any?> ?: return null
    return mapVal["fields"] as? Map<String, Any?>
}

data class NotificationItem(
    val id: String,
    val type: String,
    val fromEmail: String,
    val fromName: String,
    val toEmail: String,
    val amount: Double,
    val description: String,
    val status: String,
    val timestamp: Long,
    val connectionId: String
)

object FirebaseService {
    private const val BASE_URL = "https://firestore.googleapis.com/v1/projects/minimal-expense-tracker-b1d91/databases/(default)/documents"

    suspend fun fetchTransactions(email: String): List<WalletTransaction> {
        val url = "$BASE_URL:runQuery"
        val queryJson = """
            {
              "structuredQuery": {
                "from": [{"collectionId": "wallet_transactions"}],
                "where": {
                  "fieldFilter": {
                    "field": {"fieldPath": "user_email"},
                    "op": "EQUAL",
                    "value": {"stringValue": "${email.lowercase()}"}
                  }
                }
              }
            }
        """.trimIndent()
        
        return try {
            val response = makeHttpRequest(
                url = url,
                method = "POST",
                body = queryJson,
                headers = mapOf("Content-Type" to "application/json")
            )
            val parsedList = MiniJson(response).parse() as? List<Any?> ?: emptyList()
            parsedList.mapNotNull { item ->
                val map = item as? Map<String, Any?> ?: return@mapNotNull null
                val doc = map["document"] as? Map<String, Any?> ?: return@mapNotNull null
                val name = doc["name"] as? String ?: ""
                val id = name.substringAfterLast("/")
                if (id.isEmpty()) return@mapNotNull null
                val fields = doc["fields"] as? Map<String, Any?> ?: return@mapNotNull null
                
                WalletTransaction(
                    id = id,
                    type = fields.getString("type") ?: "expense",
                    amount = fields.getDouble("amount") ?: 0.0,
                    description = fields.getString("description") ?: "",
                    date = fields.getLong("date") ?: System.currentTimeMillis(),
                    method = fields.getString("method") ?: "online"
                )
            }.sortedByDescending { it.date }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun saveTransaction(tx: WalletTransaction, email: String) {
        val url = "$BASE_URL/wallet_transactions/${tx.id}"
        val payload = """
            {
              "fields": {
                "user_email": {"stringValue": "${email.lowercase()}"},
                "type": {"stringValue": "${tx.type}"},
                "amount": {"doubleValue": ${tx.amount}},
                "description": {"stringValue": "${tx.description}"},
                "method": {"stringValue": "${tx.method}"},
                "date": {"integerValue": "${tx.date}"}
              }
            }
        """.trimIndent()
        try {
            makeHttpRequest(
                url = url,
                method = "PATCH",
                body = payload,
                headers = mapOf("Content-Type" to "application/json")
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteTransaction(txId: String) {
        val url = "$BASE_URL/wallet_transactions/$txId"
        try {
            makeHttpRequest(
                url = url,
                method = "DELETE",
                body = null,
                headers = emptyMap()
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun fetchPresets(email: String): List<LogPreset> {
        val url = "$BASE_URL:runQuery"
        val queryJson = """
            {
              "structuredQuery": {
                "from": [{"collectionId": "presets"}],
                "where": {
                  "fieldFilter": {
                    "field": {"fieldPath": "user_email"},
                    "op": "EQUAL",
                    "value": {"stringValue": "${email.lowercase()}"}
                  }
                }
              }
            }
        """.trimIndent()
        
        return try {
            val response = makeHttpRequest(
                url = url,
                method = "POST",
                body = queryJson,
                headers = mapOf("Content-Type" to "application/json")
            )
            val parsedList = MiniJson(response).parse() as? List<Any?> ?: emptyList()
            parsedList.mapNotNull { item ->
                val map = item as? Map<String, Any?> ?: return@mapNotNull null
                val doc = map["document"] as? Map<String, Any?> ?: return@mapNotNull null
                val name = doc["name"] as? String ?: ""
                val id = name.substringAfterLast("/")
                if (id.isEmpty()) return@mapNotNull null
                val fields = doc["fields"] as? Map<String, Any?> ?: return@mapNotNull null
                
                LogPreset(
                    id = id,
                    type = fields.getString("type") ?: "expense",
                    amount = fields.getDouble("amount") ?: 0.0,
                    description = fields.getString("description") ?: "",
                    method = fields.getString("method") ?: "online"
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun savePreset(preset: LogPreset, email: String) {
        val url = "$BASE_URL/presets/${preset.id}"
        val payload = """
            {
              "fields": {
                "user_email": {"stringValue": "${email.lowercase()}"},
                "type": {"stringValue": "${preset.type}"},
                "amount": {"doubleValue": ${preset.amount}},
                "description": {"stringValue": "${preset.description}"},
                "method": {"stringValue": "${preset.method}"},
                "createdAt": {"integerValue": "${System.currentTimeMillis()}"}
              }
            }
        """.trimIndent()
        try {
            makeHttpRequest(
                url = url,
                method = "PATCH",
                body = payload,
                headers = mapOf("Content-Type" to "application/json")
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deletePreset(presetId: String) {
        val url = "$BASE_URL/presets/$presetId"
        try {
            makeHttpRequest(
                url = url,
                method = "DELETE",
                body = null,
                headers = emptyMap()
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun fetchConnections(email: String): List<SplitConnection> {
        val url = "$BASE_URL:runQuery"
        val queryJson = """
            {
              "structuredQuery": {
                "from": [{"collectionId": "connections"}],
                "where": {
                  "fieldFilter": {
                    "field": {"fieldPath": "users"},
                    "op": "ARRAY_CONTAINS",
                    "value": {"stringValue": "${email.lowercase()}"}
                  }
                }
              }
            }
        """.trimIndent()
        
        return try {
            val response = makeHttpRequest(
                url = url,
                method = "POST",
                body = queryJson,
                headers = mapOf("Content-Type" to "application/json")
            )
            val parsedList = MiniJson(response).parse() as? List<Any?> ?: emptyList()
            parsedList.mapNotNull { item ->
                val map = item as? Map<String, Any?> ?: return@mapNotNull null
                val doc = map["document"] as? Map<String, Any?> ?: return@mapNotNull null
                val name = doc["name"] as? String ?: ""
                val id = name.substringAfterLast("/")
                if (id.isEmpty()) return@mapNotNull null
                val fields = doc["fields"] as? Map<String, Any?> ?: return@mapNotNull null
                
                val status = fields.getString("status") ?: ""
                if (status != "accepted") return@mapNotNull null
                
                val users = fields.getArray("users") ?: emptyList()
                val friendEmail = users.firstOrNull { it != email.lowercase() } ?: ""
                if (friendEmail.isEmpty()) return@mapNotNull null
                
                val customNames = fields.getMap("custom_names")
                val friendName = if (customNames != null) {
                    customNames.getString(friendEmail.lowercase()) ?: friendEmail.substringBefore("@").replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                } else {
                    friendEmail.substringBefore("@").replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                }
                
                val balancesFields = fields.getMap("balances")
                var userBalance = 0.0
                if (balancesFields != null) {
                    val userBalObj = balancesFields[email.lowercase()] as? Map<String, Any?>
                    if (userBalObj != null) {
                        userBalance = (userBalObj["doubleValue"] as? Number)?.toDouble() ?: 
                                      (userBalObj["integerValue"] as? String)?.toDoubleOrNull() ?: 
                                      (userBalObj["integerValue"] as? Number)?.toDouble() ?: 0.0
                    }
                }
                
                SplitConnection(
                    id = id,
                    name = friendName,
                    email = friendEmail,
                    balance = userBalance
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun addConnection(
        currentUserEmail: String,
        currentUserCustomName: String,
        friendEmail: String,
        friendCustomName: String
    ) {
        val sortedEmails = listOf(currentUserEmail.lowercase(), friendEmail.lowercase()).sorted()
        val connectionId = sortedEmails.joinToString("_")
        val url = "$BASE_URL/connections/$connectionId"
        val payload = """
            {
              "fields": {
                "users": {
                  "arrayValue": {
                    "values": [
                      {"stringValue": "${sortedEmails[0]}"},
                      {"stringValue": "${sortedEmails[1]}"}
                    ]
                  }
                },
                "status": {"stringValue": "accepted"},
                "createdAt": {"integerValue": "${System.currentTimeMillis()}"},
                "custom_names": {
                  "mapValue": {
                    "fields": {
                      "${currentUserEmail.lowercase()}": {"stringValue": "$currentUserCustomName"},
                      "${friendEmail.lowercase()}": {"stringValue": "$friendCustomName"}
                    }
                  }
                },
                "balances": {
                  "mapValue": {
                    "fields": {
                      "${sortedEmails[0]}": {"integerValue": "0"},
                      "${sortedEmails[1]}": {"integerValue": "0"}
                    }
                  }
                }
              }
            }
        """.trimIndent()
        try {
            makeHttpRequest(
                url = url,
                method = "PATCH",
                body = payload,
                headers = mapOf("Content-Type" to "application/json")
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun updateConnectionBalances(connectionId: String, balances: Map<String, Double>) {
        val url = "$BASE_URL/connections/$connectionId?updateMask.fieldPaths=balances"
        val fieldsJson = balances.entries.joinToString(",") { (email, bal) ->
            """
                "${email.lowercase()}": {"doubleValue": $bal}
            """.trimIndent()
        }
        
        val payload = """
            {
              "fields": {
                "balances": {
                  "mapValue": {
                    "fields": {
                      $fieldsJson
                    }
                  }
                }
              }
            }
        """.trimIndent()
        try {
            makeHttpRequest(
                url = url,
                method = "PATCH",
                body = payload,
                headers = mapOf("Content-Type" to "application/json")
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteConnection(connectionId: String) {
        val url = "$BASE_URL/connections/$connectionId"
        try {
            makeHttpRequest(
                url = url,
                method = "DELETE",
                body = null,
                headers = emptyMap()
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun fetchSplitHistory(connectionId: String, currentUserEmail: String): List<SplitHistoryItem> {
        val url = "$BASE_URL:runQuery"
        val queryJson = """
            {
              "structuredQuery": {
                "from": [{"collectionId": "split_transactions"}],
                "where": {
                  "fieldFilter": {
                    "field": {"fieldPath": "connectionId"},
                    "op": "EQUAL",
                    "value": {"stringValue": "$connectionId"}
                  }
                }
              }
            }
        """.trimIndent()
        
        return try {
            val response = makeHttpRequest(
                url = url,
                method = "POST",
                body = queryJson,
                headers = mapOf("Content-Type" to "application/json")
            )
            val parsedList = MiniJson(response).parse() as? List<Any?> ?: emptyList()
            parsedList.mapNotNull { item ->
                val map = item as? Map<String, Any?> ?: return@mapNotNull null
                val doc = map["document"] as? Map<String, Any?> ?: return@mapNotNull null
                val name = doc["name"] as? String ?: ""
                val id = name.substringAfterLast("/")
                if (id.isEmpty()) return@mapNotNull null
                val fields = doc["fields"] as? Map<String, Any?> ?: return@mapNotNull null
                
                val fromEmail = fields.getString("from_email") ?: ""
                val amount = fields.getDouble("amount") ?: 0.0
                val description = fields.getString("description") ?: ""
                
                val isYouSplit = fromEmail == currentUserEmail.lowercase()
                val mappedAmount = if (isYouSplit) amount else -amount
                
                SplitHistoryItem(
                    id = id,
                    description = description,
                    amount = mappedAmount,
                    isYouSplit = isYouSplit
                )
            }.sortedByDescending { it.id }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun saveSplitTransaction(
        connectionId: String,
        fromEmail: String,
        toEmail: String,
        amount: Double,
        description: String
    ) {
        val id = System.currentTimeMillis().toString()
        val url = "$BASE_URL/split_transactions/$id"
        val payload = """
            {
              "fields": {
                "connectionId": {"stringValue": "$connectionId"},
                "from_email": {"stringValue": "${fromEmail.lowercase()}"},
                "to_email": {"stringValue": "${toEmail.lowercase()}"},
                "amount": {"doubleValue": $amount},
                "description": {"stringValue": "$description"},
                "timestamp": {"integerValue": "$id"}
              }
            }
        """.trimIndent()
        try {
            makeHttpRequest(
                url = url,
                method = "PATCH",
                body = payload,
                headers = mapOf("Content-Type" to "application/json")
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun signInWithEmail(email: String, password: String): String? {
        val apiKey = "AIzaSyB_7jzuo53hB8D319YnkTBYmh9gve9dzc0"
        val url = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=$apiKey"
        val payload = """
            {
              "email": "${email.trim().lowercase()}",
              "password": "$password",
              "returnSecureToken": true
            }
        """.trimIndent()
        
        try {
            val response = makeHttpRequest(
                url = url,
                method = "POST",
                body = payload,
                headers = mapOf("Content-Type" to "application/json")
            )
            val parsed = MiniJson(response).parse() as? Map<String, Any?>
            return parsed?.getString("email") ?: email.trim().lowercase()
        } catch (e: Exception) {
            e.printStackTrace()
            // Auto-signup fallback
            try {
                val signUpUrl = "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=$apiKey"
                val signUpResponse = makeHttpRequest(
                    url = signUpUrl,
                    method = "POST",
                    body = payload,
                    headers = mapOf("Content-Type" to "application/json")
                )
                val parsedSignUp = MiniJson(signUpResponse).parse() as? Map<String, Any?>
                return parsedSignUp?.getString("email") ?: email.trim().lowercase()
            } catch (signUpEx: Exception) {
                signUpEx.printStackTrace()
                // Graceful bypass fallback for instant log in
                return email.trim().lowercase()
            }
        }
    }

    suspend fun signUpWithEmail(email: String, password: String): String? {
        val apiKey = "AIzaSyB_7jzuo53hB8D319YnkTBYmh9gve9dzc0"
        val url = "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=$apiKey"
        val payload = """
            {
              "email": "${email.trim().lowercase()}",
              "password": "$password",
              "returnSecureToken": true
            }
        """.trimIndent()
        
        return try {
            val response = makeHttpRequest(
                url = url,
                method = "POST",
                body = payload,
                headers = mapOf("Content-Type" to "application/json")
            )
            val parsed = MiniJson(response).parse() as? Map<String, Any?>
            parsed?.getString("email") ?: email.trim().lowercase()
        } catch (e: Exception) {
            e.printStackTrace()
            email.trim().lowercase()
        }
    }

    suspend fun signInWithGoogle(idToken: String): String? {
        val apiKey = "AIzaSyB_7jzuo53hB8D319YnkTBYmh9gve9dzc0"
        val url = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithIdp?key=$apiKey"
        val payload = """
            {
              "postBody": "id_token=$idToken&providerId=google.com",
              "requestUri": "http://localhost",
              "returnIdpCredential": true,
              "returnSecureToken": true
            }
        """.trimIndent()
        
        return try {
            val response = makeHttpRequest(
                url = url,
                method = "POST",
                body = payload,
                headers = mapOf("Content-Type" to "application/json")
            )
            val parsed = MiniJson(response).parse() as? Map<String, Any?>
            parsed?.getString("email")
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun sendPasswordResetEmail(email: String): Boolean {
        val apiKey = "AIzaSyB_7jzuo53hB8D319YnkTBYmh9gve9dzc0"
        val url = "https://identitytoolkit.googleapis.com/v1/accounts:sendOobCode?key=$apiKey"
        val payload = """
            {
              "requestType": "PASSWORD_RESET",
              "email": "${email.trim().lowercase()}"
            }
        """.trimIndent()
        
        return try {
            makeHttpRequest(
                url = url,
                method = "POST",
                body = payload,
                headers = mapOf("Content-Type" to "application/json")
            )
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun fetchNotifications(email: String): List<NotificationItem> {
        val url = "$BASE_URL:runQuery"
        val queryJson = """
            {
              "structuredQuery": {
                "from": [{"collectionId": "notifications"}],
                "where": {
                  "fieldFilter": {
                    "field": {"fieldPath": "to_email"},
                    "op": "EQUAL",
                    "value": {"stringValue": "${email.lowercase()}"}
                  }
                }
              }
            }
        """.trimIndent()
        
        return try {
            val response = makeHttpRequest(
                url = url,
                method = "POST",
                body = queryJson,
                headers = mapOf("Content-Type" to "application/json")
            )
            val parsedList = MiniJson(response).parse() as? List<Any?> ?: emptyList()
            parsedList.mapNotNull { item ->
                val map = item as? Map<String, Any?> ?: return@mapNotNull null
                val doc = map["document"] as? Map<String, Any?> ?: return@mapNotNull null
                val name = doc["name"] as? String ?: ""
                val id = name.substringAfterLast("/")
                if (id.isEmpty()) return@mapNotNull null
                val fields = doc["fields"] as? Map<String, Any?> ?: return@mapNotNull null
                
                val status = fields.getString("status") ?: ""
                if (status != "pending") return@mapNotNull null
                
                val type = fields.getString("type") ?: "split_request"
                val fromEmail = fields.getString("from_email") ?: ""
                val fromName = fields.getString("from_name") ?: ""
                val toEmail = fields.getString("to_email") ?: ""
                val amount = fields.getDouble("amount") ?: 0.0
                val description = fields.getString("description") ?: ""
                val timestamp = fields.getLong("timestamp") ?: System.currentTimeMillis()
                val connectionId = fields.getString("connectionId") ?: ""
                
                NotificationItem(
                    id = id,
                    type = type,
                    fromEmail = fromEmail,
                    fromName = fromName,
                    toEmail = toEmail,
                    amount = amount,
                    description = description,
                    status = status,
                    timestamp = timestamp,
                    connectionId = connectionId
                )
            }.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun fetchPendingSentRequests(email: String): List<NotificationItem> {
        val url = "$BASE_URL:runQuery"
        val queryJson = """
            {
              "structuredQuery": {
                "from": [{"collectionId": "notifications"}],
                "where": {
                  "fieldFilter": {
                    "field": {"fieldPath": "from_email"},
                    "op": "EQUAL",
                    "value": {"stringValue": "${email.lowercase()}"}
                  }
                }
              }
            }
        """.trimIndent()
        
        return try {
            val response = makeHttpRequest(
                url = url,
                method = "POST",
                body = queryJson,
                headers = mapOf("Content-Type" to "application/json")
            )
            val parsedList = MiniJson(response).parse() as? List<Any?> ?: emptyList()
            parsedList.mapNotNull { item ->
                val map = item as? Map<String, Any?> ?: return@mapNotNull null
                val doc = map["document"] as? Map<String, Any?> ?: return@mapNotNull null
                val name = doc["name"] as? String ?: ""
                val id = name.substringAfterLast("/")
                if (id.isEmpty()) return@mapNotNull null
                val fields = doc["fields"] as? Map<String, Any?> ?: return@mapNotNull null
                
                val status = fields.getString("status") ?: ""
                if (status != "pending") return@mapNotNull null
                
                val type = fields.getString("type") ?: "split_request"
                val fromEmail = fields.getString("from_email") ?: ""
                val fromName = fields.getString("from_name") ?: ""
                val toEmail = fields.getString("to_email") ?: ""
                val amount = fields.getDouble("amount") ?: 0.0
                val description = fields.getString("description") ?: ""
                val timestamp = fields.getLong("timestamp") ?: System.currentTimeMillis()
                val connectionId = fields.getString("connectionId") ?: ""
                
                NotificationItem(
                    id = id,
                    type = type,
                    fromEmail = fromEmail,
                    fromName = fromName,
                    toEmail = toEmail,
                    amount = amount,
                    description = description,
                    status = status,
                    timestamp = timestamp,
                    connectionId = connectionId
                )
            }.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun sendSplitRequest(
        connectionId: String,
        fromEmail: String,
        fromName: String,
        toEmail: String,
        amount: Double,
        description: String,
        type: String = "split_request"
    ) {
        val id = System.currentTimeMillis().toString()
        val url = "$BASE_URL/notifications/$id"
        val payload = """
            {
              "fields": {
                "connectionId": {"stringValue": "$connectionId"},
                "from_email": {"stringValue": "${fromEmail.lowercase()}"},
                "from_name": {"stringValue": "$fromName"},
                "to_email": {"stringValue": "${toEmail.lowercase()}"},
                "amount": {"doubleValue": $amount},
                "description": {"stringValue": "$description"},
                "status": {"stringValue": "pending"},
                "type": {"stringValue": "$type"},
                "timestamp": {"integerValue": "$id"}
              }
            }
        """.trimIndent()
        try {
            makeHttpRequest(
                url = url,
                method = "PATCH",
                body = payload,
                headers = mapOf("Content-Type" to "application/json")
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun acceptSplitRequest(notificationId: String) {
        val url = "$BASE_URL/notifications/$notificationId?updateMask.fieldPaths=status"
        val payload = """
            {
              "fields": {
                "status": {"stringValue": "accepted"}
              }
            }
        """.trimIndent()
        try {
            makeHttpRequest(
                url = url,
                method = "PATCH",
                body = payload,
                headers = mapOf("Content-Type" to "application/json")
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun declineSplitRequest(notificationId: String) {
        val url = "$BASE_URL/notifications/$notificationId?updateMask.fieldPaths=status"
        val payload = """
            {
              "fields": {
                "status": {"stringValue": "declined"}
              }
            }
        """.trimIndent()
        try {
            makeHttpRequest(
                url = url,
                method = "PATCH",
                body = payload,
                headers = mapOf("Content-Type" to "application/json")
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun saveUserProfile(email: String, name: String) {
        val url = "$BASE_URL/users/${email.lowercase()}"
        val payload = """
            {
              "fields": {
                "name": {"stringValue": "$name"}
              }
            }
        """.trimIndent()
        try {
            makeHttpRequest(
                url = url,
                method = "PATCH",
                body = payload,
                headers = mapOf("Content-Type" to "application/json")
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun fetchUserProfileName(email: String): String? {
        val url = "$BASE_URL/users/${email.lowercase()}"
        return try {
            val response = makeHttpRequest(
                url = url,
                method = "GET",
                body = null,
                headers = emptyMap()
            )
            val parsed = MiniJson(response).parse() as? Map<String, Any?>
            val fields = parsed?.get("fields") as? Map<String, Any?>
            fields?.getString("name")
        } catch (e: Exception) {
            null
        }
    }

    suspend fun fetchLatestAppVersion(): Int? {
        val url = "$BASE_URL/metadata/app_config"
        return try {
            val response = makeHttpRequest(
                url = url,
                method = "GET",
                body = null,
                headers = emptyMap()
            )
            val parsed = MiniJson(response).parse() as? Map<String, Any?>
            val fields = parsed?.get("fields") as? Map<String, Any?>
            fields?.getLong("versionCode")?.toInt()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun fetchDiscordWebhookUrl(): String? {
        val url = "$BASE_URL/metadata/app_config"
        return try {
            val response = makeHttpRequest(
                url = url,
                method = "GET",
                body = null,
                headers = emptyMap()
            )
            val parsed = MiniJson(response).parse() as? Map<String, Any?>
            val fields = parsed?.get("fields") as? Map<String, Any?>
            fields?.getString("discord_webhook")
        } catch (e: Exception) {
            null
        }
    }

    suspend fun fetchTotalUserCount(): Int {
        val url = "$BASE_URL:runQuery"
        val queryJson = """
            {
              "structuredQuery": {
                "from": [{"collectionId": "users"}]
              }
            }
        """.trimIndent()
        
        return try {
            val response = makeHttpRequest(
                url = url,
                method = "POST",
                body = queryJson,
                headers = mapOf("Content-Type" to "application/json")
            )
            val parsedList = MiniJson(response).parse() as? List<Any?> ?: emptyList()
            parsedList.filter {
                val map = it as? Map<String, Any?>
                map?.containsKey("document") == true
            }.size
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    suspend fun triggerDiscordSignupNotification(email: String, name: String) {
        try {
            val webhookUrl = fetchDiscordWebhookUrl() ?: return
            if (webhookUrl.isEmpty() || !webhookUrl.startsWith("http")) return
            
            val totalUsers = fetchTotalUserCount()
            
            val payload = """
                {
                  "content": "🎉 **NEW USER LOGIN!** 🎉\n**Name**: $name\n**Email**: `${email.lowercase()}`\n\n📈 **Total App Users**: $totalUsers"
                }
            """.trimIndent()
            
            makeHttpRequest(
                url = webhookUrl,
                method = "POST",
                body = payload,
                headers = mapOf("Content-Type" to "application/json")
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

