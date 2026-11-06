package com.wattagam.app.feature.write

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Plane
import com.google.ar.core.TrackingState
import com.wattagam.app.R
import com.wattagam.app.data.draft.DraftStore
import com.wattagam.app.feature.capture.ArCaptureScreen
import com.wattagam.app.ui.components.RecordingNavy
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.arcore.createAnchorOrNull
import io.github.sceneview.ar.arcore.isValid
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberCollisionSystem
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import io.github.sceneview.rememberView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/** 푸앙이 3D 모델 (assets). 지금은 임시 모델이라 다듬은 모델이 오면 같은 이름으로 바꾼다 */
private const val PUANG_MODEL = "models/puang.glb"

/** 바닥에 놓았을 때 푸앙이 키 (m) */
private const val PUANG_HEIGHT_METERS = 0.4f

/**
 * 이 기기에서 AR 촬영(ARCore)을 쓸 수 있는지. 첫 확인은 잠깐 '확인 중'일 수 있어 몇 번 다시 본다.
 * ARCore 설치·업데이트만 필요한 기기도 지원으로 본다 (AR 화면이 설치를 안내한다). 확인 자체가 실패하면 미지원으로 보고 일반 촬영으로 안내한다.
 */
internal suspend fun isArSupported(context: Context): Boolean {
    repeat(10) {
        val availability = runCatching { ArCoreApk.getInstance().checkAvailability(context) }.getOrNull() ?: return false
        if (!availability.isTransient) return availability.isSupported
        delay(200)
    }
    return false
}

/**
 * 05-A · AR 촬영. 바닥을 인식하면 촬영 칸 안을 눌러 푸앙이를 놓고, 길게 누르면 다시 놓는다.
 * '사진 찍기'는 촬영 칸에 보이는 카메라 화면과 푸앙이를 그대로 한 장으로 저장한다. 푸앙이를 놓기 전에는 저장하지 않는다.
 * AR 을 시작하지 못하면 일반 촬영으로 안내한다.
 */
