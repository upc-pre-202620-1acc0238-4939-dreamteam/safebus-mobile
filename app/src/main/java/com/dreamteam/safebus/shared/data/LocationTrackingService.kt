package com.dreamteam.safebus.shared.data

import android.Manifest
import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import com.dreamteam.safebus.R
import com.dreamteam.safebus.SafeBusApplication
import com.dreamteam.safebus.AccountScope
import com.dreamteam.safebus.iam.domain.Role
import com.dreamteam.safebus.shared.domain.trackingDelayMillis
import com.dreamteam.safebus.trip.domain.Position
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.combine
import java.time.Instant

class LocationTrackingService : Service(), LocationListener {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var work: Job? = null
    private var account: AccountScope? = null
    private var latest: Position? = null
    private val locations by lazy { getSystemService(LocationManager::class.java) }
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val container = (application as SafeBusApplication).container.value
        val active = container?.account
        if (active == null || active.tracker == null || !permitted()) { active?.unavailable(); stopSelf(); return START_NOT_STICKY }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, getString(R.string.core_tracking_channel), NotificationManager.IMPORTANCE_LOW))
        val launch = packageManager.getLaunchIntentForPackage(packageName)
        val notification = Notification.Builder(this, CHANNEL).setSmallIcon(R.drawable.ic_location_on)
            .setContentTitle(getString(R.string.app_name)).setContentText(getString(R.string.core_tracking_notification)).setOngoing(true)
            .apply { if (launch != null) setContentIntent(PendingIntent.getActivity(this@LocationTrackingService, 0, launch, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)) }.build()
        if (Build.VERSION.SDK_INT >= 29) startForeground(2401, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION) else startForeground(2401, notification)
        if (account === active && work?.isActive == true) return START_NOT_STICKY
        work?.cancel(); locations.removeUpdates(this); latest = null; account = active
        try {
            listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).filter { locations.isProviderEnabled(it) }.forEach {
                locations.requestLocationUpdates(it, 15_000L, 0f, this, Looper.getMainLooper())
            }
        } catch (_: SecurityException) { active.unavailable(); stopSelf(); return START_NOT_STICKY }
        work = scope.launch {
            launch { container.auth.session.collect { if (it?.accessToken != active.session.accessToken) stopSelf() } }
            launch { combine(active.shift, active.journey) { shift, journey -> shift != null || journey != null }.collect { if (!it) stopSelf() } }
            while (isActive && permitted()) {
                val beganAt = SystemClock.elapsedRealtime()
                val tracker = active.tracker ?: break
                tracker.tick(latest, Instant.now())
                active.updateTracking()
                active.outbox.flush()
                delay(trackingDelayMillis(active.session.role == Role.DRIVER, SystemClock.elapsedRealtime() - beganAt))
            }
            if (!permitted()) active.unavailable()
            stopSelf()
        }
        return START_NOT_STICKY
    }
    override fun onLocationChanged(location: Location) {
        if (!location.hasAccuracy()) return
        latest = runCatching { Position(location.latitude, location.longitude, location.accuracy.toDouble(), Instant.ofEpochMilli(location.time)) }.getOrNull()
        account?.rememberDriverPosition(latest)
    }
    override fun onProviderDisabled(provider: String) { latest = null; account?.rememberDriverPosition(null); account?.unavailable() }
    private fun permitted() = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    override fun onDestroy() { locations.removeUpdates(this); scope.cancel(); latest = null; account?.rememberDriverPosition(null); super.onDestroy() }
    companion object { private const val CHANNEL = "safebus-location" }
}
