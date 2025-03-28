package com.decondition.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import com.decondition.data.PreferencesManager

class DeconditionAccessibilityService : AccessibilityService() {
    private val TAG = "DeconditionAccessibilityService"
    private lateinit var preferencesManager: PreferencesManager

    override fun onCreate() {
        super.onCreate()
        preferencesManager = PreferencesManager(this)
        Log.d(TAG, "Accessibility Service created")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // We're only interested in window state changes, which indicate app switches
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            event.packageName?.toString()?.let { packageName ->
                Log.d(TAG, "Detected app launch: $packageName")

                if (preferencesManager.isAppMonitored(packageName)) {
                    // Update last accessed time
                    preferencesManager.updateAppLastAccessed(packageName)
                    Log.d(TAG, "Monitored app detected: $packageName")

                    // Get the current challenge type from preferences
                    val challengeType = preferencesManager.getSelectedChallengeType()

                    // Launch challenge activity
                    val intent = Intent(this, ChallengeActivity::class.java).apply {
                        putExtra(ChallengeActivity.EXTRA_PACKAGE_NAME, packageName)
                        putExtra(ChallengeActivity.EXTRA_TYPE, ChallengeActivity.TYPE_APP)
                        putExtra(ChallengeActivity.EXTRA_CHALLENGE_TYPE, challengeType.name)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    startActivity(intent)

                    // Show toast for debugging
                    Toast.makeText(
                        this,
                        "Challenge required for $packageName",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onInterrupt() {
        // Required but not used
        Log.d(TAG, "Service interrupted")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Service connected")

        // You could perform any initialization here
        Toast.makeText(
            this,
            "Decondition Accessibility Service Connected",
            Toast.LENGTH_SHORT
        ).show()
    }

    companion object {
        // Constants for ChallengeActivity
        class ChallengeActivity {
            companion object {
                const val EXTRA_PACKAGE_NAME = "package_name"
                const val EXTRA_DOMAIN = "domain"
                const val EXTRA_TYPE = "type"
                const val EXTRA_CHALLENGE_TYPE = "challenge_type"

                const val TYPE_APP = 1
                const val TYPE_DOMAIN = 2
            }
        }
    }
}