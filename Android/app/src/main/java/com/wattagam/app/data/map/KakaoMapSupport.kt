package com.wattagam.app.data.map

import android.content.Context
import android.os.Build
import com.kakao.vectormap.KakaoMapSdk
import com.wattagam.app.BuildConfig

/**
 * 카카오맵 SDK 사용 가능 여부. 앱 키(local.properties 의 KAKAO_NATIVE_APP_KEY)가 있고
 * ARM 기기일 때만 실제 지도를 쓴다. SDK 가 armeabi-v7a·arm64-v8a 만 지원해서 x86 에뮬레이터에서는 안내 지도 그림으로 대신한다.
 */
object KakaoMapSupport {
    val isAvailable: Boolean
        get() = BuildConfig.KAKAO_NATIVE_APP_KEY.isNotBlank() && Build.SUPPORTED_ABIS.any { it.startsWith("arm") }

    fun init(context: Context) {
        if (isAvailable) KakaoMapSdk.init(context, BuildConfig.KAKAO_NATIVE_APP_KEY)
    }
}
