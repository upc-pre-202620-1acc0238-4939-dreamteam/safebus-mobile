package com.dreamteam.safebus.safetycase.data

import com.dreamteam.safebus.shared.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import java.net.HttpURLConnection
import java.net.URI
import java.util.concurrent.atomic.AtomicReference

sealed interface LiveEvent {
    data object Connected : LiveEvent
    data object Changed : LiveEvent
    data class Disconnected(val failure: Failure) : LiveEvent
}

class EventParser {
    private var event = ""
    fun accept(line: String): Boolean {
        require(line.length <= 8192)
        if (line.isEmpty()) {
            val recognized = event in setOf("EmergencyActivated", "PassengerReviewRequired")
            event = ""
            return recognized
        }
        if (line.startsWith("event:")) event = line.substringAfter(':').trim()
        return false
    }
}

class SupervisorEvents(private val baseUrl: String, private val token: () -> String?, private val unauthorized: (String) -> Unit) {
    fun observe() = callbackFlow {
        val current = AtomicReference<HttpURLConnection?>(null)
        val worker = launch(Dispatchers.IO) {
            while (isActive) {
                val credential = token() ?: break
                try {
                    val connection = URI("${baseUrl.trimEnd('/')}/api/v1/notifications/stream").toURL().openConnection() as HttpURLConnection
                    current.set(connection)
                    connection.connectTimeout = 10_000; connection.readTimeout = 25_000
                    connection.instanceFollowRedirects = false; connection.useCaches = false
                    connection.setRequestProperty("Accept", "text/event-stream")
                    connection.setRequestProperty("Authorization", "Bearer $credential")
                    val status = connection.responseCode
                    if (status != 200) {
                        if (status == 401) unauthorized(credential)
                        trySend(LiveEvent.Disconnected(Failure.fromStatus(status)))
                        if (status in setOf(401, 403)) break
                    } else {
                        trySend(LiveEvent.Connected)
                        val parser = EventParser()
                        connection.inputStream.bufferedReader().use { reader ->
                            while (isActive && token() == credential) {
                                val line = reader.readLine() ?: break
                                if (parser.accept(line)) trySend(LiveEvent.Changed)
                            }
                        }
                    }
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { trySend(LiveEvent.Disconnected(Failure(FailureKind.NETWORK))) }
                finally { current.getAndSet(null)?.disconnect() }
                if (isActive) { trySend(LiveEvent.Disconnected(Failure(FailureKind.NETWORK))); delay(5_000) }
            }
        }
        awaitClose { worker.cancel(); current.getAndSet(null)?.disconnect() }
    }
}
