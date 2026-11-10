package com.dreamteam.safebus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import org.junit.Assert.*
import org.junit.Test

class ModelStoreTest {
    class Probe : ViewModel() {
        var disposed = false
        override fun onCleared() { disposed = true }
    }
    @Test fun providerReusesOnlyMatchingAccountKeyAndDisposesModels() {
        val store = ViewModelStore()
        val first = store.model("account-1") { Probe() }
        assertSame(first, store.model("account-1") { Probe() })
        val second = store.model("account-2") { Probe() }
        assertNotSame(first, second)
        store.clear()
        assertTrue(first.disposed); assertTrue(second.disposed)
    }
}
