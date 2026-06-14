package com.kartavya.penny

import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.*
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

actual suspend fun makeHttpRequest(
    url: String,
    method: String,
    body: String?,
    headers: Map<String, String>
): String = suspendCancellableCoroutine { continuation ->
    val nsUrl = NSURL.URLWithString(url) ?: run {
        continuation.resumeWithException(IllegalArgumentException("Invalid URL: $url"))
        return@suspendCancellableCoroutine
    }
    
    val request = NSMutableURLRequest.requestWithURL(nsUrl)
    request.setHTTPMethod(method)
    
    headers.forEach { (key, value) ->
        request.setValue(value, forHTTPHeaderField = key)
    }
    
    if (body != null) {
        val nsData = NSString.create(string = body).dataUsingEncoding(NSUTF8StringEncoding)
        request.setHTTPBody(nsData)
    }
    
    val session = NSURLSession.sharedSession
    val task = session.dataTaskWithRequest(request) { data, response, error ->
        if (error != null) {
            continuation.resumeWithException(Exception(error.localizedDescription))
            return@dataTaskWithRequest
        }
        
        if (data != null) {
            val responseString = NSString.create(data = data, encoding = NSUTF8StringEncoding)?.toString() ?: ""
            val httpResponse = response as? NSHTTPURLResponse
            val statusCode = httpResponse?.statusCode ?: 200
            if (statusCode !in 200..299) {
                continuation.resumeWithException(Exception("HTTP $statusCode: $responseString"))
            } else {
                continuation.resume(responseString)
            }
        } else {
            continuation.resume("")
        }
    }
    
    task.resume()
    continuation.invokeOnCancellation {
        task.cancel()
    }
}
