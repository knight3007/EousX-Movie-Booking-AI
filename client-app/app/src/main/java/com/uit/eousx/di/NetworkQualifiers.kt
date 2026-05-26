package com.uit.eousx.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class BackendRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class BackendOkHttpClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class TmdbRetrofit
