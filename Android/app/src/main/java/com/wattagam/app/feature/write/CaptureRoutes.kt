package com.wattagam.app.feature.write

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.wattagam.app.R
import com.wattagam.app.data.draft.DraftStore
import com.wattagam.app.feature.capture.CaptureMode
import com.wattagam.app.feature.capture.CaptureModeScreen
import com.wattagam.app.feature.capture.PhotoCaptureScreen
import com.wattagam.app.feature.capture.PhotoPreviewScreen
import com.wattagam.app.ui.components.RecordingNavy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 05 · 촬영 방식 선택. AR 을 고르면 이 기기가 AR 을 지원하는지 확인하고, 지원하지 않으면 일반 촬영으로 안내한다.
 * 이 장소에 남겨둔 초안이 있으면 이어서 쓸지 먼저 묻는다.
 */
@Composable
fun WriteModeRoute(
    placeId: String,
    stampCount: Int,
    stampTotal: Int,
    drafts: DraftStore,
    onBack: () -> Unit,
    onAr: () -> Unit,
    onPhoto: () -> Unit,
    onResumeDraft: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val hasDraft by produceState<Boolean?>(null, placeId) { value = withContext(Dispatchers.IO) { drafts.hasPhoto(placeId) } }
    LightSystemBarIcons()
    var draftAnswered by rememberSaveable { mutableStateOf(false) }
    var checkingAr by remember { mutableStateOf(false) }
    var showAr by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }

    CaptureModeScreen(
        stampCount = stampCount,
        stampTotal = stampTotal,
        onBack = onBack,
        onHelp = { showHelp = true },
        onSelectMode = { mode ->
            when {
                mode == CaptureMode.Photo -> onPhoto()
                checkingAr -> Unit // 확인 중 중복 선택 방지
                else -> {
                    checkingAr = true
                    scope.launch {
                        val supported = isArSupported(context)
                        checkingAr = false
                        if (supported) onAr() else showAr = true
                    }
                }
            }
        },
        background = { Box(Modifier.matchParentSize().background(RecordingNavy)) },
        character = { Image(painterResource(R.drawable.bc_puang), stringResource(R.string.bc_puang), Modifier.size(240.dp)) },
    )

    if (hasDraft == true && !draftAnswered) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.write_draft_title)) },
            text = { Text(stringResource(R.string.write_draft_body)) },
            confirmButton = { TextButton(onClick = { draftAnswered = true; onResumeDraft() }) { Text(stringResource(R.string.write_draft_resume)) } },
            dismissButton = {
                TextButton(onClick = { draftAnswered = true; scope.launch { drafts.clear(placeId) } }) { Text(stringResource(R.string.write_draft_new)) }
            },
        )
    }
    if (showAr) {
        AlertDialog(
            onDismissRequest = { showAr = false },
            title = { Text(stringResource(R.string.write_ar_title)) },
            text = { Text(stringResource(R.string.write_ar_body)) },
            confirmButton = { TextButton(onClick = { showAr = false; onPhoto() }) { Text(stringResource(R.string.write_ar_photo)) } },
            dismissButton = { TextButton(onClick = { showAr = false }) { Text(stringResource(R.string.write_cancel)) } },
        )
    }
    if (showHelp) {
        MessageDialog(R.string.write_help_title, R.string.write_help_body) { showHelp = false }
    }
}

/**
 * 05-B · 일반 촬영 (CameraX). 찍은 사진과 갤러리에서 고른 사진은 앱 내부 초안 파일로 저장한 뒤 [onPhotoReady].
 * 카메라 권한은 이 화면에 들어올 때 요청하고, 거절해도 갤러리로 고를 수 있다.
 * 미리보기에 사진에 담길 칸(꾸미기·업로드와 같은 비율)을 보여주고, 찍은 사진도 그 칸만큼 잘라 저장한다.
 * 손전등 버튼은 휴대폰 손전등(토치)을 켜고 끈다.
 */
