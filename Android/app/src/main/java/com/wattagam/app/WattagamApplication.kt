package com.wattagam.app

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.wattagam.app.data.AppRepository
import com.wattagam.app.data.auth.KakaoLogin
import com.wattagam.app.data.map.KakaoMapSupport

class WattagamApplication : Application(), SingletonImageLoader.Factory {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) DebugCrashLog.install(this)
        KakaoLogin.init(this)
        KakaoMapSupport.init(this)
    }

    /** 앱 전체 사진 로더. 방명록 사진 요청에 로그인 토큰을 붙인다 */
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(OkHttpNetworkFetcherFactory(callFactory = { AppRepository.imageClient(this) })) }
            .build()
}
