package com.kartavya.penny

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

actual suspend fun makeHttpRequest(
    url: String,
    method: String,
    body: String?,
    headers: Map<String, String>
): String = withContext(Dispatchers.IO) {
    val connection = URL(url).openConnection() as HttpURLConnection
    connection.requestMethod = method
    connection.connectTimeout = 15000
    connection.readTimeout = 15000
    
    // Set headers
    headers.forEach { (key, value) ->
        connection.setRequestProperty(key, value)
    }
    
    if (body != null) {
        connection.doOutput = true
        OutputStreamWriter(connection.outputStream, "UTF-8").use { writer ->
            writer.write(body)
            writer.flush()
        }
    }
    
    val responseCode = connection.responseCode
    val stream = if (responseCode in 200..299) {
        connection.inputStream
    } else {
        connection.errorStream ?: connection.inputStream
    }
    
    val result = stream?.bufferedReader()?.use { it.readText() } ?: ""
    connection.disconnect()
    
    if (responseCode !in 200..299) {
        throw Exception("HTTP $responseCode: $result")
    }
    result
}
