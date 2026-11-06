package com.wattagam.app.feature.write

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max

/** 화면 표시용 최대 변 길이 (px). 원본 카메라 사진은 훨씬 커서 그대로 올리면 메모리가 부족하다 */
internal const val DISPLAY_MAX_SIDE = 1600

/** 합성·업로드용 최대 변 길이 (px) */
internal const val OUTPUT_MAX_SIDE = 2048

/**
 * 사진 파일을 [maxSide] 이하로 줄여 읽고, 카메라가 EXIF 에만 적어둔 회전을 실제 픽셀에 반영한다.
 * 파일이 없거나 읽지 못하면 null.
 */
internal fun decodePhoto(file: File, maxSide: Int): Bitmap? {
    if (!file.exists()) return null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sample = 1
    while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
    val bitmap = BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
    val degrees = when (runCatching { ExifInterface(file.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }.getOrNull()) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }
    if (degrees == 0f) return bitmap
    val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(degrees) }, true)
    if (rotated !== bitmap) bitmap.recycle()
    return rotated
}

/**
 * 사진 파일을 백그라운드에서 읽어 화면에 쓸 이미지로 준다. 읽는 동안·실패하면 null.
 * [version] 이 바뀌면(같은 경로에 새 사진을 저장한 경우) 다시 읽는다.
 */
@Composable
internal fun rememberPhoto(file: File?, version: Int = 0): State<ImageBitmap?> =
    produceState<ImageBitmap?>(null, file?.path, version) {
        value = file?.let { withContext(Dispatchers.IO) { decodePhoto(it, DISPLAY_MAX_SIDE)?.asImageBitmap() } }
    }

/** 지금 떠 있는 어두운 화면 수. 화면 전환 중에는 새 화면이 먼저 들어오고 이전 화면이 나중에 빠지므로 개수로 센다 */
private var darkScreens = 0

/**
 * 어두운 화면(촬영·꾸미기·업로드)에서는 시스템 바 아이콘을 밝게 하고, 어두운 화면이 모두 사라지면 앱 기본(어두운 아이콘)으로 되돌린다.
 */
@Composable
internal fun LightSystemBarIcons() {
    val view = LocalView.current
    if (view.isInEditMode) return
    DisposableEffect(view) {
        val controller = (view.context as? Activity)?.window?.let { WindowCompat.getInsetsController(it, view) }
        darkScreens++
        controller?.isAppearanceLightStatusBars = false
        controller?.isAppearanceLightNavigationBars = false
        onDispose {
            darkScreens--
            if (darkScreens == 0) {
                controller?.isAppearanceLightStatusBars = true
                controller?.isAppearanceLightNavigationBars = true
            }
        }
    }
}
