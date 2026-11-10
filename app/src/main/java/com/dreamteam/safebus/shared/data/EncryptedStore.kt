package com.dreamteam.safebus.shared.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import com.dreamteam.safebus.shared.domain.SecureStore
import java.io.File
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class StoreCipher(private val key: SecretKey) {
    fun encrypt(name: String, value: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        cipher.updateAAD(name.toByteArray(Charsets.UTF_8))
        return cipher.iv + cipher.doFinal(value)
    }
    fun decrypt(name: String, value: ByteArray): ByteArray {
        require(value.size >= 28)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, value.copyOfRange(0, 12)))
        cipher.updateAAD(name.toByteArray(Charsets.UTF_8))
        return cipher.doFinal(value.copyOfRange(12, value.size))
    }
}

class EncryptedStore(context: Context) : SecureStore {
    private val directory = File(context.noBackupFilesDir, "protected-records").apply { mkdirs() }
    private val cipher by lazy {
        val keys = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val alias = "safebus.records.v1"
        val key = (keys.getKey(alias, null) as? SecretKey) ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256).build())
        }.generateKey()
        StoreCipher(key)
    }
    private fun file(key: String): AtomicFile {
        val hash = MessageDigest.getInstance("SHA-256").digest(key.toByteArray()).joinToString("") { "%02x".format(it) }
        return AtomicFile(File(directory, hash))
    }
    @Synchronized override fun read(key: String): ByteArray? {
        val file = file(key)
        if (!file.baseFile.exists()) return null
        return cipher.decrypt(key, file.readFully())
    }
    @Synchronized override fun write(key: String, value: ByteArray) {
        val encrypted = cipher.encrypt(key, value)
        val file = file(key)
        val stream = file.startWrite()
        try { stream.write(encrypted); file.finishWrite(stream) }
        catch (error: Exception) { file.failWrite(stream); throw error }
    }
    @Synchronized override fun remove(key: String) { file(key).delete() }
}
