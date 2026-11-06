package com.wattagam.auth

import com.wattagam.auth.kakao.KakaoProperties
import java.time.Clock
import java.time.Duration
import java.util.Base64
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwtAudienceValidator
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtIssuerValidator
import org.springframework.security.oauth2.jwt.JwtTimestampValidator
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import org.springframework.web.client.RestClient

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(JwtProperties::class, KakaoProperties::class)
class AuthConfiguration {

    @Bean
    fun clock(): Clock = Clock.systemUTC()

    @Bean
    fun restClientBuilder(): RestClient.Builder = RestClient.builder()

    @Bean
    fun jwtSecretKey(properties: JwtProperties): SecretKey {
        val bytes = try {
            Base64.getDecoder().decode(properties.secret)
        } catch (_: IllegalArgumentException) {
            throw IllegalArgumentException("서비스 JWT 서명 키는 Base64 형식이어야 합니다.")
        }
        require(bytes.size >= 32) { "서비스 JWT 서명 키는 32바이트 이상이어야 합니다." }
        return SecretKeySpec(bytes, "HmacSHA256")
    }

    @Bean
    fun jwtEncoder(key: SecretKey): JwtEncoder = NimbusJwtEncoder.withSecretKey(key).build()

    @Bean
    fun jwtDecoder(key: SecretKey, properties: JwtProperties, clock: Clock): JwtDecoder {
        val timestamps = JwtTimestampValidator(Duration.ZERO)
        timestamps.setClock(clock)
        val expiration = OAuth2TokenValidator<Jwt> { jwt ->
            if (jwt.expiresAt != null) {
                OAuth2TokenValidatorResult.success()
            } else {
                OAuth2TokenValidatorResult.failure(OAuth2Error("invalid_token", "토큰 만료 시각이 없습니다.", null))
            }
        }
        return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build().also {
            it.setJwtValidator(
                DelegatingOAuth2TokenValidator(
                    timestamps,
                    expiration,
                    JwtIssuerValidator(properties.issuer),
                    JwtAudienceValidator(properties.audience),
                ),
            )
        }
    }
}
