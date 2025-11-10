package snd.komga.client.sse

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.Credentials
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import okio.withLock
import java.util.concurrent.locks.ReentrantLock

private val logger = KotlinLogging.logger {}

class OkHttpKomgaSseSession(
    private val client: OkHttpClient,
    private val json: Json,
    private val baseUrl: HttpUrl,
    private val username: String?,
    private val password: String?,
    private val apiKey: String?,
    private val authCookie: String?,
    private val useragent: String?,
) : KomgaSSESession, EventSourceListener() {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob() +
            CoroutineExceptionHandler { _, exception -> logger.catching(exception) })
    override val incoming = MutableSharedFlow<KomgaEvent>(extraBufferCapacity = 100)
    private var serverSentEventsSource: EventSource? = null
    private val connectionLock = ReentrantLock()
    private var isActive: Boolean = false

    fun connect() {
        connectionLock.withLock {
            isActive = true
            serverSentEventsSource = getSseConnection()
        }
    }

    override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
        incoming.tryEmit(json.toKomgaEvent(type, data))
    }

    override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
        if (isActive) {
            scope.launch {
                val responseCode = response?.code?.let { HttpStatusCode.fromValue(it) }?.toString()
                logger.warn(t) { "Event source error; response code $responseCode" }
                delay(10000)
                reconnect()
            }
        }
    }

    override fun onClosed(eventSource: EventSource) {
        logger.debug { "Komga event source closed" }
        scope.launch { reconnect() }
    }

    override fun onOpen(eventSource: EventSource, response: Response) {
        logger.info { "connected to Komga event source: ${baseUrl.newBuilder().addPathSegments("sse/v1/events")}" }
    }

    private fun reconnect() {
        connectionLock.withLock {
            if (isActive) {
                serverSentEventsSource?.cancel()
                serverSentEventsSource = getSseConnection()
            }
        }
    }

    override fun cancel() {
        connectionLock.withLock {
            isActive = false
            scope.cancel()
            serverSentEventsSource?.cancel()
        }
    }

    private fun getSseConnection(): EventSource {
        val request = Request.Builder()
            .url(baseUrl.newBuilder().addPathSegments("sse/v1/events").build())
        authCookie?.let { request.header("Cookie", authCookie) }
        if (apiKey != null){
            request.header("X-API-Key", apiKey)
        } else if (username != null && password != null) {
            request.header("Authorization", Credentials.basic(username, password))
        }
        if (useragent != null) {
            request.header("User-Agent", useragent)
        }

        return EventSources.createFactory(client).newEventSource(request.build(), this)
    }
}
