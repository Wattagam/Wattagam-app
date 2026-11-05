package com.wattagam.app

import android.app.Application
import com.wattagam.app.data.auth.KakaoLogin

class WattagamApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        KakaoLogin.init(this)
    }
}
