package com.wattagam.app.data.auth

import android.content.Context
import com.kakao.sdk.auth.AuthApiClient
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.KakaoSdk
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.common.model.KakaoSdkError
import com.kakao.sdk.user.UserApiClient
import com.wattagam.app.BuildConfig
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

sealed interface KakaoLoginResult {
    /** 카카오 access token. 서버 로그인 API 가 나오면 이 토큰을 서버에 보내 우리 서비스 토큰으로 교환한다 */
    data class Success(val kakaoAccessToken: String) : KakaoLoginResult
    data object Cancelled : KakaoLoginResult
    data object Failed : KakaoLoginResult
}

/**
 * 카카오 로그인 (카카오톡 앱 우선, 없거나 실패하면 카카오계정 웹 로그인).
 * 앱 키는 local.properties 의 KAKAO_NATIVE_APP_KEY. 키가 없으면 [isConfigured] 가 false 이고 샘플 흐름을 쓴다.
 * 토큰은 로그에 남기지 않는다.
 */
object KakaoLogin {
    val isConfigured: Boolean get() = BuildConfig.KAKAO_NATIVE_APP_KEY.isNotBlank()

    fun init(context: Context) {
        if (isConfigured) KakaoSdk.init(context, BuildConfig.KAKAO_NATIVE_APP_KEY)
    }

    suspend fun login(context: Context): KakaoLoginResult = suspendCancellableCoroutine { continuation ->
        fun finish(result: KakaoLoginResult) {
            if (continuation.isActive) continuation.resume(result)
        }
        val accountCallback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
            finish(
                when {
                    token != null -> KakaoLoginResult.Success(token.accessToken)
                    error.isCancel() -> KakaoLoginResult.Cancelled
                    else -> KakaoLoginResult.Failed
                },
            )
        }
        val client = UserApiClient.instance
        if (client.isKakaoTalkLoginAvailable(context)) {
            client.loginWithKakaoTalk(context) { token, error ->
                when {
                    token != null -> finish(KakaoLoginResult.Success(token.accessToken))
                    // 사용자가 카카오톡 로그인 화면에서 취소
                    error.isCancel() -> finish(KakaoLoginResult.Cancelled)
                    // 카카오톡에 연결된 계정이 없는 경우 등 → 카카오계정으로 다시 시도
                    else -> client.loginWithKakaoAccount(context, callback = accountCallback)
                }
            }
        } else {
            client.loginWithKakaoAccount(context, callback = accountCallback)
        }
    }

    /**
     * 이전에 로그인해 저장된 카카오 토큰이 아직 쓸 수 있는지 확인한다 (자동 로그인).
     * 토큰이 만료·해지됐으면 false. 네트워크 오류처럼 판단할 수 없을 때는 저장된 토큰을 믿는다 (서버 로그인 단계에서 다시 검증).
     */
    suspend fun hasSession(): Boolean {
        if (!isConfigured || !AuthApiClient.instance.hasToken()) return false
        return suspendCancellableCoroutine { continuation ->
            UserApiClient.instance.accessTokenInfo { info, error ->
                val valid = info != null || !(error is KakaoSdkError && error.isInvalidTokenError())
                if (continuation.isActive) continuation.resume(valid)
            }
        }
    }

    /** 로그아웃·탈퇴 시 카카오 SDK 의 토큰도 지운다 */
    fun logout() {
        if (isConfigured) UserApiClient.instance.logout { }
    }

    private fun Throwable?.isCancel() = this is ClientError && reason == ClientErrorCause.Cancelled
}
