package com.dreamteam.safebus.trip.data

import com.dreamteam.safebus.shared.domain.*
import org.junit.Assert.*
import org.junit.Test

class CounterFailureTest {
    @Test fun sequenceConflictsPreserveRecoveryInformationWithoutRetryingAutomatically() {
        val failure = Failure.fromStatus(409, "SEQUENCE_GAP").copy(expectedSequence = 4)
        assertFalse(failure.retryable); assertEquals(4L, failure.expectedSequence)
        assertEquals(FailureKind.CONFLICT, failure.kind)
    }
}
