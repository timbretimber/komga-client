package snd.komga.client.user

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.util.*
import snd.komga.client.common.KomgaPageRequest
import snd.komga.client.common.Page
import snd.komga.client.common.toParams

class HttpUserClient(private val ktor: HttpClient) : KomgaUserClient {

    override suspend fun logout() {
        ktor.post("api/logout")
    }

    override suspend fun getMe(): KomgaUser {
        return ktor.get("api/v2/users/me").body()
    }

    override suspend fun getMe(username: String, password: String, rememberMe: Boolean): KomgaUser {
        val usernameAndPassword = "$username:$password"
        val encoded = usernameAndPassword.encodeBase64()

        return ktor.get("api/v2/users/me") {
            header("Authorization", "Basic $encoded")
            header("Cache-Control", "no-cache, no-store, max-age=0")
            parameter("remember-me", rememberMe)
        }.body()
    }

    override suspend fun getMe(apiKey: String): Komgauser {
        return ktor.get("api/v2/users/me") {
            header("X-API-Key", apiKey)
            header("Cache-Control", "no-cache, no-store, max-age=0")
        }.body()
    }

    override suspend fun updateMyPassword(newPassword: String) {
        ktor.patch("api/v2/users/me/password") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("password" to newPassword))
        }
    }

    override suspend fun getAllUsers(): List<KomgaUser> {
        return ktor.get("api/v2/users").body()
    }

    override suspend fun addUser(user: KomgaUserCreateRequest): KomgaUser {
        return ktor.post("api/v2/users") {
            contentType(ContentType.Application.Json)
            setBody(user)
        }.body()
    }

    override suspend fun deleteUser(userId: KomgaUserId) {
        ktor.delete("api/v2/users/$userId")
    }

    override suspend fun updateUser(userId: KomgaUserId, request: KomgaUserUpdateRequest) {
        ktor.patch("api/v2/users/$userId") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    override suspend fun updatePassword(userId: KomgaUserId, password: String) {
        ktor.patch("api/v2/users/$userId/password") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("password" to password))
        }
    }

    override suspend fun getMeAuthenticationActivity(
        pageRequest: KomgaPageRequest?,
        unpaged: Boolean,
    ): Page<KomgaAuthenticationActivity> {
        return ktor.get("api/v2/users/me/authentication-activity") {
            pageRequest?.let { url.parameters.appendAll(it.toParams()) }
            url.parameters.append("unpaged", unpaged.toString())
        }.body()
    }

    override suspend fun getAuthenticationActivity(
        pageRequest: KomgaPageRequest?,
        unpaged: Boolean,
    ): Page<KomgaAuthenticationActivity> {
        return ktor.get("api/v2/users/authentication-activity") {
            pageRequest?.let { url.parameters.appendAll(it.toParams()) }
            url.parameters.append("unpaged", unpaged.toString())
        }.body()
    }

    override suspend fun getLatestAuthenticationActivityForUser(userId: KomgaUserId): KomgaAuthenticationActivity {
        return ktor.get("api/v2/users/$userId/authentication-activity/latest").body()
    }

}