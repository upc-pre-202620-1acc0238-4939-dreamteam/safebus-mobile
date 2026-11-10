package com.dreamteam.safebus.shared.data

import org.junit.Assert.*
import org.junit.Test

class LocalServerTest {
    @Test fun recognizesEmulatorLoopbackAndPrivateNetworkEndpoints() {
        listOf("http://10.0.2.2:8080", "http://127.0.0.1:8080", "http://localhost:8080",
            "https://api.local", "https://API.LOCALHOST", "https://192.168.1.3", "https://172.16.0.1",
            "https://172.31.255.255", "https://169.254.1.1", "https://[::1]", "https://[fd12::1]",
            "https://[fc12::1]", "https://[fe80::1]").forEach { assertTrue(it, isLocalServer(it)) }
    }
    @Test fun publicAndInvalidEndpointsDoNotRequestLocalAccess() {
        listOf("https://api.safebus.example", "https://api.local.example", "https://8.8.8.8",
            "https://172.15.255.255", "https://172.32.0.0", "https://[2001:4860:4860::8888]",
            "", "not a URL", "https://999.999.999.999").forEach { assertFalse(it, isLocalServer(it)) }
    }
}
