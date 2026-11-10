package com.dreamteam.safebus

import android.content.ComponentName
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.security.NetworkSecurityPolicy
import androidx.test.platform.app.InstrumentationRegistry
import com.dreamteam.safebus.shared.data.LocationTrackingService
import org.junit.Assert.*
import org.junit.Test

class ApplicationConfigurationTest {
    @Test fun declaresModernPackageAndPrivateLocationService() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.dreamteam.safebus", context.packageName)
        assertTrue(context.applicationInfo.targetSdkVersion >= 34)
        assertEquals(PackageManager.PERMISSION_GRANTED, context.checkSelfPermission(android.Manifest.permission.INTERNET))
        val service = context.packageManager.getServiceInfo(ComponentName(context, LocationTrackingService::class.java), 0)
        assertFalse(service.exported)
        assertEquals(ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION, service.foregroundServiceType)
        assertTrue(NetworkSecurityPolicy.getInstance().isCleartextTrafficPermitted("10.0.2.2"))
        assertFalse(NetworkSecurityPolicy.getInstance().isCleartextTrafficPermitted("external.example"))
    }
}
