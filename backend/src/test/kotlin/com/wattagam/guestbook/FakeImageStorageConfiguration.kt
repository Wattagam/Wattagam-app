package com.wattagam.guestbook

import com.wattagam.storage.GuestbookImageStorage
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@TestConfiguration(proxyBeanMethods = false)
class FakeImageStorageConfiguration {

    @Bean
    fun fakeObjectStorage(): FakeObjectStorage = FakeObjectStorage()

    @Bean
    fun guestbookImageStorage(fake: FakeObjectStorage): GuestbookImageStorage = fake.storage
}
