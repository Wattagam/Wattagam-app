package com.wattagam.storage

import com.oracle.bmc.objectstorage.ObjectStorageClient
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import com.wattagam.common.api.ApiException
import com.wattagam.common.api.ErrorCode
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.net.InetSocketAddress
import java.nio.file.Path
import java.security.KeyPairGenerator
import java.util.Base64
import java.util.concurrent.CopyOnWriteArrayList
import javax.imageio.ImageIO
import kotlin.io.path.writeText
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class GuestbookImageStorageTest {
    @TempDir
    lateinit var tempDir: Path

    private lateinit var server: HttpServer
    private lateinit var client: ObjectStorageClient
    private lateinit var storage: GuestbookImageStorage
    private val uploads = CopyOnWriteArrayList<Upload>()
    private var stored: ByteArray? = null

    @BeforeEach
    fun start() {
        val keyFile = tempDir.resolve("test-signing-key.pem").apply { writeText(testKey) }
        val configFile = tempDir.resolve("oci-config").apply {
            writeText(
                """
                [test]
                tenancy=ocid1.tenancy.oc1..wattagamtest
                user=ocid1.user.oc1..wattagamtest
                fingerprint=00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00
                key_file=$keyFile
                region=ap-seoul-1
                """.trimIndent(),
            )
        }
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/", ::handle)
        server.start()
        val properties = OciStorageProperties("ap-seoul-1", "test-namespace", "test-bucket", configFile.toString(), "test")
        val configuration = OciStorageConfiguration()
        client = configuration.objectStorageClient(properties)
        client.setEndpoint("http://127.0.0.1:${server.address.port}")
        storage = configuration.guestbookImageStorage(client, properties)
    }

    @AfterEach
    fun stop() {
        if (::client.isInitialized) {
            client.close()
        }
        if (::server.isInitialized) {
            server.stop(0)
        }
    }

    @Test
    fun `creates an immutable image and reads and deletes the exact object`() {
        val key = storage.createKey()
        val original = jpegFixture()
        assertTrue(storage.put(key, original))
        assertFalse(storage.put(key, jpegFixture(width = 2)))
        assertArrayEquals(original, storage.read(key))
        val upload = uploads.first()
        assertEquals("/n/test-namespace/b/test-bucket/o/$key", upload.path)
        assertEquals("image/jpeg", upload.contentType)
        assertEquals(original.size.toString(), upload.contentLength)
        assertEquals("*", upload.ifNoneMatch)
        assertNotNull(upload.authorization)

        storage.delete(key)
        storage.delete(key)
        assertNull(storage.read(key))
    }

    @Test
    fun `rejects invalid inputs before calling OCI`() {
        val key = storage.createKey()
        val invalidImages = listOf(
            byteArrayOf(),
            byteArrayOf(0xff.toByte(), 0xd8.toByte(), 0xff.toByte(), 0xd9.toByte()),
            jpegFixture(width = GuestbookImageStorage.MAX_IMAGE_SIDE + 1),
            ByteArray(GuestbookImageStorage.MAX_IMAGE_BYTES + 1),
        )
        invalidImages.forEach { image ->
            val failure = assertThrows(ApiException::class.java) { storage.put(key, image) }
            assertEquals(ErrorCode.INVALID_REQUEST, failure.errorCode)
        }
        assertThrows(IllegalArgumentException::class.java) { storage.delete("../image.jpg") }
        assertTrue(uploads.isEmpty())
    }

    @Test
    fun `propagates an ambiguous 404 on read and delete`() {
        val unauthorizedStorage = GuestbookImageStorage(client, "test-namespace", "other-bucket")
        val key = unauthorizedStorage.createKey()
        assertThrows(ImageStorageException::class.java) { unauthorizedStorage.read(key) }
        assertThrows(ImageStorageException::class.java) { unauthorizedStorage.delete(key) }
    }

    private fun handle(exchange: HttpExchange) {
        exchange.use {
            if (!exchange.requestURI.path.startsWith("/n/test-namespace/b/test-bucket/o/")) {
                return sendError(exchange, 404, "NotAuthorizedOrNotFound")
            }
            when (exchange.requestMethod) {
                "PUT" -> handlePut(exchange)
                "GET" -> {
                    val image = stored ?: return sendError(exchange, 404, "ObjectNotFound")
                    exchange.responseHeaders.set("Content-Type", "image/jpeg")
                    exchange.sendResponseHeaders(200, image.size.toLong())
                    exchange.responseBody.write(image)
                }
                "DELETE" -> {
                    if (stored == null) {
                        return sendError(exchange, 404, "ObjectNotFound")
                    }
                    stored = null
                    exchange.sendResponseHeaders(204, -1)
                }
                else -> exchange.sendResponseHeaders(405, -1)
            }
        }
    }

    private fun handlePut(exchange: HttpExchange) {
        val headers = exchange.requestHeaders
        uploads.add(Upload(
            exchange.requestURI.path,
            headers.getFirst("Content-Type"),
            headers.getFirst("Content-Length"),
            headers.getFirst("If-None-Match"),
            headers.getFirst("Authorization"),
        ))
        val body = exchange.requestBody.readAllBytes()
        if (stored != null) {
            return sendError(exchange, 412, "NoEtagMatch")
        }
        stored = body
        exchange.sendResponseHeaders(200, -1)
    }

    private fun sendError(exchange: HttpExchange, status: Int, code: String) {
        val body = """{"code":"$code","message":"Test error"}""".toByteArray()
        exchange.responseHeaders.set("Content-Type", "application/json")
        exchange.sendResponseHeaders(status, body.size.toLong())
        exchange.responseBody.write(body)
    }

    private data class Upload(
        val path: String,
        val contentType: String?,
        val contentLength: String?,
        val ifNoneMatch: String?,
        val authorization: String?,
    )

    companion object {
        private val testKey by lazy {
            val key = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair().private
            val encoded = Base64.getMimeEncoder(64, "\n".toByteArray()).encodeToString(key.encoded)
            "-----BEGIN PRIVATE KEY-----\n$encoded\n-----END PRIVATE KEY-----\n"
        }
    }
}

internal fun jpegFixture(width: Int = 1): ByteArray = ByteArrayOutputStream().use { output ->
    check(ImageIO.write(BufferedImage(width, 1, BufferedImage.TYPE_INT_RGB), "JPEG", output)) {
        "테스트 JPEG를 생성할 수 없습니다."
    }
    output.toByteArray()
}
