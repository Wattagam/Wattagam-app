package com.wattagam.storage

import com.oracle.bmc.ClientConfiguration
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider
import com.oracle.bmc.objectstorage.ObjectStorageClient
import com.oracle.bmc.retrier.RetryConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@ConfigurationProperties("app.storage.oci")
class OciStorageProperties(
    val region: String = "",
    val namespace: String = "",
    val bucket: String = "",
    val configFile: String = "",
    val profile: String = "DEFAULT",
)

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "app.storage.oci", name = ["enabled"], havingValue = "true")
@EnableConfigurationProperties(OciStorageProperties::class)
class OciStorageConfiguration {

    @Bean(destroyMethod = "close")
    fun objectStorageClient(properties: OciStorageProperties): ObjectStorageClient {
        require(properties.region.isNotBlank() && properties.namespace.isNotBlank() && properties.bucket.isNotBlank()) {
            "OCI 이미지 저장소를 켜려면 리전, 네임스페이스와 버킷을 지정해야 합니다."
        }
        val provider = if (properties.configFile.isBlank()) {
            ConfigFileAuthenticationDetailsProvider(properties.profile)
        } else {
            ConfigFileAuthenticationDetailsProvider(properties.configFile, properties.profile)
        }
        return ObjectStorageClient.builder()
            .region(properties.region)
            .configuration(
                ClientConfiguration.builder()
                    .connectionTimeoutMillis(2_000)
                    .readTimeoutMillis(5_000)
                    .retryConfiguration(RetryConfiguration.NO_RETRY_CONFIGURATION)
                    .build(),
            )
            .build(provider)
    }

    @Bean
    fun guestbookImageStorage(client: ObjectStorageClient, properties: OciStorageProperties): GuestbookImageStorage =
        GuestbookImageStorage(client, properties.namespace, properties.bucket)
}
