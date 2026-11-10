package com.dreamteam.safebus.shared.data

import java.net.InetAddress
import java.net.URI
import java.util.Locale

fun isLocalServer(endpoint: String): Boolean {
    val host = runCatching { URI(endpoint).host?.lowercase(Locale.ROOT)?.removeSurrounding("[", "]") }.getOrNull() ?: return false
    if (host == "localhost" || host.endsWith(".localhost") || host.endsWith(".local")) return true
    if (':' !in host && !host.matches(Regex("[0-9.]+"))) return false
    return runCatching {
        val address = InetAddress.getByName(host)
        address.isLoopbackAddress || address.isSiteLocalAddress || address.isLinkLocalAddress ||
            (address.address.size == 16 && (address.address[0].toInt() and 0xfe) == 0xfc)
    }.getOrDefault(false)
}
