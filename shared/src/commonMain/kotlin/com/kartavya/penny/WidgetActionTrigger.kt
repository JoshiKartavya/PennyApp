package com.kartavya.penny

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object WidgetActionTrigger {
    var activeAction by mutableStateOf<String?>(null)
}
