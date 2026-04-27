package io.squarescreen.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Automatically injects device authentication headers on every outgoing request.
 * Developers never set these headers manually — they are configured once at SDK init.
 */
internal class DeviceAuthInterceptor(
    private val deviceId: String,
    private val deviceToken: String
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header("X-Device-Id", deviceId)
            .header("X-Device-Token", deviceToken)
            .header("Accept", "application/json")
            .build()
        return chain.proceed(request)
    }
}
