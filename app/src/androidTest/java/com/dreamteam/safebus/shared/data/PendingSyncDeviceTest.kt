package com.dreamteam.safebus.shared.data

import android.app.job.JobScheduler
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test

class PendingSyncDeviceTest {
    @Test fun schedulesPersistedNetworkJobWithoutCredentialsInExtras() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val scheduler = context.getSystemService(JobScheduler::class.java)
        PendingSyncService.schedule(context)
        val job = requireNotNull(scheduler.getPendingJob(PendingSyncService.JOB_ID))
        assertTrue(job.isPersisted)
        assertTrue(job.extras.isEmpty)
        assertNotNull(job.requiredNetwork)
        scheduler.cancel(PendingSyncService.JOB_ID)
    }
}
