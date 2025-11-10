package snd.komga.client

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.w3c.dom.EventSource
import org.w3c.dom.EventSourceInit
import org.w3c.dom.MessageEvent
import snd.komga.client.sse.KomgaEvent
import snd.komga.client.sse.KomgaSSESession
import snd.komga.client.sse.toKomgaEvent

internal actual suspend fun getSseSession(
    json: Json,
    baseUrl: String,
    username: String?,
    password: String?,
    apiKey: String?,
    useragent: String?,
    authCookie: String?
): KomgaSSESession {
    return JsKomgaSseSession(json, baseUrl)
}

class JsKomgaSseSession(
    private val json: Json,
    baseUrl: String,
) : KomgaSSESession {
    private val scope = CoroutineScope(Dispatchers.Default)
    override val incoming = MutableSharedFlow<KomgaEvent>(extraBufferCapacity = 100)
    private val eventSource: EventSource

    init {
        val eventSource = EventSource("$baseUrl/sse/v1/events", EventSourceInit(withCredentials = true))

        eventSource.addEventListener("TaskQueueStatus") { handleEvent(it as MessageEvent) }
        eventSource.addEventListener("LibraryAdded") { handleEvent(it as MessageEvent) }
        eventSource.addEventListener("LibraryChanged") { handleEvent(it as MessageEvent) }
        eventSource.addEventListener("LibraryDeleted") { handleEvent(it as MessageEvent) }
        eventSource.addEventListener("SeriesAdded") { handleEvent(it as MessageEvent) }
        eventSource.addEventListener("SeriesChanged") { handleEvent(it as MessageEvent) }
        eventSource.addEventListener("SeriesDeleted") { handleEvent(it as MessageEvent) }
        eventSource.addEventListener("BookAdded") { handleEvent(it as MessageEvent) }
        eventSource.addEventListener("BookChanged") { handleEvent(it as MessageEvent) }
        eventSource.addEventListener("BookDeleted") { handleEvent(it as MessageEvent) }
        eventSource.addEventListener("BookImported") { handleEvent(it as MessageEvent) }
        eventSource.addEventListener("ReadListAdded") { handleEvent(it as MessageEvent) }
        eventSource.addEventListener("ReadListChanged") { handleEvent(it as MessageEvent) }
        eventSource.addEventListener("ReadListDeleted") { handleEvent(it as MessageEvent) }
        eventSource.addEventListener("CollectionAdded") { handleEvent(it as MessageEvent) }
        eventSource.addEventListener("CollectionChanged") { handleEvent(it as MessageEvent) }
        eventSource.addEventListener("CollectionDeleted") { handleEvent(it as MessageEvent) }
        eventSource.addEventListener("ReadProgressChanged") { handleEvent(it as MessageEvent) }
        eventSource.addEventListener("ReadProgressDeleted") { handleEvent(it as MessageEvent) }
        eventSource.addEventListener("ReadProgressSeriesChanged") { handleEvent(it as MessageEvent) }
        eventSource.addEventListener("ReadProgressSeriesDeleted") { handleEvent(it as MessageEvent) }
        eventSource.addEventListener("ThumbnailBookAdded") { handleEvent(it as MessageEvent) }
        eventSource.addEventListener("ThumbnailBookDeleted") { handleEvent(it as MessageEvent) }
        eventSource.addEventListener("ThumbnailSeriesAdded") { handleEvent(it as MessageEvent) }
        eventSource.addEventListener("ThumbnailSeriesDeleted") { handleEvent(it as MessageEvent) }
        eventSource.addEventListener("ThumbnailSeriesCollectionAdded") { handleEvent(it as MessageEvent) }
        eventSource.addEventListener("ThumbnailSeriesCollectionDeleted") { handleEvent(it as MessageEvent) }

        eventSource.addEventListener("ThumbnailReadListAdded") { handleEvent(it as MessageEvent) }
        eventSource.addEventListener("ThumbnailReadListDeleted") { handleEvent(it as MessageEvent) }
        eventSource.addEventListener("SessionExpired") { handleEvent(it as MessageEvent) }

        this.eventSource = eventSource
    }


    override fun cancel() {
        eventSource.close()
    }

    private fun handleEvent(event: MessageEvent) {
        scope.launch { incoming.emit(json.toKomgaEvent(event.type, event.data?.toString())) }
    }
}