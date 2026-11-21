package com.wattagam.common.api

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tools.jackson.databind.cfg.CoercionAction
import tools.jackson.databind.cfg.CoercionInputShape
import tools.jackson.databind.type.LogicalType

@Configuration(proxyBeanMethods = false)
class ApiJsonConfiguration {

    @Bean
    fun strictStringInputs(): JsonMapperBuilderCustomizer = JsonMapperBuilderCustomizer { builder ->
        // 문자열 입력을 숫자나 불리언에서 자동 변환하면 API의 자료형 검증을 우회할 수 있다.
        builder.withCoercionConfig(LogicalType.Textual) { config ->
            config.setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
            config.setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
            config.setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail)
        }
        builder.withCoercionConfig(LogicalType.Float) { config ->
            config.setCoercion(CoercionInputShape.String, CoercionAction.Fail)
            config.setCoercion(CoercionInputShape.EmptyString, CoercionAction.Fail)
            config.setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail)
        }
    }
}
