package com.social.media.decondition.data.model

data class MonitoredDomain(
    val domain: String,
    val lastAccessed: Long = 0
)