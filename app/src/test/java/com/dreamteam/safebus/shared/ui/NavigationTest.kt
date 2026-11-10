package com.dreamteam.safebus.shared.ui

import com.dreamteam.safebus.iam.domain.Role
import org.junit.Assert.*
import org.junit.Test

class NavigationTest {
    @Test fun eachRoleGetsOnlyItsAuthorizedNavigation() {
        assertEquals(listOf(Destination.SHIFT, Destination.DRIVER_ALERTS, Destination.ACCOUNT), destinations(Role.DRIVER))
        assertEquals(listOf(Destination.JOURNEY, Destination.HISTORY, Destination.REQUESTS, Destination.ACCOUNT), destinations(Role.PASSENGER))
        assertEquals(listOf(Destination.FLEET, Destination.EMERGENCIES, Destination.REVIEWS, Destination.ASSIGNMENTS, Destination.ACCOUNT), destinations(Role.SUPERVISOR))
    }
}
