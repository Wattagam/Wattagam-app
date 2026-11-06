package com.wattagam.app.feature.write

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import com.wattagam.app.R
import com.wattagam.app.data.draft.DraftStore
import com.wattagam.app.feature.guestbook.DrawingColor
import com.wattagam.app.feature.guestbook.DrawingTool
import com.wattagam.app.feature.guestbook.PhotoEditorScreen
import com.wattagam.app.feature.guestbook.PhotoEditorUiState
import com.wattagam.app.feature.guestbook.PublishLocation
import com.wattagam.app.feature.guestbook.PublishScreen
import com.wattagam.app.feature.guestbook.PublishStatus
import com.wattagam.app.feature.guestbook.PublishUiState
import com.wattagam.app.feature.place.rememberVisitCheck
import com.wattagam.app.ui.components.LocationUiState
import com.wattagam.app.ui.components.WattagamTextField
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TEXT_MAX_LENGTH = 30

/** 글자는 사진 왼쪽 아래에 한 줄 넣는다 */
private val TextPosition = Offset(0.04f, 0.95f)

/**
 * 06-A · 사진 꾸미기. 펜·지우개·색으로 사진 위에 그리고 글자 한 줄을 넣을 수 있다.
 * '다음'을 누르면 사진과 그림·글자를 한 장으로 합쳐 초안의 꾸민 사진으로 저장한다.
 */
@Composable
fun WriteEditorRoute(
    placeId: String,
    placeName: String,
    drafts: DraftStore,
    onBack: () -> Unit,
    onDone: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val photo by rememberPhoto(drafts.photoFile(placeId))
    LightSystemBarIcons()
    val strokes = remember { mutableStateListOf<DrawnStroke>() }
    var text by rememberSaveable { mutableStateOf("") }
    var tool by rememberSaveable { mutableStateOf(DrawingTool.Pen) }
    var color by rememberSaveable { mutableStateOf(DrawingColor.Yellow) }
    var editingText by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<Int?>(null) }
    val texts = if (text.isBlank()) emptyList() else listOf(PlacedText(text, TextPosition))

    PhotoEditorScreen(
        state = PhotoEditorUiState(placeName, tool, color),
        onBack = onBack,
        onClear = { strokes.clear() },
        onTool = { tool = it },
        onColor = { color = it; tool = DrawingTool.Pen },
        onAddText = { editingText = text },
        onDone = {
            if (!saving && photo != null) {
                saving = true
                val strokeSnapshot = strokes.toList()
                val textSnapshot = texts
                scope.launch {
                    val saved = withContext(Dispatchers.IO) {
                        runCatching {
                            val source = decodePhoto(drafts.photoFile(placeId), OUTPUT_MAX_SIDE) ?: return@runCatching false
                            val output = composeDecoratedPhoto(source, strokeSnapshot, textSnapshot)
                            drafts.decoratedFile(placeId).outputStream().use { output.compress(Bitmap.CompressFormat.JPEG, 90, it) }
                        }.getOrDefault(false)
                    }
                    saving = false
                    if (saved) onDone() else error = R.string.write_save_failed
                }
            }
        },
        canvas = { modifier ->
            DrawingCanvas(photo, strokes, texts, tool, color, stringResource(R.string.bc_selected_photo), modifier)
        },
    )

    editingText?.let { draft ->
        AlertDialog(
            onDismissRequest = { editingText = null },
            title = { Text(stringResource(R.string.write_text_title)) },
            text = {
                WattagamTextField(
                    value = draft,
                    onValueChange = { editingText = it.take(TEXT_MAX_LENGTH) },
                    label = stringResource(R.string.write_text_label, TEXT_MAX_LENGTH),
                )
            },
            confirmButton = { TextButton(onClick = { text = draft.trim(); editingText = null }) { Text(stringResource(R.string.write_ok)) } },
            dismissButton = { TextButton(onClick = { editingText = null }) { Text(stringResource(R.string.write_cancel)) } },
        )
    }
    error?.let { MessageDialog(null, it) { error = null } }
}

/**
 * 07 · 공개 업로드. 메시지는 초안 파일에 계속 저장한다.
 * '등록'을 누르면 현재 위치를 다시 재서 범위 안일 때만 업로드로 넘어간다 (작성 시작 때 확인했어도 다시 확인).
 * 업로드 API 연결 전이라 범위 안이면 '아직 등록되지 않음'을 알리고 초안을 그대로 둔다.
 */
@Composable
fun WritePublishRoute(
    placeId: String,
    placeName: String,
    authorName: String,
    stampCount: Int,
    latitude: Double,
    longitude: Double,
    drafts: DraftStore,
    onBack: () -> Unit,
    onEditPhoto: () -> Unit,
) {
    val photoFile by produceState<java.io.File?>(null, placeId) { value = withContext(Dispatchers.IO) { drafts.publishPhoto(placeId) } }
    val photo by rememberPhoto(photoFile)
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(placeId) { if (message == null) message = drafts.readMessage(placeId) }
    // 바뀔 때마다 초안 파일에 저장 (화면을 바로 떠나도 남도록 기다리지 않는다)
    LaunchedEffect(message) {
        val current = message ?: return@LaunchedEffect
        drafts.writeMessage(placeId, current)
    }

    LightSystemBarIcons()
    var publishRequested by remember { mutableStateOf(false) }
    var showUploadPending by remember { mutableStateOf(false) }
    val check = rememberVisitCheck(latitude, longitude, onAllowed = {
        if (publishRequested) {
            publishRequested = false
            showUploadPending = true
        }
    })
    // 다시 확인하는 동안에는 직전 판정을 보여준다
    var location by rememberSaveable { mutableStateOf(PublishLocation.Unverified) }
    var showProblem by remember { mutableStateOf(false) }
    LaunchedEffect(check.status) {
        when (check.status) {
            LocationUiState.Available -> location = PublishLocation.Verified
            LocationUiState.Outside -> { location = PublishLocation.Outside; publishRequested = false }
            LocationUiState.Denied, LocationUiState.Unavailable -> { location = PublishLocation.Unverified; publishRequested = false; showProblem = true }
            LocationUiState.Checking, null -> Unit
        }
    }

    PublishScreen(
        state = PublishUiState(
            placeName = placeName,
            authorName = authorName,
            message = message.orEmpty(),
            stampCount = stampCount,
            status = if (check.checking) PublishStatus.CheckingLocation else PublishStatus.Ready,
            location = location,
        ),
        onBack = onBack,
        onMessageChange = { message = it },
        onEditPhoto = onEditPhoto,
        onRecheck = check.start,
        onPublish = {
            if (!check.checking) {
                publishRequested = true
                check.start()
            }
        },
        photo = { modifier ->
            Box(modifier.background(Color.Black)) {
                photo?.let { Image(it, stringResource(R.string.bc_selected_photo), Modifier.matchParentSize(), contentScale = ContentScale.Crop) }
            }
        },
    )

    if (showUploadPending) MessageDialog(R.string.write_upload_pending_title, R.string.write_upload_pending_body) { showUploadPending = false }
    if (showProblem) {
        val body = if (check.status == LocationUiState.Denied) R.string.location_denied_body else R.string.location_unavailable_body
        MessageDialog(null, body) { showProblem = false }
    }
}
