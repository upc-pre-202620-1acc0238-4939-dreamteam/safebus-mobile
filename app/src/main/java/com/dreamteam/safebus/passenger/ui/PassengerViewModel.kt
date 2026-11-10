package com.dreamteam.safebus.passenger.ui

import com.dreamteam.safebus.passenger.domain.*
import com.dreamteam.safebus.shared.domain.*
import com.dreamteam.safebus.shared.ui.TaskViewModel
import kotlinx.coroutines.*

data class PassengerState(val journey: Journey? = null, val ended: Delivery? = null, val loaded: Boolean = false)

class PassengerViewModel(private val repository: PassengerRepository, private val changed: (Journey?) -> Unit = {},
    dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate) : TaskViewModel<PassengerState>(PassengerState(), dispatcher) {
    fun restore() = execute { old -> repository.restore().map { changed(it); old.copy(journey = it, loaded = true) } }
    fun synchronize(journey: Journey?) { update(state.value.value.copy(journey = journey)) }
    fun start(value: String) = execute { old ->
        val qr = try { BusQr.parse(value) } catch (_: Exception) { return@execute invalid("QR_INVALID") }
        repository.start(qr).map { changed(it); old.copy(journey = it, ended = null) }
    }
    fun end() = execute { old ->
        val journey = old.journey ?: return@execute invalid("JOURNEY_REQUIRED")
        repository.end(journey, EndReason.MANUAL).map { changed(null); old.copy(journey = null, ended = it) }
    }
}
