package com.dreamteam.safebus.shared.ui

import com.dreamteam.safebus.shared.domain.*
import kotlinx.coroutines.*

class SyncViewModel(private val pending: suspend () -> DomainResult<Int>, private val flush: suspend () -> DomainResult<Int>,
    dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate) : TaskViewModel<Int?>(null, dispatcher) {
    fun count() = execute { pending() }
    fun retry() = execute {
        when (val result = flush()) {
            is DomainResult.Success -> result
            is DomainResult.Error -> {
                val current = pending()
                if (current is DomainResult.Success) update(current.value)
                result
            }
        }
    }
}
