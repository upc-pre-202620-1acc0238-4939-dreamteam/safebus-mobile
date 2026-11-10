package com.dreamteam.safebus.trip.ui

import com.dreamteam.safebus.shared.domain.*
import com.dreamteam.safebus.shared.ui.TaskViewModel
import com.dreamteam.safebus.trip.domain.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import java.time.Instant
import java.util.UUID

data class CountingState(val sources: List<CountingSource> = emptyList(), val occupancy: Occupancy? = null,
    val credential: SourceCredential? = null, val loaded: Boolean = false)
class CountingViewModel(private val repository: CountingRepository, val busId: Long,
    dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate) : TaskViewModel<CountingState>(CountingState(), dispatcher) {
    fun refresh() = execute { old ->
        when (val sources = repository.sources(busId)) {
            is DomainResult.Error -> sources
            is DomainResult.Success -> repository.occupancy(busId).map { old.copy(sources = sources.value, occupancy = it, loaded = true) }
        }
    }
    fun register(label: String) = execute { old -> repository.register(busId, label).map { credential ->
        old.copy(sources = old.sources.filterNot { it.id == credential.source.id } + credential.source, credential = credential)
    } }
    fun rotate(sourceId: Long) { clearCredential(); execute { old -> repository.rotate(busId, sourceId).map { credential ->
        old.copy(sources = old.sources.map { if (it.id == sourceId) credential.source else it }, credential = credential)
    } } }
    fun revoke(sourceId: Long) { clearCredential(); execute { old -> repository.revoke(busId, sourceId).map { source ->
        old.copy(sources = old.sources.map { if (it.id == sourceId) source else it })
    } } }
    fun clearCredential() { update(state.value.value.copy(credential = null)) }
}

data class CounterUiState(val counter: CounterState = CounterState(), val heartbeat: Instant? = null)
class CounterViewModel(private val repository: CounterRepository, private val busId: Long, private val shiftId: Long,
    dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate) : TaskViewModel<CounterUiState>(CounterUiState(), dispatcher) {
    fun restore() = execute { old -> repository.restore().map { old.copy(counter = it) } }
    private suspend fun reflect(result: DomainResult<CounterState>, old: CounterUiState): DomainResult<CounterUiState> = when (result) {
        is DomainResult.Success -> DomainResult.Success(old.copy(counter = result.value))
        is DomainResult.Error -> {
            val restored = repository.restore()
            if (restored is DomainResult.Success) update(old.copy(counter = restored.value))
            result
        }
    }
    fun send(action: CountAction, sequence: Long, key: String, confirmedEmpty: Boolean) = execute { old ->
        if (sequence <= 0 || sequence == Long.MAX_VALUE || key.isBlank() || (action == CountAction.INITIALIZE && !confirmedEmpty)) invalid("COUNTING_FIELDS_REQUIRED")
        else reflect(repository.submit(CountCommand(UUID.randomUUID().toString(), busId, shiftId, action, sequence, Instant.now()), key), old)
    }
    fun retry(key: String) = execute { old -> reflect(repository.retry(key), old) }
    fun heartbeat(key: String) = execute { old -> repository.heartbeat(busId, key).map { old.copy(heartbeat = it) } }
}
