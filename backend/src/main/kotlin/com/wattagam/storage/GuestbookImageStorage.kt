package com.wattagam.storage

import com.oracle.bmc.model.BmcException
import com.oracle.bmc.objectstorage.ObjectStorage
import com.oracle.bmc.objectstorage.requests.DeleteObjectRequest
import com.oracle.bmc.objectstorage.requests.GetObjectRequest
import com.oracle.bmc.objectstorage.requests.PutObjectRequest
import com.wattagam.common.api.ApiException
import com.wattagam.common.api.ErrorCode
import java.io.IOException
import java.util.UUID
import javax.imageio.ImageIO
import javax.imageio.stream.MemoryCacheImageInputStream

class GuestbookImageStorage(
    private val client: ObjectStorage,
    private val namespace: String,
    private val bucket: String,
) {

    fun createKey(): String = "guestbook/${UUID.randomUUID()}.jpg"

    fun put(key: String, image: ByteArray): Boolean {
        validateKey(key)
        validateJpeg(image)
        val request = PutObjectRequest.builder().namespaceName(namespace).bucketName(bucket).objectName(key)
            .contentType("image/jpeg").contentLength(image.size.toLong()).ifNoneMatch("*")
            .putObjectBody(image.inputStream()).build()
        return try {
            client.putObject(request)
            true
        } catch (exception: BmcException) {
            if (exception.statusCode == 412) {
                return false
            }
            throw ImageStorageException("방명록 이미지를 저장하지 못했습니다.", exception)
        }
    }

    fun read(key: String): ByteArray? {
        validateKey(key)
        val request = GetObjectRequest.builder().namespaceName(namespace).bucketName(bucket).objectName(key).build()
        try {
            return client.getObject(request).inputStream.use { stream ->
                val image = stream.readNBytes(MAX_IMAGE_BYTES + 1)
                if (image.size > MAX_IMAGE_BYTES) {
                    throw ImageStorageException("저장된 방명록 이미지가 허용 용량을 초과합니다.")
                }
                image
            }
        } catch (exception: BmcException) {
            if (exception.isObjectMissing()) {
                return null
            }
            throw ImageStorageException("방명록 이미지를 읽지 못했습니다.", exception)
        } catch (exception: IOException) {
            throw ImageStorageException("방명록 이미지를 읽지 못했습니다.", exception)
        }
    }

    fun delete(key: String) {
        validateKey(key)
        val request = DeleteObjectRequest.builder().namespaceName(namespace).bucketName(bucket).objectName(key).build()
        try {
            client.deleteObject(request)
        } catch (exception: BmcException) {
            if (!exception.isObjectMissing()) {
                throw ImageStorageException("방명록 이미지를 삭제하지 못했습니다.", exception)
            }
        }
    }

    private fun validateKey(key: String) {
        require(KEY_PATTERN.matches(key)) { "방명록 이미지 키 형식이 올바르지 않습니다." }
    }

    private fun validateJpeg(image: ByteArray) {
        if (image.size !in 4..MAX_IMAGE_BYTES ||
            image[image.lastIndex - 1] != 0xff.toByte() || image.last() != 0xd9.toByte()
        ) {
            throw ApiException(ErrorCode.INVALID_REQUEST)
        }
        val reader = ImageIO.getImageReadersByFormatName("JPEG").next()
        try {
            MemoryCacheImageInputStream(image.inputStream()).use { input ->
                reader.setInput(input, true, true)
                if (reader.getWidth(0) > MAX_IMAGE_SIDE || reader.getHeight(0) > MAX_IMAGE_SIDE) {
                    throw ApiException(ErrorCode.INVALID_REQUEST)
                }
                val sample = reader.defaultReadParam.apply { setSourceSubsampling(8, 8, 0, 0) }
                reader.read(0, sample).flush()
            }
        } catch (_: IOException) {
            throw ApiException(ErrorCode.INVALID_REQUEST)
        } finally {
            reader.dispose()
        }
    }

    private fun BmcException.isObjectMissing(): Boolean = statusCode == 404 && serviceCode == "ObjectNotFound"

    companion object {
        const val MAX_IMAGE_BYTES = 5 * 1024 * 1024
        const val MAX_IMAGE_SIDE = 4096
        private val KEY_PATTERN = Regex("guestbook/[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}\\.jpg")
    }
}
