package com.dreamteam.safebus.trip.ui

import com.dreamteam.safebus.trip.domain.*
import com.dreamteam.safebus.shared.domain.*
import com.dreamteam.safebus.shared.ui.TaskViewModel
import kotlinx.coroutines.*

data class TripState(val assignment: Assignment? = null, val shift: ActiveShift? = null, val loaded: Boolean = false)

class TripViewModel(private val repository: TripRepository, private val changed: (ActiveShift?) -> Unit = {},
    dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate) : TaskViewModel<TripState>(TripState(), dispatcher) {
    fun refresh() = execute { old ->
        when (val restored = repository.restore()) {
            is DomainResult.Error -> restored
            is DomainResult.Success -> {
                changed(restored.value)
                update(old.copy(shift = restored.value))
                when (val result = repository.assignment()) {
                    is DomainResult.Success -> DomainResult.Success(TripState(result.value, restored.value, true))
                    is DomainResult.Error -> if (result.failure.kind == FailureKind.NOT_FOUND)
                        DomainResult.Success(TripState(null, null, true)).also { changed(null) } else result
                }
            }
        }
    }
    fun activate(credential: String) = execute { old ->
        val assignment = old.assignment ?: return@execute invalid("ASSIGNMENT_REQUIRED")
        repository.activate(assignment.id, credential).map { shift -> changed(shift); old.copy(shift = shift) }
    }
    fun close() = execute { old ->
        val shift = old.shift ?: return@execute invalid("SHIFT_REQUIRED")
        repository.close(shift).map { changed(null); old.copy(shift = null, assignment = null) }
    }
}
