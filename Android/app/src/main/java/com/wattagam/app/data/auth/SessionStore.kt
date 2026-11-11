package com.wattagam.app.data.auth

import android.content.Context
import androidx.core.content.edit

/** 서버가 발급한 로그인 정보. [nickname] 이 null 이면 아직 닉네임을 정하지 않은 계정 */
data class Session(val accessToken: String, val expiresAtMillis: Long, val nickname: String?)

interface SessionStorage {
    fun load(): Session?
    fun save(session: Session)
    fun clear()
}

/**
 * 서비스 로그인 토큰을 앱 전용 저장소에 둔다 (다른 앱이 읽을 수 없고, 앱 백업은 꺼져 있다).
 * 토큰은 1시간 뒤 만료되며, 그때는 카카오 토큰으로 다시 교환한다. 토큰 값은 로그에 남기지 않는다.
 */
class PrefsSessionStorage(context: Context) : SessionStorage {
    private val prefs = context.applicationContext.getSharedPreferences("session", Context.MODE_PRIVATE)

    override fun load(): Session? {
        val token = prefs.getString(KEY_TOKEN, null) ?: return null
        return Session(token, prefs.getLong(KEY_EXPIRES_AT, 0L), prefs.getString(KEY_NICKNAME, null))
    }

    override fun save(session: Session) = prefs.edit(commit = true) {
        putString(KEY_TOKEN, session.accessToken)
        putLong(KEY_EXPIRES_AT, session.expiresAtMillis)
        putString(KEY_NICKNAME, session.nickname)
    }

    override fun clear() = prefs.edit(commit = true) { clear() }

    private companion object {
        const val KEY_TOKEN = "access_token"
        const val KEY_EXPIRES_AT = "expires_at"
        const val KEY_NICKNAME = "nickname"
    }
}
