package com.wattagam.app.data

import android.content.Context
import com.wattagam.app.BuildConfig
import com.wattagam.app.R
import com.wattagam.app.data.api.ServerRepository
import com.wattagam.app.data.auth.KakaoLogin
import com.wattagam.app.data.auth.LocalProfile
import com.wattagam.app.data.auth.PrefsSessionStorage
import com.wattagam.app.data.sample.SampleRepository
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * 앱 전체가 함께 쓰는 저장소 하나 (로그인 정보와 토큰 갱신을 한곳에서 관리하려고).
 * 서버 주소와 카카오 앱 키가 둘 다 있어야 서버에 연결한다. 서버 로그인은 카카오 토큰으로만 할 수 있어서다.
 */
object AppRepository {
    @Volatile private var instance: WattagamRepository? = null

    val serverConfigured: Boolean get() = BuildConfig.WATTAGAM_API_URL.isNotBlank() && KakaoLogin.isConfigured

    fun get(context: Context): WattagamRepository = instance ?: synchronized(this) {
        instance ?: create(context.applicationContext).also { instance = it }
    }

    private fun create(context: Context): WattagamRepository =
        if (serverConfigured) {
            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                // 사진 업로드 (최대 5MB)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build()
            ServerRepository(
                BuildConfig.WATTAGAM_API_URL, client, PrefsSessionStorage(context),
                kakaoAccessToken = { KakaoLogin.currentAccessToken() },
                courseName = context.getString(R.string.campus_course),
            )
        } else {
            SampleRepository(LocalProfile(context))
        }
}
