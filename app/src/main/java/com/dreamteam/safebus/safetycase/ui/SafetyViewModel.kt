package com.dreamteam.safebus.safetycase.ui

import com.dreamteam.safebus.safetycase.domain.*
import com.dreamteam.safebus.shared.domain.*
import com.dreamteam.safebus.shared.ui.TaskViewModel
import kotlinx.coroutines.*

data class SafetyState(val requests: List<PassengerRequest> = emptyList(), val driverAlerts: List<Emergency> = emptyList(),
    val queue: ReviewQueue? = null, val history: List<SafetyEntry> = emptyList(), val evidence: Photo? = null,
    val receipt: Receipt? = null, val decisionSaved: Boolean = false, val loaded: Boolean = false)

class SafetyViewModel(private val repository: SafetyRepository, dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate) : TaskViewModel<SafetyState>(SafetyState(), dispatcher) {
    fun requests() = execute { old -> repository.mine().map { old.copy(requests = it, loaded = true) } }
    fun alerts() = execute { old -> repository.driverAlerts().map { old.copy(driverAlerts = it, loaded = true) } }
    fun review() = execute { old -> repository.queue().map { old.copy(queue = it, loaded = true) } }
    fun history(busId: Long?) = execute { old -> if (busId == null) invalid("BUS_ID_UNAVAILABLE") else repository.history(busId).map { old.copy(history = it, loaded = true) } }
    fun submit(journeyId: Long?, message: String, photo: Photo?) = execute { old -> SubmitRequest(repository)(journeyId, message, photo).map { old.copy(receipt = it) } }
    fun evidence(id: String, supervisor: Boolean) = execute { old -> repository.evidence(id, supervisor).map { old.copy(evidence = it) } }
    fun clearEvidence() { update(state.value.value.copy(evidence = null)) }
    fun clearReceipt() { update(state.value.value.copy(receipt = null)) }
    fun approve(id: Long) = decision { repository.approve(id) }
    fun decline(id: Long, reason: String, response: String) = decision { repository.decline(id, reason, response) }
    fun attend(id: String) = decision { repository.attend(id) }
    fun close(id: String, outcome: String, response: String) = decision { repository.close(id, outcome, response) }
    private fun decision(action: suspend () -> DomainResult<Unit>) = execute { old ->
        when (val result = action()) {
            is DomainResult.Error -> result
            is DomainResult.Success -> {
                update(old.copy(decisionSaved = true))
                repository.queue().map { old.copy(queue = it, decisionSaved = true) }
            }
        }
    }
}

class PanicViewModel(private val repository: SafetyRepository, dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate) : TaskViewModel<Receipt?>(null, dispatcher) {
    fun panic(shiftId: Long?) = execute { if (shiftId == null) invalid("SHIFT_REQUIRED") else repository.panic(shiftId) }
}
