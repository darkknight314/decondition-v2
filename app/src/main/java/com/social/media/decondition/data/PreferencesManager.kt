package com.decondition.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.social.media.decondition.data.ChallengeType

class PreferencesManager(context: Context) {
    private val TAG = "PreferencesManager"
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Challenge Type
    fun setSelectedChallengeType(type: ChallengeType) {
        prefs.edit().putString(KEY_CHALLENGE_TYPE, type.name).apply()
        Log.d(TAG, "Challenge type set to: ${type.name}")
    }

    fun getSelectedChallengeType(): ChallengeType {
        val typeName = prefs.getString(KEY_CHALLENGE_TYPE, ChallengeType.SUDOKU.name)
        return try {
            ChallengeType.valueOf(typeName ?: ChallengeType.SUDOKU.name)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Invalid challenge type: $typeName, defaulting to SUDOKU")
            ChallengeType.SUDOKU
        }
    }

    // Monitored Apps
    fun getMonitoredApps(): Set<String> {
        return prefs.getStringSet(KEY_MONITORED_APPS, emptySet()) ?: emptySet()
    }

    fun addMonitoredApp(packageName: String) {
        val apps = getMonitoredApps().toMutableSet()
        apps.add(packageName)
        prefs.edit().putStringSet(KEY_MONITORED_APPS, apps).apply()
        Log.d(TAG, "Added monitored app: $packageName")
    }

    fun removeMonitoredApp(packageName: String) {
        val apps = getMonitoredApps().toMutableSet()
        apps.remove(packageName)
        prefs.edit().putStringSet(KEY_MONITORED_APPS, apps).apply()
        Log.d(TAG, "Removed monitored app: $packageName")
    }

    fun isAppMonitored(packageName: String): Boolean {
        return getMonitoredApps().contains(packageName)
    }

    // App Names (mapping package names to app names)
    fun saveAppName(packageName: String, appName: String) {
        prefs.edit().putString("$KEY_APP_NAME_PREFIX$packageName", appName).apply()
    }

    fun getAppName(packageName: String): String? {
        return prefs.getString("$KEY_APP_NAME_PREFIX$packageName", null)
    }

    // App Last Accessed Times
    fun updateAppLastAccessed(packageName: String) {
        prefs.edit().putLong("$KEY_APP_LAST_ACCESSED_PREFIX$packageName", System.currentTimeMillis()).apply()
    }

    fun getAppLastAccessed(packageName: String): Long {
        return prefs.getLong("$KEY_APP_LAST_ACCESSED_PREFIX$packageName", 0)
    }

    // Monitored Domains
    fun getMonitoredDomains(): Set<String> {
        return prefs.getStringSet(KEY_MONITORED_DOMAINS, emptySet()) ?: emptySet()
    }

    fun addMonitoredDomain(domain: String) {
        val domains = getMonitoredDomains().toMutableSet()
        domains.add(normalizeDomain(domain))
        prefs.edit().putStringSet(KEY_MONITORED_DOMAINS, domains).apply()
        Log.d(TAG, "Added monitored domain: $domain")
    }

    fun removeMonitoredDomain(domain: String) {
        val domains = getMonitoredDomains().toMutableSet()
        domains.remove(normalizeDomain(domain))
        prefs.edit().putStringSet(KEY_MONITORED_DOMAINS, domains).apply()
        Log.d(TAG, "Removed monitored domain: $domain")
    }

    fun isDomainMonitored(domain: String): Boolean {
        val normalizedDomain = normalizeDomain(domain)
        val result = getMonitoredDomains().contains(normalizedDomain)

        // Also check for subdomain matches if full domain not found
        if (!result) {
            getMonitoredDomains().forEach { monitoredDomain ->
                if (normalizedDomain.endsWith(".$monitoredDomain")) {
                    return true
                }
            }
        }

        return result
    }

    // Domain Access Handling
    fun handleDomainAccess(domain: String): Boolean {
        val normalizedDomain = normalizeDomain(domain)
        if (isDomainMonitored(normalizedDomain)) {
            updateDomainLastAccessed(normalizedDomain)
            Log.d(TAG, "Handled access to monitored domain: $normalizedDomain")
            return true
        }
        return false
    }

    // Domain Last Accessed Times
    fun updateDomainLastAccessed(domain: String) {
        val normalizedDomain = normalizeDomain(domain)
        prefs.edit().putLong("$KEY_DOMAIN_LAST_ACCESSED_PREFIX$normalizedDomain", System.currentTimeMillis()).apply()
    }

    fun getDomainLastAccessed(domain: String): Long {
        val normalizedDomain = normalizeDomain(domain)
        return prefs.getLong("$KEY_DOMAIN_LAST_ACCESSED_PREFIX$normalizedDomain", 0)
    }

    // Helper to normalize domain (remove http://, https://, www. prefixes)
    private fun normalizeDomain(domain: String): String {
        return domain
            .replace(Regex("^https?://"), "")
            .replace(Regex("^www\\."), "")
            .lowercase()
    }

    companion object {
        private const val PREFS_NAME = "decondition_prefs"
        private const val KEY_CHALLENGE_TYPE = "challenge_type"
        private const val KEY_MONITORED_APPS = "monitored_apps"
        private const val KEY_MONITORED_DOMAINS = "monitored_domains"
        private const val KEY_APP_NAME_PREFIX = "app_name_"
        private const val KEY_APP_LAST_ACCESSED_PREFIX = "app_last_accessed_"
        private const val KEY_DOMAIN_LAST_ACCESSED_PREFIX = "domain_last_accessed_"
    }
}