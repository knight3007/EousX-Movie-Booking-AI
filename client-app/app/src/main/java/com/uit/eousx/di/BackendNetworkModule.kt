package com.uit.eousx.di

import com.squareup.moshi.Moshi
import com.uit.eousx.BuildConfig
import com.uit.eousx.core.network.AuthInterceptor
import com.uit.eousx.data.remote.api.BackendApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object BackendNetworkModule {

    private const val BACKEND_BASE_URL = "http://10.0.2.2:3000/"

    @Provides
    @Singleton
    @BackendOkHttpClient
    fun provideBackendOkHttpClient(
        authInterceptor: AuthInterceptor
    ): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @BackendRetrofit
    fun provideBackendRetrofit(
        @BackendOkHttpClient okHttpClient: OkHttpClient,
        moshi: Moshi
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BACKEND_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    @Provides
    @Singleton
    fun provideBackendApi(
        @BackendRetrofit retrofit: Retrofit
    ): BackendApi {
        return retrofit.create(BackendApi::class.java)
    }
}
