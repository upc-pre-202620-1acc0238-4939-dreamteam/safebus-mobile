package com.dreamteam.safebus.safetycase.domain

import com.dreamteam.safebus.shared.domain.DomainResult

interface RequestDetailRepository {
    suspend fun detail(id: String): DomainResult<PassengerRequest>
}
