package com.nhviewer.core.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.nhviewer.data.remote.NhentaiService
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

object NetworkClientFactory {
    private const val BASE_URL = "https://nhentai.net"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val original = chain.request()
                val builder: Request.Builder = original.newBuilder()
                    .header("User-Agent", "NhViewer/0.1 (codex-local-build)")

                val key = NetworkAuthState.apiKey?.trim().orEmpty()
                if (key.isNotBlank()) {
                    builder.header("Authorization", "Key $key")
                }
                chain.proceed(builder.build())
            }
            .addInterceptor(logging)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    fun createNhentaiService(): NhentaiService = retrofit.create(NhentaiService::class.java)
}
