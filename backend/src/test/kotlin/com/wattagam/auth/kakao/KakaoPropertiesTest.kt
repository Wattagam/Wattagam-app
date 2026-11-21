package com.wattagam.auth.kakao

import jakarta.validation.Validation
import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class KakaoPropertiesTest {

    @Test
    fun requiresAPositiveConfiguredAppId() {
        Validation.buildDefaultValidatorFactory().use { factory ->
            val violations = factory.validator.validate(KakaoProperties(appId = 0))

            assertTrue(violations.any { it.propertyPath.toString() == "appId" })
        }
    }

    @Test
    fun preventsInfiniteOrOverflowingTimeouts() {
        for (timeout in listOf(Duration.ZERO, Duration.ofNanos(1), Duration.ofMillis(Int.MAX_VALUE.toLong() + 1))) {
            assertFailsWith<IllegalArgumentException> {
                KakaoProperties(appId = 12345, connectTimeout = timeout)
            }
            assertFailsWith<IllegalArgumentException> {
                KakaoProperties(appId = 12345, readTimeout = timeout)
            }
        }
    }
}
