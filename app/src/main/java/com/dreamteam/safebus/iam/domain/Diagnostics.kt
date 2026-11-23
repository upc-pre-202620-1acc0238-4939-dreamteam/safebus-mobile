package com.dreamteam.safebus.iam.domain

data class SystemDiagnostics(
    val appVersion: String = "0.1.0",
    val versionCode: Int = 1,
    val endpoint: String,
    val role: Role,
    val userId: String,
    val pendingSyncs: Int = 0
) {
    val isLocalHost: Boolean get() = endpoint.contains("10.0.2.2") || endpoint.contains("127.0.0.1") || endpoint.contains("localhost")
    val hasPendingWork: Boolean get() = pendingSyncs > 0

    fun sanitizedEndpoint(): String {
        return endpoint.trim().removeSuffix("/")
    }
}
