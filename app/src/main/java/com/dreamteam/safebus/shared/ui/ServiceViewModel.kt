package com.dreamteam.safebus.shared.ui

import com.dreamteam.safebus.shared.domain.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

data class ServiceState(val contact: ContactState = ContactState(), val available: Boolean = false)
class ServiceViewModel(private val service: ServiceContact, private val repository: ContactRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate) : TaskViewModel<ServiceState>(ServiceState(), dispatcher) {
    fun restore() = execute { old -> repository.restore().map { old.copy(contact = it) } }
    fun health() { update(state.value.value.copy(available = false)); execute { old -> service.health().map { old.copy(available = it) } } }
    fun submit(input: ContactSubmission) = execute { old ->
        when (val result = repository.submit(input)) {
            is DomainResult.Success -> DomainResult.Success(old.copy(contact = result.value))
            is DomainResult.Error -> {
                val restored = repository.restore()
                if (restored is DomainResult.Success) update(old.copy(contact = restored.value))
                result
            }
        }
    }
    fun clear() = execute { old -> repository.clear().map { old.copy(contact = it) } }
}
