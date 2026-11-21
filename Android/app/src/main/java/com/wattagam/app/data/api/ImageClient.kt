package com.wattagam.app.data.api

import com.wattagam.app.data.auth.SessionStorage
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * 방명록 사진을 받는 연결. 서버는 로그인 토큰을 확인한 뒤에만 사진을 보내 준다 (GET /api/guestbook/{id}/image).
 * 토큰은 왔다감 서버(주소·포트가 같은 곳)로 가는 요청에만 붙이고, 다른 주소로는 보내지 않는다.
 */
fun authorizedImageClient(apiBaseUrl: String, session: SessionStorage): OkHttpClient {
    val server = apiBaseUrl.toHttpUrlOrNull()
    return OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request()
            val token = session.load()?.accessToken
            if (token != null && server != null && request.url.isSameServer(server)) {
                chain.proceed(request.newBuilder().header("Authorization", "Bearer $token").build())
            } else {
                chain.proceed(request)
            }
        }
        .build()
}

private fun HttpUrl.isSameServer(other: HttpUrl) = scheme == other.scheme && host == other.host && port == other.port
