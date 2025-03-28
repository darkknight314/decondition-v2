package com.decondition.service

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.widget.Toast
import com.decondition.data.PreferencesManager
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.InetAddress

class DeconditionVpnService : VpnService() {

    private lateinit var preferencesManager: PreferencesManager
    private var vpnInterface: ParcelFileDescriptor? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        preferencesManager = PreferencesManager(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Start the VPN connection
        startVpn()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopVpn()
    }

    private fun startVpn() {
        // This is a simplified implementation
        // In a real app, you would need to handle DNS resolution, packet forwarding, etc.
        try {
            // Configure the VPN
            val builder = Builder()
                .setSession("Decondition VPN")
                .addAddress("10.0.0.2", 32)
                .addRoute("0.0.0.0", 0)
                .addDnsServer("8.8.8.8")

            // Establish the connection
            vpnInterface = builder.establish()

            if (vpnInterface != null) {
                Toast.makeText(
                    this,
                    "VPN Service Started",
                    Toast.LENGTH_SHORT
                ).show()

                // Start processing packets
                serviceScope.launch {
                    processPackets()
                }
            } else {
                Toast.makeText(
                    this,
                    "Failed to establish VPN connection",
                    Toast.LENGTH_SHORT
                ).show()
            }

        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Error starting VPN: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun stopVpn() {
        try {
            vpnInterface?.close()
            vpnInterface = null
        } catch (e: Exception) {
            // Log error
        }
    }

    private fun processPackets() {
        // This is a highly simplified implementation
        // In a real app, you would need to properly parse DNS queries and other packets

        vpnInterface?.let { vpn ->
            val inputStream = FileInputStream(vpn.fileDescriptor)
            val outputStream = FileOutputStream(vpn.fileDescriptor)

            val packet = ByteBuffer.allocate(32767)

            try {
                while (true) {
                    // Read a packet
                    packet.clear()
                    val length = inputStream.read(packet.array())
                    if (length <= 0) {
                        break
                    }

                    // Process packet - this is where you would look for DNS queries
                    // and detect access to monitored domains
                    processDnsPacket(packet, length)

                    // Forward the packet (in a real implementation)
                    // outputStream.write(packet.array(), 0, length)
                }
            } catch (e: Exception) {
                // Handle exceptions
            }
        }
    }

    private fun processDnsPacket(packet: ByteBuffer, length: Int) {
        // This is a placeholder for DNS packet processing
        // In a real app, you would parse the DNS query to extract the domain

        // For demo purposes, let's pretend we detected a monitored domain
        val fakeDomain = "instagram.com" // Example domain

        if (preferencesManager.isDomainMonitored(fakeDomain)) {
            // Update last accessed time
            preferencesManager.updateDomainLastAccessed(fakeDomain)

            // Launch challenge activity
            serviceScope.launch(Dispatchers.Main) {
                val intent = Intent(this@DeconditionVpnService, ChallengeActivity::class.java).apply {
                    putExtra(ChallengeActivity.EXTRA_DOMAIN, fakeDomain)
                    putExtra(ChallengeActivity.EXTRA_TYPE, ChallengeActivity.TYPE_DOMAIN)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
            }
        }
    }
}