package snd.komga.client.user

import snd.komga.client.common.KomgaPageRequest
import snd.komga.client.common.Page

interface KomgaUserClient {

    suspend fun logout()
    suspend fun getMe(): KomgaUser
    suspend fun getMe(username: String, password: String, rememberMe: Boolean): KomgaUser
    suspend fun getMe(apiKey: String): KomgaUser
    suspend fun updateMyPassword(newPassword: String)
    suspend fun getAllUsers(): List<KomgaUser>
    suspend fun addUser(user: KomgaUserCreateRequest): KomgaUser
    suspend fun deleteUser(userId: KomgaUserId)
    suspend fun updateUser(userId: KomgaUserId, request: KomgaUserUpdateRequest)
    suspend fun updatePassword(userId: KomgaUserId, password: String)
    suspend fun getMeAuthenticationActivity(
        pageRequest: KomgaPageRequest? = null,
        unpaged: Boolean = false,
    ): Page<KomgaAuthenticationActivity>

    suspend fun getAuthenticationActivity(
        pageRequest: KomgaPageRequest? = null,
        unpaged: Boolean = false,
    ): Page<KomgaAuthenticationActivity>

    suspend fun getLatestAuthenticationActivityForUser(userId: KomgaUserId): KomgaAuthenticationActivity

}