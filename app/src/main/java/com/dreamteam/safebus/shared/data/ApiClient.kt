package com.dreamteam.safebus.shared.data

import com.dreamteam.safebus.shared.domain.DomainResult
import com.dreamteam.safebus.shared.domain.Failure
import com.dreamteam.safebus.shared.domain.FailureKind
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.util.UUID

data class ApiRequest(val method: String, val url: String, val token: String?, val body: ByteArray? = null, val contentType: String? = null, val countingKey: String? = null) {
    override fun toString(): String = "ApiRequest(method=$method)"
}
data class ApiResponse(val status: Int, val bytes: ByteArray, val contentType: String = "application/json")
fun interface HttpEngine { fun execute(request: ApiRequest): ApiResponse }

class UrlConnectionEngine : HttpEngine {
    override fun execute(request: ApiRequest): ApiResponse {
        val connection = URI(request.url).toURL().openConnection() as HttpURLConnection
        try {
            connection.requestMethod = request.method
            connection.connectTimeout = 10_000
            connection.readTimeout = 20_000
            connection.instanceFollowRedirects = false
            connection.useCaches = false
            connection.setRequestProperty("Accept", "application/json, image/jpeg, image/png")
            request.token?.let { connection.setRequestProperty("Authorization", "Bearer $it") }
            request.countingKey?.let { connection.setRequestProperty("X-Counting-Source-Key", it) }
            request.body?.let { bytes ->
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", request.contentType)
                connection.setFixedLengthStreamingMode(bytes.size)
                connection.outputStream.use { it.write(bytes) }
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    if (output.size() + count > 7 * 1024 * 1024) throw IOException("Response too large")
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            } ?: byteArrayOf()
            return ApiResponse(status, body, connection.contentType.orEmpty())
        } finally { connection.disconnect() }
    }
}

class ApiClient(
    val baseUrl: String,
    allowLocalHttp: Boolean,
    private val token: () -> String?,
    private val unauthorized: (String) -> Unit,
    private val engine: HttpEngine = UrlConnectionEngine(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val errorCode: (ByteArray) -> String = { runCatching { JSONObject(it.toString(Charsets.UTF_8)).optString("code") }.getOrDefault("") },
    private val deviceAuthentication: Boolean = false,
) {
    init { require(validBaseUrl(baseUrl, allowLocalHttp)) }

    suspend fun <T> call(method: String, path: String, body: ByteArray? = null, contentType: String? = null,
        authenticated: Boolean = true, decode: (ApiResponse) -> T): DomainResult<T> = withContext(dispatcher) {
        if (!path.startsWith("/api/v1/") || path.contains("..") || path.contains('?') || path.contains('#')) {
            return@withContext DomainResult.Error(Failure(FailureKind.VALIDATION, "INVALID_ENDPOINT"))
        }
        val credential = if (authenticated) token() else null
        if (deviceAuthentication && (method != "POST" || path !in setOf("/api/v1/occupancy-initializations", "/api/v1/occupancy-heartbeats", "/api/v1/occupancy-events"))) {
            return@withContext DomainResult.Error(Failure(FailureKind.FORBIDDEN))
        }
        if (authenticated && credential == null) return@withContext DomainResult.Error(Failure(FailureKind.UNAUTHORIZED))
        if (credential?.any { it == '\r' || it == '\n' } == true) return@withContext DomainResult.Error(Failure(FailureKind.VALIDATION))
        try {
            val response = engine.execute(ApiRequest(method, baseUrl.trimEnd('/') + path, if (deviceAuthentication) null else credential, body, contentType,
                if (deviceAuthentication) credential else null))
            if (authenticated && token() != credential) return@withContext DomainResult.Error(Failure(FailureKind.UNAUTHORIZED))
            if (response.status in 200..299) DomainResult.Success(decode(response))
            else {
                if (response.status == 401 && credential != null) unauthorized(credential)
                DomainResult.Error(Failure.fromStatus(response.status, errorCode(response.bytes)))
            }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: IOException) { DomainResult.Error(Failure(FailureKind.NETWORK)) }
        catch (_: Exception) { DomainResult.Error(Failure(FailureKind.INVALID_RESPONSE)) }
    }

    companion object {
        fun validBaseUrl(value: String, allowLocalHttp: Boolean): Boolean = runCatching {
            val uri = URI(value)
            val secure = uri.scheme == "https" || (allowLocalHttp && uri.scheme == "http" && uri.host in setOf("10.0.2.2", "127.0.0.1", "localhost"))
            secure && !uri.host.isNullOrBlank() && uri.userInfo == null && uri.query == null && uri.fragment == null && uri.path.orEmpty() in setOf("", "/")
        }.getOrDefault(false)
    }
}

data class UploadBody(val bytes: ByteArray, val contentType: String) {
    override fun toString(): String = "UploadBody(redacted)"
}

fun multipart(fields: Map<String, String>, part: String, photo: ByteArray, mime: String): UploadBody {
    require(mime in setOf("image/jpeg", "image/png"))
    require((fields.keys + part).all { it.matches(Regex("[a-zA-Z][a-zA-Z0-9]*")) })
    val boundary = "SafeBus${UUID.randomUUID()}"
    val output = java.io.ByteArrayOutputStream()
    fun write(value: String) { output.write(value.toByteArray(Charsets.UTF_8)) }
    fields.forEach { (name, value) ->
        write("--$boundary\r\nContent-Disposition: form-data; name=\"$name\"\r\n\r\n$value\r\n")
    }
    write("--$boundary\r\nContent-Disposition: form-data; name=\"$part\"; filename=\"image.${if (mime == "image/png") "png" else "jpg"}\"\r\nContent-Type: $mime\r\n\r\n")
    output.write(photo)
    write("\r\n--$boundary--\r\n")
    return UploadBody(output.toByteArray(), "multipart/form-data; boundary=$boundary")
}

fun jsonBody(vararg values: Pair<String, Any?>): ByteArray = JSONObject().apply {
    values.forEach { (key, value) -> put(key, value ?: JSONObject.NULL) }
}.toString().toByteArray(Charsets.UTF_8)

fun ApiResponse.json(): JSONObject = JSONObject(bytes.toString(Charsets.UTF_8))
fun JSONObject.optionalString(key: String): String? = if (isNull(key) || !has(key)) null else getString(key)
fun JSONObject.optionalLong(key: String): Long? = if (isNull(key) || !has(key)) null else getLong(key)
fun JSONObject.optionalInt(key: String): Int? = if (isNull(key) || !has(key)) null else getInt(key)
fun JSONObject.optionalDouble(key: String): Double? = if (isNull(key) || !has(key)) null else getDouble(key)
fun JSONObject.objects(key: String): List<JSONObject> = getJSONArray(key).let { array -> (0 until array.length()).map(array::getJSONObject) }
