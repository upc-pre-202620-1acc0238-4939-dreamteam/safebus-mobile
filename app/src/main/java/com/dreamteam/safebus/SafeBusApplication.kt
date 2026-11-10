package com.dreamteam.safebus

import android.app.Application
import android.content.Intent
import com.dreamteam.safebus.shared.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.Instant

class SafeBusApplication : Application() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var sessionWatcher: Job? = null
    private val mutableContainer = MutableStateFlow<CoreContainer?>(null)
    val container = mutableContainer.asStateFlow()
    private val preferences by lazy { getSharedPreferences("connection", MODE_PRIVATE) }
    val endpoint: String get() = preferences.getString("endpoint", null) ?: BuildConfig.API_BASE_URL
    override fun onCreate() { super.onCreate(); connect(endpoint) }
    fun configure(value: String): Boolean {
        val endpoint = value.trim().trimEnd('/')
        if (!ApiClient.validBaseUrl(endpoint, BuildConfig.DEBUG)) return false
        preferences.edit().putString("endpoint", endpoint).apply()
        connect(endpoint)
        return true
    }
    private fun connect(endpoint: String) {
        sessionWatcher?.cancel()
        stopService(Intent(this, LocationTrackingService::class.java))
        mutableContainer.value?.dispose()
        val next = if (ApiClient.validBaseUrl(endpoint, BuildConfig.DEBUG)) CoreContainer(endpoint, BuildConfig.DEBUG, EncryptedStore(this)) else null
        mutableContainer.value = next
        if (next != null) {
            sessionWatcher = scope.launch {
                next.auth.session.collectLatest { session ->
                    if (session == null) {
                        stopService(Intent(this@SafeBusApplication, LocationTrackingService::class.java))
                        next.detach()
                    } else {
                        val milliseconds = java.time.Duration.between(Instant.now(), session.expiresAt).toMillis().coerceAtLeast(0)
                        delay(milliseconds)
                        withContext(Dispatchers.IO) { next.auth.invalidate(session.accessToken) }
                    }
                }
            }
            next.authModel.restore()
        }
    }
}
