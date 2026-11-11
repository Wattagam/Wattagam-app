package com.wattagam.storage

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable

@EnabledIfEnvironmentVariable(named = "OCI_STORAGE_SMOKE_TEST", matches = "true")
class OciImageStorageSmokeTest {

    @Test
    fun `uploads reads and deletes a synthetic JPEG in the development bucket`() {
        val properties = OciStorageProperties(
            region = System.getenv("OCI_STORAGE_REGION").orEmpty(),
            namespace = System.getenv("OCI_STORAGE_NAMESPACE").orEmpty(),
            bucket = System.getenv("OCI_STORAGE_BUCKET").orEmpty(),
            configFile = System.getenv("OCI_CONFIG_FILE").orEmpty(),
            profile = System.getenv("OCI_CONFIG_PROFILE") ?: "DEFAULT",
        )
        require(properties.bucket == "wattagam-images-dev") { "개발 버킷에서만 실제 저장소 테스트를 실행합니다." }
        val configuration = OciStorageConfiguration()
        configuration.objectStorageClient(properties).use { client ->
            val storage = configuration.guestbookImageStorage(client, properties)
            val key = storage.createKey()
            val image = jpegFixture()
            var cleanup = true
            AutoCloseable {
                if (cleanup) {
                    storage.delete(key)
                }
            }.use {
                cleanup = storage.put(key, image)
                assertTrue(cleanup)
                assertArrayEquals(image, storage.read(key))
                storage.delete(key)
                cleanup = false
                storage.delete(key)
                assertNull(storage.read(key))
            }
        }
    }
}
