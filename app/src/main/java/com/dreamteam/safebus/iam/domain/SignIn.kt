package com.dreamteam.safebus.iam.domain

import com.dreamteam.safebus.shared.domain.DomainResult
import com.dreamteam.safebus.shared.domain.invalid

class SignIn(private val repository: AuthRepository) {
    suspend operator fun invoke(login: String, password: String): DomainResult<Session> =
        if (login.isBlank() || password.isBlank()) invalid("CREDENTIALS_REQUIRED")
        else repository.signIn(login.trim(), password)
}
