package com.decondition.service

import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import android.widget.Toast
import com.decondition.data.PreferencesManager
import com.social.media.decondition.util.VpnConnectionHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DeconditionVpnService : VpnService() {
    private val TAG = "DeconditionVpnService"
    private lateinit var preferencesManager: PreferencesManager
    private var vpnInterface: ParcelFileDescriptor? = null
    private var vpnConnectionHelper: VpnConnectionHelper? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private var isRunning = false

    override fun onCreate() {
        super.onCreate()
        preferencesManager = PreferencesManager(this)
        Log.d(TAG, "VPN Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "VPN Service received start command")

        // Check if it's a stop request
        if (intent?.action == ACTION_STOP) {
            stopVpn()
            stopSelf()
            return START_NOT_STICKY
        }

        // Start the VPN connection if not already running
        if (!isRunning) {
            startVpn()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopVpn()
        Log.d(TAG, "VPN Service destroyed")
    }

    private fun startVpn() {
        try {
            // Create a pending intent for the system VPN confirmation dialog
            val configureIntent = PendingIntent.getActivity(
                this, 0,
                Intent(this, Class.forName("com.social.media.decondition.MainActivity")),
                PendingIntent.FLAG_IMMUTABLE
            )

            // Initialize the VPN connection helper
            vpnConnectionHelper = VpnConnectionHelper(this, preferencesManager)
            vpnConnectionHelper?.startVpn(configureIntent)

            isRunning = true

            Toast.makeText(this, "VPN Service Started", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "VPN started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting VPN: ${e.message}")
            Toast.makeText(this, "Failed to start VPN: ${e.message}", Toast.LENGTH_SHORT).show()
            stopSelf()
        }
    }

    private fun stopVpn() {
        try {
            vpnConnectionHelper?.stopVpn()
            vpnConnectionHelper = null
            isRunning = false
            Log.d(TAG, "VPN stopped")
            Toast.makeText(this, "VPN Service Stopped", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping VPN: ${e.message}")
        }
    }

    // This method will be called by VpnConnectionHelper when a monitored domain is accessed
    fun handleMonitoredDomainAccess(domain: String) {
        // Update last accessed time
        preferencesManager.updateDomainLastAccessed(domain)
        Log.d(TAG, "Detected access to monitored domain: $domain")

        // Get the current challenge type from preferences
        val challengeType = preferencesManager.getSelectedChallengeType()

        // Launch challenge activity on the main thread
        serviceScope.launch {
            withContext(Dispatchers.Main) {
                val intent = Intent(this@DeconditionVpnService, ChallengeActivity::class.java).apply {
                    putExtra(ChallengeActivity.EXTRA_DOMAIN, domain)
                    putExtra(ChallengeActivity.EXTRA_TYPE, ChallengeActivity.TYPE_DOMAIN)
                    putExtra(ChallengeActivity.EXTRA_CHALLENGE_TYPE, challengeType.name)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)

                Toast.makeText(
                    this@DeconditionVpnService,
                    "Challenge required for domain: $domain",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    companion object {
        const val ACTION_START = "com.social.media.decondition.START_VPN"
        const val ACTION_STOP = "com.social.media.decondition.STOP_VPN"

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