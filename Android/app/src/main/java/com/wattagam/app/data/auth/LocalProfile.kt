package com.wattagam.app.data.auth

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 이 기기에 저장한 닉네임. 계정 API 가 나오기 전까지 자동 로그인 후 닉네임 화면을 건너뛰는 데만 쓴다.
 * 서버 계정 정보가 생기면 서버 값으로 대체하고 이 저장소는 지운다.
 */
class LocalProfile(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("profile", Context.MODE_PRIVATE)

    suspend fun nickname(): String? = withContext(Dispatchers.IO) { prefs.getString(KEY_NICKNAME, null) }

    suspend fun saveNickname(nickname: String) = withContext(Dispatchers.IO) {
        prefs.edit(commit = true) { putString(KEY_NICKNAME, nickname) }
    }

    suspend fun clear() = withContext(Dispatchers.IO) { prefs.edit(commit = true) { clear() } }

    private companion object {
        const val KEY_NICKNAME = "nickname"
    }
}
