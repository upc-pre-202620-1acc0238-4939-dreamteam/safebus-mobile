package com.dreamteam.safebus.iam.data

import com.dreamteam.safebus.iam.domain.Role
import com.dreamteam.safebus.iam.domain.Session
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.time.Instant

data class SessionDto(val accessToken: String, val tokenType: String, val role: String, val expiresAt: String) {
    override fun toString(): String = "SessionDto(redacted)"
    fun toDomain(subject: String, claimRole: String, companyId: Long?, tokenExpiry: Instant, now: Instant): Session {
        require(tokenType == "Bearer" && role == claimRole)
        val expiry = minOf(Instant.parse(expiresAt), tokenExpiry)
        require(expiry.isAfter(now))
        return Session(subject, Role.valueOf(role), accessToken, expiry, companyId)
    }
}

object SessionRecordCodec {
    fun encode(session: Session): ByteArray = ByteArrayOutputStream().also { bytes ->
        DataOutputStream(bytes).use { output ->
            output.writeInt(1)
            output.writeUTF(session.userId)
            output.writeUTF(session.role.name)
            output.writeUTF(session.accessToken)
            output.writeUTF(session.expiresAt.toString())
            output.writeLong(session.companyId ?: 0)
        }
    }.toByteArray()
    fun decode(bytes: ByteArray): Session = DataInputStream(ByteArrayInputStream(bytes)).use { input ->
        require(input.readInt() == 1)
        val session = Session(input.readUTF(), Role.valueOf(input.readUTF()), input.readUTF(), Instant.parse(input.readUTF()), input.readLong().takeIf { it > 0 })
        require(input.available() == 0)
        session
    }
}
