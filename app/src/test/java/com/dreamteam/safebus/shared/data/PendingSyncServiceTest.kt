package com.dreamteam.safebus.shared.data

import com.dreamteam.safebus.shared.domain.*
import org.junit.Assert.*
import org.junit.Test

class PendingSyncServiceTest {
    @Test fun retriesTransientFailuresButNeverLoopsOnRejectedOrExpiredCredentials() {
        assertTrue(PendingSyncService.shouldRetry(DomainResult.Success(1)))
        assertFalse(PendingSyncService.shouldRetry(DomainResult.Success(0)))
        assertTrue(PendingSyncService.shouldRetry(DomainResult.Error(Failure(FailureKind.NETWORK))))
        assertFalse(PendingSyncService.shouldRetry(DomainResult.Error(Failure.fromStatus(401))))
        assertFalse(PendingSyncService.shouldRetry(DomainResult.Error(Failure.fromStatus(422))))
    }
}
