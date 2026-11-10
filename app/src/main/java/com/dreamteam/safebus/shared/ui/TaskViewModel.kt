package com.dreamteam.safebus.shared.ui

import androidx.lifecycle.ViewModel
import com.dreamteam.safebus.shared.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ScreenState<T>(val value: T, val busy: Boolean = false, val failure: Failure? = null)

open class TaskViewModel<T>(initial: T, dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate) : ViewModel() {
    protected val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val mutable = MutableStateFlow(ScreenState(initial))
    val state = mutable.asStateFlow()

    protected fun update(value: T) { mutable.value = mutable.value.copy(value = value) }
    protected fun execute(action: suspend (T) -> DomainResult<T>) {
        if (mutable.value.busy) return
        mutable.value = mutable.value.copy(busy = true, failure = null)
        scope.launch {
            val result = try { action(mutable.value.value) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { DomainResult.Error(Failure(FailureKind.STORAGE)) }
            mutable.value = when (result) {
                is DomainResult.Success -> ScreenState(result.value)
                is DomainResult.Error -> mutable.value.copy(busy = false, failure = result.failure)
            }
        }
    }
    override fun onCleared() { scope.cancel() }
}
