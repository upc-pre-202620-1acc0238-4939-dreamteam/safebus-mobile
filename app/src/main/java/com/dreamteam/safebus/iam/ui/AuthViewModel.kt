package com.dreamteam.safebus.iam.ui

import com.dreamteam.safebus.iam.domain.*
import com.dreamteam.safebus.passenger.domain.Registration
import com.dreamteam.safebus.shared.domain.*
import com.dreamteam.safebus.shared.ui.TaskViewModel
import kotlinx.coroutines.*

data class AuthState(val session: Session? = null, val registered: Boolean = false)

class AuthViewModel(private val repository: AuthRepository, private val registerAccount: suspend (Registration) -> DomainResult<Long>,
    private val beforeSignOut: suspend () -> DomainResult<Unit> = { DomainResult.Success(Unit) },
    dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate) : TaskViewModel<AuthState>(AuthState(), dispatcher) {
    init { scope.launch { repository.session.collect { update(state.value.value.copy(session = it)) } } }
    fun restore() = execute { old -> repository.restore().map { old.copy(session = it) } }
    fun signIn(login: String, password: String) = execute { old -> SignIn(repository)(login, password).map { old.copy(session = it, registered = false) } }
    fun register(input: Registration) = execute { old ->
        if (!input.valid()) invalid("REGISTRATION_INVALID") else registerAccount(input).map { old.copy(registered = true) }
    }
    fun signOut() = execute {
        val preparation = beforeSignOut()
        val result = repository.signOut()
        if (preparation is DomainResult.Error) preparation else result.map { AuthState() }
    }
}
