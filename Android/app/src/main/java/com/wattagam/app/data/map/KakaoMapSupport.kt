package com.wattagam.app.data.map

import android.content.Context
import android.os.Build
import com.kakao.vectormap.KakaoMapSdk
import com.wattagam.app.BuildConfig

/**
 * 카카오맵 SDK 사용 가능 여부. 앱 키(local.properties 의 KAKAO_NATIVE_APP_KEY)가 있고 SDK 를 실제로 불러왔을 때만 실제 지도를 쓴다.
 * SDK 는 armeabi-v7a·arm64-v8a 만 지원한다. x86 에뮬레이터는 ARM 번역을 지원한다고 알려도 앱의 기본 ABI 가 x86 이라
 * SDK 라이브러리가 설치되지 않으므로, 기본 ABI 로 판단하고 불러오기에 실패해도 안내 지도 그림으로 대신한다.
 */
object KakaoMapSupport {
    var isAvailable: Boolean = false
        private set

    fun init(context: Context) {
        val primaryAbiIsArm = Build.SUPPORTED_ABIS.firstOrNull()?.startsWith("arm") == true
        if (BuildConfig.KAKAO_NATIVE_APP_KEY.isBlank() || !primaryAbiIsArm) return
        isAvailable = try {
            KakaoMapSdk.init(context, BuildConfig.KAKAO_NATIVE_APP_KEY)
            true
        } catch (_: UnsatisfiedLinkError) {
            false
        }
    }
}
