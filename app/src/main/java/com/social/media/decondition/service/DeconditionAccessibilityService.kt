package com.decondition.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import com.decondition.data.PreferencesManager

class DeconditionAccessibilityService : AccessibilityService() {

    private lateinit var preferencesManager: PreferencesManager

    override fun onCreate() {
        super.onCreate()
        preferencesManager = PreferencesManager(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // We're only interested in window state changes, which indicate app switches
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            event.packageName?.toString()?.let { packageName ->
                if (preferencesManager.isAppMonitored(packageName)) {
                    // Update last accessed time
                    preferencesManager.updateAppLastAccessed(packageName)

                    // Launch challenge activity
                    val intent = Intent(this, ChallengeActivity::class.java).apply {
                        putExtra(ChallengeActivity.EXTRA_PACKAGE_NAME, packageName)
                        putExtra(ChallengeActivity.EXTRA_TYPE, ChallengeActivity.TYPE_APP)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    startActivity(intent)

                    // Show toast for debugging
                    Toast.makeText(
                        this,
                        "Monitored app detected: $packageName",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onInterrupt() {
        // Required but not used
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        // You could perform any initialization here
        Toast.makeText(
            this,
            "Decondition Accessibility Service Connected",
            Toast.LENGTH_SHORT
        ).show()
    }
}