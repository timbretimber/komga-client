package snd.komga.client

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import snd.komga.client.sse.KomgaSSESession
import snd.komga.client.sse.OkHttpKomgaSseSession
import java.util.concurrent.TimeUnit

internal actual suspend fun getSseSession(
    json: Json,
    baseUrl: String,
    username: String?,
    password: String?,
    apiKey: String?,
    useragent: String?,
    authCookie: String?
): KomgaSSESession {
    return withContext(Dispatchers.IO) {
        val client = OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.SECONDS)
            .build()
        val session = OkHttpKomgaSseSession(
            client = client,
            json = json,
            baseUrl = baseUrl.toHttpUrl(),
            username = username,
            password = password,
            apiKey = apiKey,
            useragent = useragent,
            authCookie = authCookie
        )
        session.connect()

        session
    }
}
