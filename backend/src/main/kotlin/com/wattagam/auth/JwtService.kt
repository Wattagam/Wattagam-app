package com.wattagam.auth

import java.time.Clock
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.stereotype.Service

@Service
class JwtService(
    private val encoder: JwtEncoder,
    private val properties: JwtProperties,
    private val clock: Clock,
) {

    fun issueAccessToken(userId: Long): String {
        val now = clock.instant()
        val claims = JwtClaimsSet.builder()
            .issuer(properties.issuer)
            .audience(listOf(properties.audience))
            .subject(userId.toString())
            .issuedAt(now)
            .expiresAt(now.plusSeconds(properties.expiresIn))
            .build()
        val header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build()
        return encoder.encode(JwtEncoderParameters.from(header, claims)).tokenValue
    }
}
