package com.dreamteam.safebus.safetycase.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.dreamteam.safebus.R
import com.dreamteam.safebus.safetycase.domain.PassengerRequest
import com.dreamteam.safebus.shared.ui.*

@Composable fun RequestDetailScreen(state: ScreenState<PassengerRequest?>, refresh: () -> Unit, back: () -> Unit) {
    CorePage(R.string.extra_request_detail_title, state.busy, state.failure, refresh, back) {
        CoreButton(R.string.extra_close_detail, "detailBack", secondary = true, action = back)
        state.value?.let { request -> CoreCard {
            Text(stringResource(R.string.core_case_reference, request.id))
            Text(request.message, Modifier.testTag("requestDetailMessage"))
            StateLabel(request.status); TimeLabel(request.submittedAt)
            request.contributorCount?.let { Text(stringResource(R.string.core_contributors, it)) }
            request.groupReference?.let { Text(it) }; request.decision?.let { StateLabel(it) }
            request.decidedAt?.let { TimeLabel(it) }; request.response?.let { Text(it) }
        } }
        CoreNotice(stringResource(R.string.ui_private_evidence), R.drawable.ic_lock)
    }
}
