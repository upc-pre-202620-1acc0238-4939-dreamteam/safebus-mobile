package com.dreamteam.safebus.shared.data

import android.content.ContentResolver
import android.graphics.BitmapFactory
import android.net.Uri
import com.dreamteam.safebus.shared.domain.*
import kotlinx.coroutines.*

object PhotoLoader {
    fun acceptableDimensions(width: Int, height: Int) = width > 0 && height > 0 && width.toLong() * height <= 25_000_000
    fun decode(bytes: ByteArray): Photo {
        require(bytes.isNotEmpty() && bytes.size <= 5 * 1024 * 1024)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        require(acceptableDimensions(bounds.outWidth, bounds.outHeight))
        val photo = Photo(bytes, bounds.outMimeType.orEmpty())
        val options = BitmapFactory.Options().apply { inSampleSize = 8 }
        val image = requireNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options))
        image.recycle()
        return photo
    }
    suspend fun load(resolver: ContentResolver, uri: Uri): DomainResult<Photo> = withContext(Dispatchers.IO) {
        try {
            val bytes = requireNotNull(resolver.openInputStream(uri)).use { input ->
                val buffer = java.io.ByteArrayOutputStream()
                val chunk = ByteArray(8192)
                while (true) {
                    val read = input.read(chunk)
                    if (read < 0) break
                    require(buffer.size() + read <= 5 * 1024 * 1024)
                    buffer.write(chunk, 0, read)
                }
                buffer.toByteArray()
            }
            DomainResult.Success(decode(bytes))
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { invalid("PHOTO_INVALID") }
    }
}
