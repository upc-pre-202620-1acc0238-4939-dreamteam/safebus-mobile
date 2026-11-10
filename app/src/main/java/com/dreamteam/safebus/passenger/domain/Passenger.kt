package com.dreamteam.safebus.passenger.domain

import com.dreamteam.safebus.shared.domain.*
import com.dreamteam.safebus.trip.domain.Position
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.time.Instant

data class Journey(val id: Long, val busId: Long?, val plate: String, val company: String, val validation: String,
    val route: String, val origin: String, val destination: String, val driver: String, val startedAt: Instant) {
    init { require(id > 0 && (busId == null || busId > 0) && plate.isNotBlank()) }
}

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

data class Registration(val dni: String, val password: String, val acceptedTerms: Boolean, val face: Photo) {
    fun valid() = dni.matches(Regex("[0-9]{8}")) && password.length >= 8 && acceptedTerms
    override fun toString() = "Registration(redacted)"
}

enum class EndReason { MANUAL, AUTOMATIC_SEPARATION, SIGN_OUT }
data class Separation(val meters: Int, val seconds: Int)

interface PassengerRepository {
    suspend fun register(input: Registration): DomainResult<Long>
    suspend fun restore(): DomainResult<Journey?>
    suspend fun start(qr: BusQr): DomainResult<Journey>
    suspend fun end(journey: Journey, reason: EndReason, separation: Separation? = null): DomainResult<Delivery>
    suspend fun location(busId: Long): DomainResult<Position>
}

class SeparationDetector {
    private var since: Instant? = null
    private var last: Instant? = null
    fun reset() { since = null; last = null }
    fun compare(passenger: Position?, bus: Position?, now: Instant): Separation? {
        if (passenger == null || bus == null || !passenger.reliableAt(now) || !bus.reliableAt(now)) { reset(); return null }
        val distance = passenger.distanceTo(bus)
        if (distance <= 100) { reset(); return null }
        if (last == null || now < last || now > last!!.plusSeconds(30)) since = now
        last = now
        val seconds = java.time.Duration.between(since, now).seconds
        return if (seconds >= 60) Separation(distance.toInt(), seconds.toInt()) else null
    }
}
