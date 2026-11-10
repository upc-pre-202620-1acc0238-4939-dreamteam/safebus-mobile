package com.dreamteam.safebus.shared.data

import com.dreamteam.safebus.shared.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class DeviceAuthenticationTest {
    @Test fun sendsOnlyDeviceKeyAndRestrictsItToDeviceRoutes() = runBlocking {
        val requests = mutableListOf<ApiRequest>()
        var key = "secret-source-key"
        val api = ApiClient("https://example.com", false, { key }, {}, HttpEngine {
            requests.add(it); ApiResponse(200, byteArrayOf())
        }, Dispatchers.Unconfined, deviceAuthentication = true)
        assertTrue(api.call("POST", "/api/v1/occupancy-heartbeats") { Unit } is DomainResult.Success)
        assertNull(requests.single().token); assertEquals(key, requests.single().countingKey)
        assertFalse(requests.single().toString().contains(key))
        assertEquals(FailureKind.FORBIDDEN, (api.call("GET", "/api/v1/fleet/overview") { Unit } as DomainResult.Error).failure.kind)
        assertEquals(1, requests.size)
        key = "invalid\r\nheader"
        assertTrue(api.call("POST", "/api/v1/occupancy-heartbeats") { Unit } is DomainResult.Error)
        assertEquals(1, requests.size)
    }
}
