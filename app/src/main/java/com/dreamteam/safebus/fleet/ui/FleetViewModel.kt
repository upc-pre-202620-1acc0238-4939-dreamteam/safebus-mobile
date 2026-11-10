package com.dreamteam.safebus.fleet.ui

import com.dreamteam.safebus.fleet.domain.*
import com.dreamteam.safebus.shared.domain.*
import com.dreamteam.safebus.shared.ui.TaskViewModel
import kotlinx.coroutines.*

data class FleetState(val overview: FleetOverview? = null, val receipt: ProvisionReceipt? = null, val savedChange: FleetChange? = null)

class FleetViewModel(private val repository: FleetRepository, dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate) : TaskViewModel<FleetState>(FleetState(), dispatcher) {
    fun refresh() = execute { old -> repository.overview().map { old.copy(overview = it) } }
    fun save(change: FleetChange) = execute { old ->
        SaveFleetResource(repository)(change).map { old.copy(receipt = it, savedChange = if (change is FleetChange.Driver) change.copy(password = "") else change) }
    }
    fun clearReceipt() { update(state.value.value.copy(receipt = null, savedChange = null)) }
}
