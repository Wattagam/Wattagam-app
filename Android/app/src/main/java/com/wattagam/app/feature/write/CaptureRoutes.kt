package com.wattagam.app.feature.write

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
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
                    controller.imageCaptureFlashMode = if (flash) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
                    val options = ImageCapture.OutputFileOptions.Builder(drafts.captureTempFile(placeId)).build()
                    controller.takePicture(options, ContextCompat.getMainExecutor(context), object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            scope.launch {
                                drafts.commitCapture(placeId)
                                capturing = false
                                onPhotoReady()
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
                    modifier = Modifier.matchParentSize(),
                )
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
