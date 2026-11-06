package com.wattagam.app

import android.app.Application
import com.wattagam.app.data.auth.KakaoLogin
import com.wattagam.app.data.map.KakaoMapSupport

class WattagamApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        KakaoLogin.init(this)
        KakaoMapSupport.init(this)
    }
}
