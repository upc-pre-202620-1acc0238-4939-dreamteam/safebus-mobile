package com.dreamteam.safebus.shared.domain

import org.junit.Assert.*
import org.junit.Test

class DomainResultTest {
    @Test fun mapsEveryApiErrorWithoutTreatingFailuresAsData() {
        val expected = mapOf(401 to FailureKind.UNAUTHORIZED, 403 to FailureKind.FORBIDDEN,
            404 to FailureKind.NOT_FOUND, 409 to FailureKind.CONFLICT, 422 to FailureKind.VALIDATION,
            413 to FailureKind.VALIDATION, 429 to FailureKind.RATE_LIMITED, 503 to FailureKind.SERVER)
        for ((status, kind) in expected) {
            val error = DomainResult.Error(Failure.fromStatus(status, "SERVER_CODE"))
            var persisted = false
            val result = error.map { persisted = true; "invalid" }
            assertSame(error, result)
            assertEquals(kind, error.failure.kind)
            assertEquals(status, error.failure.status)
            assertEquals("SERVER_CODE", error.failure.code)
            assertFalse(persisted)
        }
    }

    @Test fun retriesOnlyTransientFailuresAndPreservesSuccessfulValues() {
        assertTrue(Failure(FailureKind.NETWORK).retryable)
        assertFalse(Failure.fromStatus(409).retryable)
        assertFalse(Failure.fromStatus(422).retryable)
        assertEquals(FailureKind.INVALID_RESPONSE, Failure.fromStatus(302).kind)
        assertEquals(DomainResult.Success(6), DomainResult.Success(3).map { it * 2 })
    }
}
