package com.dreamteam.safebus.iam.ui

import com.dreamteam.safebus.iam.domain.*
import com.dreamteam.safebus.shared.domain.*
import com.dreamteam.safebus.shared.ui.TaskViewModel
import kotlinx.coroutines.*

data class AuthState(val session: Session? = null)

class AuthViewModel(private val repository: AuthRepository,
    private val beforeSignOut: suspend () -> DomainResult<Unit> = { DomainResult.Success(Unit) },
    dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate) : TaskViewModel<AuthState>(AuthState(), dispatcher) {
    init { scope.launch { repository.session.collect { update(state.value.value.copy(session = it)) } } }
    fun restore() = execute { old -> repository.restore().map { old.copy(session = it) } }
    fun signIn(login: String, password: String) = execute { old -> SignIn(repository)(login, password).map { old.copy(session = it) } }
    fun signOut() = execute {
        val preparation = beforeSignOut()
        val result = repository.signOut()
        if (preparation is DomainResult.Error) preparation else result.map { AuthState() }
    }
}
