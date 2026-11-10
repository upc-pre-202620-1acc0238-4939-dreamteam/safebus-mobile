package com.dreamteam.safebus

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.security.NetworkSecurityPolicy
import androidx.test.platform.app.InstrumentationRegistry
import androidx.core.content.edit
import com.dreamteam.safebus.shared.data.LocationTrackingService
import org.junit.Assert.*
import org.junit.Test

class ApplicationConfigurationTest {
    @Test fun configurationPersistsNormalizedEndpointAndRejectsInvalidChanges() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val application = instrumentation.targetContext.applicationContext as SafeBusApplication
        val preferences = application.getSharedPreferences("connection", Context.MODE_PRIVATE)
        val previous = preferences.getString("endpoint", null)
        val originalEndpoint = application.endpoint
        instrumentation.runOnMainSync {
            try {
                assertTrue(application.configure("  https://configuration.safebus.invalid/  "))
                assertEquals("https://configuration.safebus.invalid", application.endpoint)
                assertEquals(application.endpoint, preferences.getString("endpoint", null))
                assertNotNull(application.container.value)
                val configured = application.container.value
                assertFalse(application.configure("not a URL"))
                assertEquals("https://configuration.safebus.invalid", preferences.getString("endpoint", null))
                assertSame(configured, application.container.value)
            } finally {
                application.configure(originalEndpoint)
                preferences.edit { if (previous == null) remove("endpoint") else putString("endpoint", previous) }
            }
        }
    }
    @Test fun declaresModernPackageAndPrivateLocationService() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.dreamteam.safebus", context.packageName)
        assertEquals(37, context.applicationInfo.targetSdkVersion)
        val permissions = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS).requestedPermissions.orEmpty()
        assertTrue(permissions.contains(android.Manifest.permission.ACCESS_LOCAL_NETWORK))
        assertEquals(PackageManager.PERMISSION_GRANTED, context.checkSelfPermission(android.Manifest.permission.INTERNET))
        val service = context.packageManager.getServiceInfo(ComponentName(context, LocationTrackingService::class.java), 0)
        assertFalse(service.exported)
        assertEquals(ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION, service.foregroundServiceType)
        assertTrue(NetworkSecurityPolicy.getInstance().isCleartextTrafficPermitted("10.0.2.2"))
        assertFalse(NetworkSecurityPolicy.getInstance().isCleartextTrafficPermitted("external.example"))
    }
}
