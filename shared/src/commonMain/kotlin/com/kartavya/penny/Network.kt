package com.kartavya.penny

expect suspend fun makeHttpRequest(
    url: String,
    method: String,
    body: String? = null,
    headers: Map<String, String> = emptyMap()
): String
