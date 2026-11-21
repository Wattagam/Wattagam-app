package com.wattagam.auth.kakao

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.wattagam.common.api.ApiException
import com.wattagam.common.api.ErrorCode
import org.springframework.http.MediaType
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import java.net.HttpURLConnection

@Component
class KakaoClient(
    private val properties: KakaoProperties,
    builder: RestClient.Builder,
) {
    private val restClient = builder.clone()
        .baseUrl(properties.baseUrl)
        .requestFactory(
            object : SimpleClientHttpRequestFactory() {
                override fun prepareConnection(connection: HttpURLConnection, httpMethod: String) {
                    super.prepareConnection(connection, httpMethod)
                    // 리다이렉트 대상에 카카오 토큰이 전달되지 않도록 한다.
                    connection.instanceFollowRedirects = false
                }
            }.apply {
                setConnectTimeout(properties.connectTimeout.toMillis().toInt())
                setReadTimeout(properties.readTimeout.toMillis().toInt())
            },
        )
        .build()

    fun verifyAccessToken(token: String): Long {
        if (
            token.isBlank() || token.equals("Bearer", ignoreCase = true) ||
            token.any { it.isWhitespace() || it.isISOControl() }
        ) {
            throw ApiException(ErrorCode.KAKAO_UNAUTHORIZED)
        }

        val response = requestTokenInfo(token) ?: throw ApiException(ErrorCode.KAKAO_UNAUTHORIZED)
        val userId = response.id ?: throw ApiException(ErrorCode.KAKAO_UNAUTHORIZED)
        if (userId <= 0 || response.appId != properties.appId || (response.expiresIn ?: 0) <= 0) {
            throw ApiException(ErrorCode.KAKAO_UNAUTHORIZED)
        }
        return userId
    }

    private fun requestTokenInfo(token: String): KakaoTokenInfoResponse? {
        try {
            return restClient.get()
                .uri("/v1/user/access_token_info")
                .headers { it.setBearerAuth(token) }
                .accept(MediaType.APPLICATION_JSON)
                .exchange { _, response ->
                    when (response.statusCode.value()) {
                        200 -> response.bodyTo(KakaoTokenInfoResponse::class.java)
                        401 -> throw ApiException(ErrorCode.KAKAO_UNAUTHORIZED)
                        else -> throw ApiException(ErrorCode.KAKAO_UNAVAILABLE)
                    }
                }
        } catch (_: RestClientException) {
            // 외부 예외에는 응답 원문이 포함될 수 있으므로 원인을 전달하지 않는다.
            throw ApiException(ErrorCode.KAKAO_UNAVAILABLE)
        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
private data class KakaoTokenInfoResponse(
    val id: Long? = null,
    @param:JsonProperty("app_id")
    val appId: Long? = null,
    @param:JsonProperty("expires_in")
    val expiresIn: Long? = null,
)
