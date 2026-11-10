package com.dreamteam.safebus.shared.domain

sealed interface DomainResult<out T> {
    data class Success<T>(val value: T) : DomainResult<T>
    data class Error(val failure: Failure) : DomainResult<Nothing>
}

enum class FailureKind { NETWORK, UNAUTHORIZED, FORBIDDEN, NOT_FOUND, CONFLICT, VALIDATION, RATE_LIMITED, SERVER, INVALID_RESPONSE, STORAGE }

data class Failure(val kind: FailureKind, val code: String = "", val status: Int? = null, val expectedSequence: Long? = null) {
    val retryable: Boolean get() = kind in setOf(FailureKind.NETWORK, FailureKind.SERVER, FailureKind.RATE_LIMITED)

    companion object {
        fun fromStatus(status: Int, code: String = ""): Failure = Failure(when (status) {
            401 -> FailureKind.UNAUTHORIZED
            403 -> FailureKind.FORBIDDEN
            404 -> FailureKind.NOT_FOUND
            409 -> FailureKind.CONFLICT
            400, 413, 415, 422 -> FailureKind.VALIDATION
            429 -> FailureKind.RATE_LIMITED
            in 500..599 -> FailureKind.SERVER
            else -> FailureKind.INVALID_RESPONSE
        }, code, status)
    }
}

inline fun <T, R> DomainResult<T>.map(transform: (T) -> R): DomainResult<R> = when (this) {
    is DomainResult.Success -> DomainResult.Success(transform(value))
    is DomainResult.Error -> this
}

fun invalid(code: String): DomainResult.Error = DomainResult.Error(Failure(FailureKind.VALIDATION, code))
