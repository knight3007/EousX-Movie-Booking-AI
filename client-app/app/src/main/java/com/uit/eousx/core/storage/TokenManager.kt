package com.uit.eousx.core.storage

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.uit.eousx.core.dispatcher.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.tokenDataStore by preferencesDataStore(name = "eousx_auth_tokens")

@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher ioDispatcher: CoroutineDispatcher
) {
    private val accessTokenKey = stringPreferencesKey("access_token")
    @Volatile
    private var cachedAccessToken: String? = null

    init {
        CoroutineScope(SupervisorJob() + ioDispatcher).launch {
            getAccessToken().collect { token ->
                cachedAccessToken = token
            }
        }
    }

    suspend fun saveAccessToken(token: String) {
        cachedAccessToken = token
        context.tokenDataStore.edit { preferences ->
            preferences[accessTokenKey] = token
        }
    }

    fun getAccessToken(): Flow<String?> {
        return context.tokenDataStore.data.map { preferences ->
            preferences[accessTokenKey].also { token ->
                cachedAccessToken = token
            }
        }
    }

    fun getAccessTokenOnce(): String? = cachedAccessToken

    suspend fun clearToken() {
        cachedAccessToken = null
        context.tokenDataStore.edit { preferences ->
            preferences.remove(accessTokenKey)
        }
    }
}
