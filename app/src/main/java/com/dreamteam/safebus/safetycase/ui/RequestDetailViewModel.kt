package com.dreamteam.safebus.safetycase.ui

import com.dreamteam.safebus.safetycase.domain.*
import com.dreamteam.safebus.shared.domain.*
import com.dreamteam.safebus.shared.ui.TaskViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

class RequestDetailViewModel(private val repository: RequestDetailRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate) : TaskViewModel<PassengerRequest?>(null, dispatcher) {
    private var generation = 0
    fun clear() { generation++; update(null) }
    fun load(id: String) {
        if (state.value.busy) return
        clear()
        val current = generation
        execute {
            val result = repository.detail(id)
            if (current == generation) result else DomainResult.Success(null)
        }
    }
}
