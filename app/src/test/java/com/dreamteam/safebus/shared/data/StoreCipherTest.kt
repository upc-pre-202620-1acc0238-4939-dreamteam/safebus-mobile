package com.dreamteam.safebus.shared.data

import org.junit.Assert.*
import org.junit.Test
import javax.crypto.KeyGenerator

class StoreCipherTest {
    private val cipher = StoreCipher(KeyGenerator.getInstance("AES").apply { init(256) }.generateKey())
    @Test fun encryptsPrivateDataAndUsesFreshNonces() {
        val content = "token and incident evidence".toByteArray()
        val first = cipher.encrypt("account-1", content)
        val second = cipher.encrypt("account-1", content)
        assertFalse(first.contentEquals(second))
        assertFalse(first.toString(Charsets.UTF_8).contains("token"))
        assertArrayEquals(content, cipher.decrypt("account-1", first))
    }
    @Test fun rejectsTamperingAndCrossAccountDecryptionWithoutReturningPlaintext() {
        val encrypted = cipher.encrypt("account-1", byteArrayOf(1, 2, 3))
        assertThrows(Exception::class.java) { cipher.decrypt("account-2", encrypted) }
        encrypted[encrypted.lastIndex] = (encrypted.last().toInt() xor 1).toByte()
        assertThrows(Exception::class.java) { cipher.decrypt("account-1", encrypted) }
    }
}