@Composable
fun WriteCameraRoute(
    placeId: String,
    placeName: String,
    drafts: DraftStore,
    onBack: () -> Unit,
    onPhotoReady: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var asked by rememberSaveable { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    LaunchedEffect(Unit) {
        if (!granted && !asked) {
            asked = true
            permission.launch(Manifest.permission.CAMERA)
        }
    }

    val controller = remember { LifecycleCameraController(context).apply { setEnabledUseCases(CameraController.IMAGE_CAPTURE) } }
    DisposableEffect(granted, lifecycleOwner) {
        if (granted) controller.bindToLifecycle(lifecycleOwner)
        onDispose { controller.unbind() }
    }

    LightSystemBarIcons()
    var flash by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(flash, granted) { if (granted) controller.enableTorch(flash) }
    var previewSize by remember { mutableStateOf(IntSize.Zero) }
    var capturing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<Int?>(null) }
    val picker = rememberGalleryPicker(placeId, drafts, onImported = onPhotoReady, onFailed = { error = R.string.write_photo_failed })

    PhotoCaptureScreen(
        placeName = placeName,
        onBack = onBack,
        onCapture = {
            when {
                !granted -> permission.launch(Manifest.permission.CAMERA)
                capturing -> Unit // 저장 중 중복 촬영 방지
                else -> {
                    capturing = true
                    val viewAspect = if (previewSize.height > 0) previewSize.width.toFloat() / previewSize.height else EDITOR_ASPECT
                    val options = ImageCapture.OutputFileOptions.Builder(drafts.captureTempFile(placeId)).build()
                    controller.takePicture(options, ContextCompat.getMainExecutor(context), object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            scope.launch {
                                val cropped = withContext(Dispatchers.IO) { cropToGuide(drafts.captureTempFile(placeId), viewAspect) }
                                capturing = false
                                if (cropped) {
                                    drafts.commitCapture(placeId)
                                    onPhotoReady()
                                } else {
                                    error = R.string.write_capture_failed
                                }
                            }
                        }

                        override fun onError(exception: ImageCaptureException) {
                            capturing = false
                            error = R.string.write_capture_failed
                        }
                    })
                }
            }
        },
        onGallery = { picker() },
        onFlash = { flash = !flash },
        flashEnabled = flash,
        background = {
            if (granted) {
                AndroidView(
                    factory = { PreviewView(it).apply { this.controller = controller; scaleType = PreviewView.ScaleType.FILL_CENTER } },
                    modifier = Modifier.matchParentSize().onSizeChanged { previewSize = it },
                )
                CaptureGuide(Modifier.matchParentSize())
            } else {
                Box(Modifier.matchParentSize().background(RecordingNavy)) {
                    Text(
                        stringResource(R.string.write_camera_permission),
                        Modifier.align(Alignment.Center).padding(32.dp),
                        color = Color.White, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
                    )
                }
            }
        },
    )
    error?.let { MessageDialog(null, it) { error = null } }
}

/** 미리보기 위에 사진에 담길 칸(가로 꽉, [EDITOR_ASPECT] 비율, 가운데)을 보여주고 칸 밖은 어둡게 한다 */
@Composable
private fun CaptureGuide(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val frameHeight = size.width / EDITOR_ASPECT
        val top = (size.height - frameHeight) / 2
        val corner = CornerRadius(16.dp.toPx())
        val frame = RoundRect(0f, top, size.width, top + frameHeight, corner)
        val outside = Path().apply {
            fillType = PathFillType.EvenOdd
            addRect(Rect(Offset.Zero, size))
            addRoundRect(frame)
        }
        drawPath(outside, Color.Black.copy(alpha = .45f))
        drawPath(Path().apply { addRoundRect(frame) }, Color.White.copy(alpha = .8f), style = Stroke(2.dp.toPx()))
    }
}

/**
 * 찍은 사진을 미리보기의 칸([CaptureGuide])만큼 잘라 같은 파일에 다시 저장한다 (백그라운드 스레드에서 호출).
 * 미리보기는 사진 가운데를 화면 비율([viewAspect])로 채워 보여주므로, 보이던 영역의 가로 전체를 칸 비율로 자른다.
 */
private fun cropToGuide(file: java.io.File, viewAspect: Float): Boolean = runCatching {
    val source = decodePhoto(file, OUTPUT_MAX_SIDE) ?: return false
    val visibleWidth = minOf(source.width.toFloat(), source.height * viewAspect)
    val cropWidth = visibleWidth.toInt()
    val cropHeight = minOf(source.height.toFloat(), visibleWidth / EDITOR_ASPECT).toInt()
    val cropped = Bitmap.createBitmap(source, (source.width - cropWidth) / 2, (source.height - cropHeight) / 2, cropWidth, cropHeight)
    file.outputStream().use { cropped.compress(Bitmap.CompressFormat.JPEG, 92, it) }
}.getOrDefault(false)

/** 06 · 사진 확인. 갤러리에서 바꾸면 같은 초안 파일을 다시 읽는다 */
@Composable
fun WritePreviewRoute(
    placeId: String,
    placeName: String,
    drafts: DraftStore,
    onBack: () -> Unit,
    onRetake: () -> Unit,
    onEdit: () -> Unit,
    onNext: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var version by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<Int?>(null) }
    val photo by rememberPhoto(drafts.photoFile(placeId), version)
    val picker = rememberGalleryPicker(placeId, drafts, onImported = { version++ }, onFailed = { error = R.string.write_photo_failed })
    val painter = remember(photo) { photo?.let { BitmapPainter(it) } ?: ColorPainter(RecordingNavy) }

    PhotoPreviewScreen(
        placeName = placeName,
        photo = painter,
        onBack = onBack,
        onRetake = onRetake,
        onGallery = { picker() },
        onEdit = onEdit,
        // 꾸미지 않고 넘어가면 예전에 꾸민 사진 대신 원본을 올린다
        onNext = { scope.launch { withContext(Dispatchers.IO) { drafts.decoratedFile(placeId).delete() }; onNext() } },
    )
    error?.let { MessageDialog(null, it) { error = null } }
}

/** 갤러리(Photo Picker)에서 사진 한 장을 골라 초안 원본으로 복사한다. 반환값을 부르면 선택 화면이 열린다 */
@Composable
private fun rememberGalleryPicker(placeId: String, drafts: DraftStore, onImported: () -> Unit, onFailed: () -> Unit): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch { if (drafts.importPhoto(context, placeId, uri)) onImported() else onFailed() }
    }
    return { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
}

@Composable
internal fun MessageDialog(title: Int?, body: Int, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = if (title != null) { { Text(stringResource(title)) } } else null,
        text = { Text(stringResource(body)) },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.write_ok)) } },
    )
}
