package com.dreamteam.safebus.fleet.domain

import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder

data class BusQr(val code: String, val busId: Long? = null) {
    init { require(code.isNotBlank() && (busId == null || busId > 0)) }
    fun encoded(): String = busId?.let { "safebus://bus/$it?code=${URLEncoder.encode(code, "UTF-8")}" } ?: code
    companion object {
        fun parse(value: String): BusQr {
            val text = value.trim()
            if (!text.startsWith("safebus://")) return BusQr(text)
            val uri = URI(text)
            require(uri.host == "bus" && uri.fragment == null && uri.rawQuery.startsWith("code=") && !uri.rawQuery.contains('&'))
            return BusQr(URLDecoder.decode(uri.rawQuery.substringAfter("code="), "UTF-8"), uri.path.removePrefix("/").toLong())
        }
    }
}

