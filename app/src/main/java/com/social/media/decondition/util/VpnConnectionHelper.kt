package com.social.media.decondition.util

import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import com.decondition.data.PreferencesManager
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Helper class to manage VPN connection and DNS monitoring.
 * This class handles the low-level VPN operations and DNS packet parsing.
 */
class VpnConnectionHelper(
    private val vpnService: VpnService,
    private val preferencesManager: PreferencesManager
) {
    private val TAG = "VpnConnectionHelper"

    // VPN connection components
    private var vpnInterface: ParcelFileDescriptor? = null
    private var executorService: ExecutorService? = null
    private val activeQueryIds = ConcurrentHashMap<Short, Long>()

    // DNS packet constants
    private val DNS_HEADER_SIZE = 12
    private val UDP_HEADER_SIZE = 8
    private val IPV4_HEADER_SIZE = 20

    companion object {
        private const val VPN_ADDRESS = "10.0.0.2"
        private const val VPN_ROUTE = "0.0.0.0"
        private const val VPN_DNS = "8.8.8.8" // Google DNS
    }

    /**
     * Start the VPN connection.
     */
    fun startVpn(configureIntent: android.app.PendingIntent) {
        // Configure the VPN using the VpnService instance
        val builder = vpnService.Builder()
            .addAddress(VPN_ADDRESS, 24)
            .addRoute(VPN_ROUTE, 0)
            .addDnsServer(VPN_DNS)
            .setSession("TrafficMonitorVPN")
            .setConfigureIntent(configureIntent)

        try {
            vpnInterface = builder.establish()

            // Start processing data
            executorService = Executors.newFixedThreadPool(2)
            executorService?.submit { processOutgoingPackets() }
            executorService?.submit { processIncomingPackets() }

            Log.d(TAG, "VPN interface established successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error establishing VPN interface: ${e.message}")
            stopVpn()
        }
    }

    /**
     * Stop the VPN connection.
     */
    fun stopVpn() {
        Log.d(TAG, "Stopping VPN connection")
        executorService?.shutdownNow()
        vpnInterface?.close()
        vpnInterface = null
    }

    /**
     * Process outgoing packets to detect DNS queries for monitored domains.
     */
    private fun processOutgoingPackets() {
        if (vpnInterface == null) return

        try {
            val buffer = ByteBuffer.allocate(32767)
            val input = FileInputStream(vpnInterface?.fileDescriptor)

            while (!Thread.interrupted()) {
                // Read packet from VPN interface
                buffer.clear()
                val length = input.read(buffer.array())
                if (length < 1) continue

                buffer.limit(length)

                // Check if it's an IPv4 packet
                val version = buffer.get(0).toInt() shr 4
                if (version != 4) continue

                // Check if it's UDP
                val protocol = buffer.get(9).toInt()
                if (protocol != 17) continue // 17 = UDP

                // Extract source and destination ports
                val sourcePort = ((buffer.get(IPV4_HEADER_SIZE).toInt() and 0xFF) shl 8) or
                        (buffer.get(IPV4_HEADER_SIZE + 1).toInt() and 0xFF)
                val destPort = ((buffer.get(IPV4_HEADER_SIZE + 2).toInt() and 0xFF) shl 8) or
                        (buffer.get(IPV4_HEADER_SIZE + 3).toInt() and 0xFF)

                // Check if it's a DNS query (destination port 53)
                if (destPort == 53) {
                    // Extract DNS query ID
                    val queryId = ((buffer.get(IPV4_HEADER_SIZE + UDP_HEADER_SIZE).toInt() and 0xFF) shl 8) or
                            (buffer.get(IPV4_HEADER_SIZE + UDP_HEADER_SIZE + 1).toInt() and 0xFF)

                    // Store query ID and timestamp
                    activeQueryIds[queryId.toShort()] = System.currentTimeMillis()

                    // Extract domain from DNS query
                    val domain = extractDomainFromDnsQuery(buffer, IPV4_HEADER_SIZE + UDP_HEADER_SIZE)

                    if (domain != null) {
                        Log.d(TAG, "DNS query for domain: $domain")

                        // Use AppPreferencesManager to handle the domain access
                        preferencesManager.handleDomainAccess(domain)
                    }
                }

                // Forward the packet
                forwardPacket(buffer, length)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing outgoing packets: ${e.message}")
        }
    }

    /**
     * Process incoming packets.
     */
    private fun processIncomingPackets() {
        var vpnOutput: FileOutputStream? = null
        try {
            val channel = DatagramChannel.open()
            channel.configureBlocking(false)
            vpnService.protect(channel.socket())

            val buffer = ByteBuffer.allocate(32767)
            vpnOutput = FileOutputStream(vpnInterface?.fileDescriptor)

            while (!Thread.interrupted()) {
                buffer.clear()
                val socketAddress = channel.receive(buffer)

                if (socketAddress != null) {
                    buffer.flip()
                    vpnOutput.write(buffer.array(), 0, buffer.limit())
                }

                // Process activeQueryIds removal (clean up older than 10 seconds)
                val currentTime = System.currentTimeMillis()
                synchronized(activeQueryIds) {
                    val iterator = activeQueryIds.entries.iterator()
                    while (iterator.hasNext()) {
                        val entry = iterator.next()
                        if (currentTime - entry.value > 10000) {
                            iterator.remove()
                        }
                    }
                }

                Thread.sleep(10)  // Prevent CPU hogging
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing incoming packets: ${e.message}")
        } finally {
            try {
                vpnOutput?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Error closing vpnOutput: ${e.message}")
            }
        }
    }

    /**
     * Forward a packet to its intended destination.
     */
    private fun forwardPacket(buffer: ByteBuffer, length: Int) {
        var channel: DatagramChannel? = null
        try {
            // Extract destination address from IP header
            val destAddr = StringBuilder()
            for (i in 16..19) {
                destAddr.append(buffer.get(i).toInt() and 0xFF)
                if (i < 19) destAddr.append(".")
            }

            // Extract destination port from UDP header
            val destPort = ((buffer.get(IPV4_HEADER_SIZE + 2).toInt() and 0xFF) shl 8) or
                    (buffer.get(IPV4_HEADER_SIZE + 3).toInt() and 0xFF)

            // Create a new buffer for the UDP payload only
            val udpPayloadSize = length - IPV4_HEADER_SIZE - UDP_HEADER_SIZE
            val payloadBuffer = ByteBuffer.allocate(udpPayloadSize)

            // Copy only the UDP payload data
            buffer.position(IPV4_HEADER_SIZE + UDP_HEADER_SIZE)
            buffer.limit(length)
            payloadBuffer.put(buffer)
            payloadBuffer.flip()

            // Create and configure the channel
            channel = DatagramChannel.open()

            // CRITICAL: Protect the socket BEFORE any connect operations
            if (!vpnService.protect(channel.socket())) {
                Log.e(TAG, "Failed to protect the DatagramChannel socket")
                throw IOException("Failed to protect the socket")
            }

            // Connect to destination
            val socketAddress = InetSocketAddress(destAddr.toString(), destPort)
            channel.connect(socketAddress)

            // Send data
            channel.write(payloadBuffer)

            Log.d(TAG, "Successfully forwarded packet to $destAddr:$destPort")
        } catch (e: Exception) {
            Log.e(TAG, "Error forwarding packet: ${e.message}", e)
        } finally {
            try {
                channel?.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing channel: ${e.message}")
            }
        }
    }

    /**
     * Extract domain name from a DNS query.
     */
    private fun extractDomainFromDnsQuery(buffer: ByteBuffer, dnsStart: Int): String? {
        try {
            // Skip DNS header (12 bytes)
            var position = dnsStart + DNS_HEADER_SIZE

            // Skip question name
            while (position < buffer.limit()) {
                val labelLength = buffer.get(position).toInt() and 0xFF
                if (labelLength == 0) break
                position += labelLength + 1
            }

            // Skip question type and class
            position += 5

            // Build domain name from the question section
            val domainBuilder = StringBuilder()
            position = dnsStart + DNS_HEADER_SIZE

            while (position < buffer.limit()) {
                val labelLength = buffer.get(position++).toInt() and 0xFF
                if (labelLength == 0) break

                // Process each label
                for (i in 0 until labelLength) {
                    if (position >= buffer.limit()) return null
                    val b = buffer.get(position++).toInt() and 0xFF
                    domainBuilder.append(b.toChar())
                }
                domainBuilder.append('.')
            }

            // Remove the trailing dot
            if (domainBuilder.isNotEmpty() && domainBuilder.last() == '.') {
                domainBuilder.setLength(domainBuilder.length - 1)
            }

            return domainBuilder.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting domain: ${e.message}")
            return null
        }
    }
}