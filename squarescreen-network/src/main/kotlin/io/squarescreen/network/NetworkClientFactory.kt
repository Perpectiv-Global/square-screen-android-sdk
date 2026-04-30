package io.squarescreen.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import io.squarescreen.core.config.SquareScreenConfig
import io.squarescreen.core.datasource.NetworkDataSource
import io.squarescreen.network.api.SquareScreenApiService
import io.squarescreen.network.interceptor.DeviceAuthInterceptor
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

/**
 * Internal factory — creates and wires the network stack.
 * Not exposed to integrators.
 */
internal object NetworkClientFactory {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    fun create(config: SquareScreenConfig): NetworkDataSource {
        val okHttpClient = buildOkHttpClient(config)
        val retrofit = buildRetrofit(BuildConfig.BASE_URL, okHttpClient)
        val api = retrofit.create(SquareScreenApiService::class.java)
        return NetworkDataSourceImpl(api)
    }

    private fun buildOkHttpClient(config: SquareScreenConfig): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(DeviceAuthInterceptor(config.deviceId, config.deviceToken))
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private fun buildRetrofit(baseUrl: String, okHttpClient: OkHttpClient): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(baseUrl.trimEnd('/') + "/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }
}
