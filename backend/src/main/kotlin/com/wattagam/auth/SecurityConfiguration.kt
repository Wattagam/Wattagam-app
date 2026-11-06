package com.wattagam.auth

import com.wattagam.common.api.ApiResponse
import com.wattagam.common.api.ErrorCode
import com.wattagam.common.security.AuthenticatedUser
import com.wattagam.user.UserService
import jakarta.servlet.DispatcherType
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.dao.DataAccessException
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.BearerTokenErrors
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver
import org.springframework.security.web.SecurityFilterChain
import tools.jackson.databind.json.JsonMapper

@Configuration(proxyBeanMethods = false)
class SecurityConfiguration(private val users: UserService, private val jsonMapper: JsonMapper) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        val bearerTokens = DefaultBearerTokenResolver()
        http.csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .requestCache { it.disable() }
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            .logout { it.disable() }
            .authorizeHttpRequests { authorize ->
                authorize.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                    .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/auth/kakao").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/auth/logout").authenticated()
                    .requestMatchers(HttpMethod.PATCH, "/api/me/nickname").authenticated()
                    .requestMatchers(HttpMethod.DELETE, "/api/me").authenticated()
                    .requestMatchers("/api/**").hasAuthority("USER")
                    .anyRequest().denyAll()
            }
            .exceptionHandling { exceptions ->
                exceptions.authenticationEntryPoint { _, response, error -> writeAuthenticationError(response, error) }
                    .accessDeniedHandler { _, response, _ ->
                        val user = SecurityContextHolder.getContext().authentication?.principal as? AuthenticatedUser
                        val error = if (user?.needsNickname == true) ErrorCode.NICKNAME_REQUIRED else ErrorCode.FORBIDDEN
                        writeError(response, error)
                    }
            }
            .oauth2ResourceServer { resourceServer ->
                resourceServer.bearerTokenResolver { request ->
                    // 만료된 서비스 토큰이 카카오 재로그인을 막지 않도록 공개 진입에서는 무시한다.
                    if (request.method == "POST" && request.requestURI == request.contextPath + "/api/auth/kakao") {
                        null
                    } else {
                        bearerTokens.resolve(request)
                    }
                }.jwt { jwt -> jwt.jwtAuthenticationConverter(::authenticateUser) }
                    .authenticationEntryPoint { _, response, error -> writeAuthenticationError(response, error) }
            }
        return http.build()
    }

    private fun authenticateUser(jwt: Jwt): UsernamePasswordAuthenticationToken {
        val userId = jwt.subject?.toLongOrNull()?.takeIf { it > 0 }
            ?: throw OAuth2AuthenticationException(BearerTokenErrors.invalidToken("인증 정보가 유효하지 않습니다."))
        val user = try {
            users.findByIdOrNull(userId)
        } catch (_: DataAccessException) {
            throw OAuth2AuthenticationException(OAuth2Error("server_error"), "인증 정보를 조회할 수 없습니다.")
        }
            ?: throw OAuth2AuthenticationException(BearerTokenErrors.invalidToken("인증 정보가 유효하지 않습니다."))
        val principal = AuthenticatedUser(userId, user.nickname == null)
        val authorities = if (principal.needsNickname) emptyList() else listOf(SimpleGrantedAuthority("USER"))
        return UsernamePasswordAuthenticationToken(principal, null, authorities)
    }

    private fun writeAuthenticationError(response: HttpServletResponse, exception: AuthenticationException) {
        val errorCode = if (exception is OAuth2AuthenticationException && exception.error.errorCode == "server_error") {
            ErrorCode.INTERNAL_ERROR
        } else {
            ErrorCode.UNAUTHORIZED
        }
        writeError(response, errorCode)
    }

    private fun writeError(response: HttpServletResponse, errorCode: ErrorCode) {
        response.status = errorCode.status.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = "UTF-8"
        jsonMapper.writeValue(response.outputStream, ApiResponse.failure(errorCode))
    }
}