@Composable
fun WriteArRoute(
    placeId: String,
    placeName: String,
    drafts: DraftStore,
    onBack: () -> Unit,
    onSwitchToPhoto: () -> Unit,
    onPhotoReady: () -> Unit,
) {
    val context = LocalContext.current
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

    LightSystemBarIcons()
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)
    val view = rememberView(engine)
    val collisionSystem = rememberCollisionSystem(view)
    val childNodes = rememberNodes()
    val puang = remember(modelLoader) {
        ModelNode(modelInstance = modelLoader.createModelInstance(PUANG_MODEL), scaleToUnits = PUANG_HEIGHT_METERS).apply {
            isShadowCaster = true
        }
    }
    var anchorNode by remember { mutableStateOf<AnchorNode?>(null) }
    var sceneView by remember { mutableStateOf<ARSceneView?>(null) }
    var sceneOrigin by remember { mutableStateOf(Offset.Zero) }
    var frameInRoot by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    var hint by remember { mutableIntStateOf(R.string.write_ar_hint_place) }
    var capturing by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<Int?>(null) }

    /** 촬영 칸의 AR 화면 안 위치 (px) */
    fun frameBounds(): Rect? = frameInRoot?.let {
        Rect(
            (it.left - sceneOrigin.x).roundToInt(),
            (it.top - sceneOrigin.y).roundToInt(),
            (it.right - sceneOrigin.x).roundToInt(),
            (it.bottom - sceneOrigin.y).roundToInt(),
        )
    }

    /** 화면 좌표 [x],[y] 가 가리키는 바닥에 푸앙이를 놓는다. 바닥을 아직 못 찾았으면 false */
    fun placeAt(x: Float, y: Float): Boolean {
        val arView = sceneView ?: return false
        val frame = arView.frame ?: return false
        if (frame.camera.trackingState != TrackingState.TRACKING) return false
        val hit = frame.hitTest(x, y).firstOrNull {
            it.isValid(planeTypes = setOf(Plane.Type.HORIZONTAL_UPWARD_FACING), point = false, depthPoint = false)
        } ?: return false
        // 바닥 위 위치의 +Z 는 휴대폰 쪽을 향하고, 푸앙이 모델의 앞도 +Z 라 그대로 놓으면 카메라를 바라본다
        val anchor = hit.createAnchorOrNull() ?: return false
        val current = anchorNode
        if (current == null) {
            val node = AnchorNode(engine = engine, anchor = anchor).apply { addChildNode(puang) }
            anchorNode = node
            childNodes += node
        } else {
            val old = current.anchor
            current.anchor = anchor
            old.detach()
        }
        arView.planeRenderer.isVisible = false
        return true
    }

    fun onFrameTap(local: Offset, reposition: Boolean) {
        if (anchorNode != null && !reposition) return
        val bounds = frameBounds() ?: return
        if (!placeAt(bounds.left + local.x, bounds.top + local.y)) hint = R.string.write_ar_hint_scan
    }

    fun capture() {
        val arView = sceneView
        val bounds = frameBounds()
        when {
            !granted -> permission.launch(Manifest.permission.CAMERA)
            capturing || arView == null || bounds == null -> Unit
            anchorNode == null -> error = R.string.write_ar_place_first
            else -> {
                capturing = true
                val screen = createBitmap(arView.width, arView.height)
                PixelCopy.request(arView, screen, { result ->
                    if (result != PixelCopy.SUCCESS) {
                        screen.recycle()
                        capturing = false
                        error = R.string.write_capture_failed
                        return@request
                    }
                    scope.launch {
                        val saved = withContext(Dispatchers.IO) {
                            runCatching {
                                val crop = Rect(bounds)
                                if (!crop.intersect(0, 0, screen.width, screen.height)) return@runCatching false
                                val photo = Bitmap.createBitmap(screen, crop.left, crop.top, crop.width(), crop.height())
                                drafts.captureTempFile(placeId).outputStream().use { photo.compress(Bitmap.CompressFormat.JPEG, 92, it) }
                            }.getOrDefault(false)
                        }
                        screen.recycle()
                        if (saved) {
                            drafts.commitCapture(placeId)
                            capturing = false
                            onPhotoReady()
                        } else {
                            capturing = false
                            error = R.string.write_capture_failed
                        }
                    }
                }, Handler(Looper.getMainLooper()))
            }
        }
    }

    ArCaptureScreen(
        placeName = placeName,
        onBack = onBack,
        onHelp = { showHelp = true },
        onCapture = ::capture,
        onSwitchToPhoto = onSwitchToPhoto,
        background = {
            if (granted && !failed) {
                ARScene(
                    modifier = Modifier.matchParentSize().onGloballyPositioned { sceneOrigin = it.positionInRoot() },
                    engine = engine,
                    modelLoader = modelLoader,
                    materialLoader = materialLoader,
                    view = view,
                    collisionSystem = collisionSystem,
                    childNodes = childNodes,
                    planeRenderer = true,
                    sessionConfiguration = { _, config ->
                        config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL
                        config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
                        config.focusMode = Config.FocusMode.AUTO
                    },
                    onSessionFailed = { failed = true },
                    onViewCreated = {
                        sceneView = this
                        // 푸앙이 그림자가 인식한 바닥에 드리워지게
                        planeRenderer.isShadowReceiver = true
                        mainLightNode?.isShadowCaster = true
                    },
                )
            } else {
                Box(Modifier.matchParentSize().background(RecordingNavy))
            }
        },
        arContent = {
            val description = stringResource(R.string.write_ar_frame)
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(EDITOR_ASPECT)
                    .border(2.dp, Color.White.copy(alpha = .7f), RoundedCornerShape(16.dp))
                    .onGloballyPositioned { frameInRoot = it.boundsInRoot() }
                    .semantics { contentDescription = description }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { onFrameTap(it, reposition = false) },
                            onLongPress = { onFrameTap(it, reposition = true) },
                        )
                    },
            ) {
                val message = when {
                    !granted -> R.string.write_ar_camera_permission
                    anchorNode == null -> hint
                    else -> null
                }
                if (message != null) {
                    Text(
                        stringResource(message),
                        Modifier
                            .align(Alignment.Center)
                            .padding(24.dp)
                            .background(Color(0xCC161616), RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        color = Color.White, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
                    )
                }
            }
        },
    )

    if (failed) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.write_ar_failed_title)) },
            text = { Text(stringResource(R.string.write_ar_failed_body)) },
            confirmButton = { TextButton(onClick = onSwitchToPhoto) { Text(stringResource(R.string.write_ar_photo)) } },
            dismissButton = { TextButton(onClick = onBack) { Text(stringResource(R.string.write_cancel)) } },
        )
    }
    if (showHelp) MessageDialog(R.string.write_ar_help_title, R.string.write_ar_help_body) { showHelp = false }
    error?.let { MessageDialog(null, it) { error = null } }
}
