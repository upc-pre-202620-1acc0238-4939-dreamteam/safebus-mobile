package com.dreamteam.safebus.shared.domain

class Photo(bytes: ByteArray, val mime: String) {
    private val content = bytes.copyOf()
    val bytes: ByteArray get() = content.copyOf()
    init {
        require(content.isNotEmpty() && content.size <= 5 * 1024 * 1024)
        require(mime == "image/jpeg" || mime == "image/png")
    }
    override fun toString() = "Photo(redacted)"
}
