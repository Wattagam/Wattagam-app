package com.wattagam.guestbook

import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

/** 이미지 정리 재시도 작업을 실행하기 위해 스케줄링을 켠다. */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
class GuestbookConfiguration
