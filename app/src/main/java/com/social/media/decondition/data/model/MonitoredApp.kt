package com.social.media.decondition.data.model

data class MonitoredApp(
    val packageName: String,
    val appName: String,
    val lastAccessed: Long = 0
)