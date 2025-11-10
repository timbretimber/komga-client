package snd.komga.client

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.cookies.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import snd.komga.client.actuator.HttpActuatorClient
import snd.komga.client.announcements.HttpAnnouncementsClient
import snd.komga.client.book.HttpBookClient
import snd.komga.client.collection.HttpCollectionClient
import snd.komga.client.filesystem.HttpFileSystemClient
import snd.komga.client.library.HttpLibraryClient
import snd.komga.client.readlist.HttpReadListClient
import snd.komga.client.referential.HttpReferentialClient
import snd.komga.client.series.HttpSeriesClient
import snd.komga.client.settings.HttpSettingsClient
import snd.komga.client.sse.KomgaSSESession
import snd.komga.client.task.HttpTaskClient
import snd.komga.client.user.HttpUserClient

class KomgaClientFactory private constructor(
    private val builder: Builder
) {

    private val json = Json(builder.json) {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    private val baseUrl: () -> URLBuilder = builder.baseUrl
    private val ktor: HttpClient = configureKtor(builder.ktor ?: HttpClient())

    private fun configureKtor(client: HttpClient): HttpClient {
        return client.config {
            val cookiesStorage = builder.cookieStorage
            if (cookiesStorage != null) {
                install(HttpCookies) { storage = cookiesStorage }
            }

            install(ContentNegotiation) { json(json) }
            defaultRequest {
                url {
                    this.takeFrom(baseUrl())
                    this.pathSegments = this.pathSegments.filter { it.isNotBlank() } + "" // always add trailing slash
                }
            }

            val username = builder.username
            val password = builder.password
            val useragent = builder.useragent
            val apiKey = builder.apiKey
        if (apiKey != null) {
            install(DefaultRequest) {
                header("X-API-Key", apiKey)
            }
        } else if (username != null && password != null) {
            install(Auth) {
                basic {
                    credentials { BasicAuthCredentials(username = username, password = password) }
                    sendWithoutRequest { true }
                }
            }
            }

            if (useragent != null) {
                install(UserAgent) { agent = useragent }
            }

            expectSuccess = true
        }
    }

    fun libraryClient() = HttpLibraryClient(ktor)
    fun seriesClient() = HttpSeriesClient(ktor)
    fun bookClient() = HttpBookClient(ktor)
    fun userClient() = HttpUserClient(ktor)
    fun fileSystemClient() = HttpFileSystemClient(ktor)
    fun settingsClient() = HttpSettingsClient(ktor)
    fun taskClient() = HttpTaskClient(ktor)
    fun actuatorClient() = HttpActuatorClient(ktor)
    fun announcementClient() = HttpAnnouncementsClient(ktor)
    fun collectionClient() = HttpCollectionClient(ktor)
    fun readListClient() = HttpReadListClient(ktor)
    fun referentialClient() = HttpReferentialClient(ktor)
    fun ktor() = ktor

    //FIXME ktor sse session implementation does NOT terminate sse connection on session context cancellation
    // using custom implementation as a workaround
    suspend fun sseSession(): KomgaSSESession {
        val authCookie = ktor.cookies(Url(baseUrl()))
            .find { it.name == "SESSION" || it.name == "KOMGA-SESSION" }
            ?.let { renderCookieHeader(it) }

        return getSseSession(
            json = json,
            baseUrl = baseUrl().buildString(),
            username = builder.username,
            password = builder.password,
            apiKey = builder.apiKey,
            useragent = builder.useragent,
            authCookie = authCookie
        )
    }

    class Builder {
        internal var ktor: HttpClient? = null
        internal var baseUrl: () -> URLBuilder = { URLBuilder("http://localhost:25600/") }
        internal var cookieStorage: CookiesStorage? = null
        internal var json: Json = Json

        internal var username: String? = null
        internal var password: String? = null
        internal var apiKey: String? = null
        internal var useragent: String? = null

        fun ktor(ktor: HttpClient) = apply {
            this.ktor = ktor
        }

        fun baseUrl(block: () -> String) = apply {
            this.baseUrl = { URLBuilder(block()) }
        }

        fun baseUrlBuilder(block: () -> URLBuilder) = apply {
            this.baseUrl = block
        }

        fun cookieStorage(cookiesStorage: CookiesStorage) = apply {
            this.cookieStorage = cookiesStorage
        }

        fun username(username: String) = apply {
            this.username = username
        }

        fun useragent(useragent: String) = apply {
            this.useragent = useragent
        }

        fun password(password: String) = apply {
            this.password = password
        }

        fun apiKey(apiKey: String) = apply {
            this.apiKey = apiKey
        }

        fun build(): KomgaClientFactory {
            return KomgaClientFactory(this)
        }

    }
}

internal expect suspend fun getSseSession(
    json: Json,
    baseUrl: String,
    username: String?,
    password: String?,
    apiKey: String?,
    useragent: String?,
    authCookie: String?
): KomgaSSESession
