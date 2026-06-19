package io.squarescreen.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response

internal class DemoBypassInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header("X-Demo-Bypass", "true")
            .build()
        return chain.proceed(request)
    }
}
