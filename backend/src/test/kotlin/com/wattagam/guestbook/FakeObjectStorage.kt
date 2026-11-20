package com.wattagam.guestbook

import com.oracle.bmc.model.BmcException
import com.oracle.bmc.objectstorage.ObjectStorage
import com.oracle.bmc.objectstorage.requests.DeleteObjectRequest
import com.oracle.bmc.objectstorage.requests.GetObjectRequest
import com.oracle.bmc.objectstorage.requests.PutObjectRequest
import com.oracle.bmc.objectstorage.responses.DeleteObjectResponse
import com.oracle.bmc.objectstorage.responses.GetObjectResponse
import com.oracle.bmc.objectstorage.responses.PutObjectResponse
import com.wattagam.storage.GuestbookImageStorage
import java.io.ByteArrayInputStream
import java.util.concurrent.ConcurrentHashMap
import org.mockito.Mockito

/**
 * OCI 대신 메모리에 객체를 저장하는 테스트용 저장소.
 * 실제 [GuestbookImageStorage] 검증 로직을 그대로 쓰고 OCI 클라이언트만 대체한다.
 */
class FakeObjectStorage {
    val objects: MutableMap<String, ByteArray> = ConcurrentHashMap()
    val deletedKeys = java.util.concurrent.CopyOnWriteArrayList<String>()

    @Volatile
    var failPut = false

    @Volatile
    var failGet = false

    @Volatile
    var failDeleteKeys: Set<String> = emptySet()

    @Volatile
    var failAllDeletes = false

    /** 다음 put N회를 이미 존재하는 객체(412)로 거절한다. */
    @Volatile
    var rejectNextPuts = 0

    private val client: ObjectStorage = Mockito.mock(ObjectStorage::class.java) { invocation ->
        when (invocation.method.name) {
            "putObject" -> put(invocation.arguments[0] as PutObjectRequest)
            "getObject" -> get(invocation.arguments[0] as GetObjectRequest)
            "deleteObject" -> delete(invocation.arguments[0] as DeleteObjectRequest)
            "close" -> null
            else -> throw UnsupportedOperationException(invocation.method.name)
        }
    }

    val storage = GuestbookImageStorage(client, "test-namespace", "test-bucket")

    fun reset() {
        objects.clear()
        deletedKeys.clear()
        failPut = false
        failGet = false
        failDeleteKeys = emptySet()
        failAllDeletes = false
        rejectNextPuts = 0
    }

    private fun put(request: PutObjectRequest): PutObjectResponse {
        if (failPut) {
            throw BmcException(500, "InternalError", "저장소 오류", "test")
        }
        if (rejectNextPuts > 0) {
            rejectNextPuts--
            throw BmcException(412, "IfNoneMatchFailed", "이미 존재하는 객체", "test")
        }
        val bytes = request.putObjectBody.readAllBytes()
        if (objects.putIfAbsent(request.objectName, bytes) != null) {
            throw BmcException(412, "IfNoneMatchFailed", "이미 존재하는 객체", "test")
        }
        return PutObjectResponse.builder().build()
    }

    private fun get(request: GetObjectRequest): GetObjectResponse {
        if (failGet) {
            throw BmcException(500, "InternalError", "저장소 오류", "test")
        }
        val bytes = objects[request.objectName] ?: throw BmcException(404, "ObjectNotFound", "객체 없음", "test")
        return GetObjectResponse.builder().inputStream(ByteArrayInputStream(bytes)).build()
    }

    private fun delete(request: DeleteObjectRequest): DeleteObjectResponse {
        if (failAllDeletes || request.objectName in failDeleteKeys) {
            throw BmcException(500, "InternalError", "저장소 오류", "test")
        }
        objects.remove(request.objectName)
        deletedKeys += request.objectName
        return DeleteObjectResponse.builder().build()
    }
}
