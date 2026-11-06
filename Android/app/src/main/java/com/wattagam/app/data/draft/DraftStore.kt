package com.wattagam.app.data.draft

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 장소별 작성 초안 (사진 원본·꾸민 사진·메시지).
 * 앱 내부 저장소(filesDir/drafts/<placeId>/)에만 두므로 다른 앱에서 볼 수 없고, 앱을 다시 켜도 남는다.
 * 업로드에 실패하거나 범위를 벗어나도 지우지 않는다. 등록에 성공했을 때나 '새로 찍기'를 고를 때만 [clear] 한다.
 */
class DraftStore(context: Context) {
    private val root = File(context.applicationContext.filesDir, "drafts")

    // 경로만 만든다 (화면 그리는 중에도 부를 수 있게). 폴더는 쓰기 직전에 [ensureDir]
    private fun dir(placeId: String) = File(root, placeId)

    private fun ensureDir(placeId: String) = dir(placeId).mkdirs()

    /** 촬영하거나 갤러리에서 고른 원본 */
    fun photoFile(placeId: String) = File(dir(placeId), "photo.jpg")

    /** 손그림·글자를 합친 사진 */
    fun decoratedFile(placeId: String) = File(dir(placeId), "decorated.jpg")

    private fun messageFile(placeId: String) = File(dir(placeId), "message.txt")

    /** 파일 존재 확인이라 백그라운드에서 부른다 */
    fun hasPhoto(placeId: String) = photoFile(placeId).exists()

    /** 등록에 쓸 사진: 꾸민 사진이 있으면 그것, 없으면 원본 (백그라운드에서 부른다) */
    fun publishPhoto(placeId: String): File? =
        decoratedFile(placeId).takeIf { it.exists() } ?: photoFile(placeId).takeIf { it.exists() }

    /** 카메라가 원본을 새로 쓰기 전에 임시 파일 경로를 준다 (폴더도 만든다). 촬영이 끝나면 [commitCapture] */
    fun captureTempFile(placeId: String): File {
        ensureDir(placeId)
        return File(dir(placeId), "capture.tmp")
    }

    /** 새 원본이 생기면 이전 꾸민 사진은 맞지 않으므로 지운다 */
    suspend fun commitCapture(placeId: String) = withContext(Dispatchers.IO) {
        val temp = captureTempFile(placeId)
        if (temp.exists()) {
            decoratedFile(placeId).delete()
            temp.copyTo(photoFile(placeId), overwrite = true)
            temp.delete()
        }
    }

    /** 갤러리에서 고른 사진을 초안 원본으로 복사한다. 실패하면 false */
    suspend fun importPhoto(context: Context, placeId: String, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        val temp = captureTempFile(placeId)
        val copied = runCatching {
            context.contentResolver.openInputStream(uri)?.use { input -> temp.outputStream().use { input.copyTo(it) } } != null
        }.getOrDefault(false)
        if (copied) commitCapture(placeId) else temp.delete()
        copied
    }

    suspend fun readMessage(placeId: String): String = withContext(Dispatchers.IO) {
        messageFile(placeId).takeIf { it.exists() }?.readText().orEmpty()
    }

    suspend fun writeMessage(placeId: String, message: String) = withContext(Dispatchers.IO) {
        ensureDir(placeId)
        messageFile(placeId).writeText(message)
    }

    suspend fun clear(placeId: String) = withContext(Dispatchers.IO) {
        File(root, placeId).deleteRecursively()
    }
}
