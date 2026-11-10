package com.dreamteam.safebus.shared.data

import android.app.job.*
import android.content.ComponentName
import android.content.Context
import com.dreamteam.safebus.SafeBusApplication
import com.dreamteam.safebus.shared.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class PendingSyncService : JobService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var work: Job? = null
    override fun onStartJob(params: JobParameters): Boolean {
        work = scope.launch {
            val container = (application as SafeBusApplication).container.value
            if (container == null) { jobFinished(params, false); return@launch }
            container.authModel.state.first { !it.busy }
            val session = container.auth.session.value
            if (session == null) { jobFinished(params, false); return@launch }
            val account = container.bind(session)
            val result = account.outbox.flush()
            account.syncModel.count()
            jobFinished(params, shouldRetry(result))
        }
        return true
    }
    override fun onStopJob(params: JobParameters): Boolean { work?.cancel(); return true }
    override fun onDestroy() { scope.cancel(); super.onDestroy() }
    companion object {
        const val JOB_ID = 2402
        fun shouldRetry(result: DomainResult<Int>): Boolean = when (result) {
            is DomainResult.Success -> result.value > 0
            is DomainResult.Error -> result.failure.retryable || result.failure.kind == FailureKind.STORAGE
        }
        fun schedule(context: Context) {
            val scheduler = context.getSystemService(JobScheduler::class.java)
            if (scheduler.getPendingJob(JOB_ID) != null) return
            scheduler.schedule(JobInfo.Builder(JOB_ID, ComponentName(context, PendingSyncService::class.java))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setMinimumLatency(1_000)
                .setBackoffCriteria(30_000, JobInfo.BACKOFF_POLICY_EXPONENTIAL).setPersisted(true).build())
        }
    }
}
