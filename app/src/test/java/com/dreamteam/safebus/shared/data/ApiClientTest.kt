package com.dreamteam.safebus.shared.data

import com.dreamteam.safebus.shared.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class ApiClientTest {
    @Test fun refusesInsecureRemoteUrlsAndCredentialBearingUrls() {
        assertTrue(ApiClient.validBaseUrl("https://api.example.com", false))
        assertTrue(ApiClient.validBaseUrl("http://10.0.2.2:8080", true))
        listOf("http://api.example.com", "https://user:secret@api.example.com", "https://api.example.com?q=token", "file:///private", "https://api.example.com/elsewhere").forEach {
            assertFalse(ApiClient.validBaseUrl(it, true))
        }
        assertFalse(ApiClient.validBaseUrl("http://10.0.2.2:8080", false))
    }
    @Test fun attachesBearerAndNeverDecodesOrPersistsHttpFailures() = runBlocking {
        for (status in listOf(401, 403, 404, 409, 422, 429, 500)) {
            var invalidated = false
            var persisted = false
            val client = ApiClient("https://api.example.com", false, { "private-token" }, { invalidated = true },
                HttpEngine { request -> assertEquals("private-token", request.token); assertFalse(request.toString().contains("private")); ApiResponse(status, byteArrayOf()) },
                Dispatchers.Unconfined, { "CODE" })
            val result = client.call("GET", "/api/v1/review-queue") { persisted = true; it.status }
            assertEquals(DomainResult.Error(Failure.fromStatus(status, "CODE")), result)
            assertEquals(status == 401, invalidated)
            assertFalse(persisted)
        }
    }
    @Test fun mapsNetworkErrorsAndRejectsResponsesFromASignedOutSession() = runBlocking {
        var token: String? = "token"
        var persisted = false
        val client = ApiClient("https://api.example.com", false, { token }, {}, HttpEngine {
            token = null; ApiResponse(200, byteArrayOf())
        }, Dispatchers.Unconfined)
        assertEquals(FailureKind.UNAUTHORIZED, (client.call("GET", "/api/v1/fleet/overview") { persisted = true } as DomainResult.Error).failure.kind)
        assertFalse(persisted)
        val offline = ApiClient("https://api.example.com", false, { "token" }, {}, HttpEngine { throw IOException() }, Dispatchers.Unconfined)
        assertEquals(FailureKind.NETWORK, (offline.call("GET", "/api/v1/fleet/overview") { persisted = true } as DomainResult.Error).failure.kind)
        assertFalse(persisted)
    }
    @Test fun sendsMultipartWithoutChangingEvidenceBytesOrFieldNames() {
        val photo = byteArrayOf(0, 1, 2, 3)
        val body = multipart(mapOf("id" to "original-id", "journeyId" to "7", "message" to "Incidente"), "photo", photo, "image/png")
        val text = body.bytes.toString(Charsets.ISO_8859_1)
        assertTrue(text.contains("name=\"journeyId\"\r\n\r\n7"))
        assertTrue(text.contains("name=\"photo\"; filename=\"image.png\""))
        assertTrue(text.contains(photo.toString(Charsets.ISO_8859_1)))
        assertFalse(body.toString().contains("Incidente"))
    }
}
