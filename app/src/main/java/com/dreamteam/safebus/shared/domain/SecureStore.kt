package com.dreamteam.safebus.shared.domain

interface SecureStore {
    fun read(key: String): ByteArray?
    fun write(key: String, value: ByteArray)
    fun remove(key: String)
}
