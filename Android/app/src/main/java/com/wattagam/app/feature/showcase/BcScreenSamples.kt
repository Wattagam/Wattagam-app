package com.wattagam.app.feature.showcase

import android.app.Activity
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.wattagam.app.R
import com.wattagam.app.feature.capture.*
import com.wattagam.app.feature.guestbook.*
import com.wattagam.app.ui.components.*
import com.wattagam.app.ui.theme.*
import kotlinx.serialization.Serializable

@Serializable
enum class BcSample(@param:StringRes val title: Int) {
    Visit(R.string.bc_sample_visit), Outside(R.string.bc_sample_outside), Mode(R.string.bc_sample_mode),
    Ar(R.string.bc_sample_ar), Camera(R.string.bc_sample_camera), Photo(R.string.bc_sample_photo),
    Editor(R.string.bc_sample_editor), Publish(R.string.bc_sample_publish), Failure(R.string.bc_sample_failure), Success(R.string.bc_sample_success),
}

@Composable
fun BcSampleIndex(onBack: () -> Unit, onSelect: (BcSample) -> Unit, modifier: Modifier = Modifier) {
    RecordingScaffold(stringResource(R.string.bc_samples_title), onBack, modifier) {
        Text(stringResource(R.string.bc_sample_notice), style = MaterialTheme.typography.bodyMedium, color = WattagamColors.Muted)
        BcSample.entries.forEach { sample -> WattagamButton(stringResource(sample.title), { onSelect(sample) }, Modifier.fillMaxWidth(), kind = ButtonKind.Secondary) }
    }
}

@Composable
fun BcSampleScreen(sample: BcSample, onBack: () -> Unit, onNavigate: (BcSample) -> Unit, modifier: Modifier = Modifier) {
    val place = stringResource(R.string.bc_sample_place)
    val author = stringResource(R.string.bc_sample_author)
    val initialMessage = stringResource(R.string.bc_sample_message)
    var message by rememberSaveable(sample) { mutableStateOf(initialMessage) }
    var tool by rememberSaveable { mutableStateOf(DrawingTool.Pen) }
    var color by rememberSaveable { mutableStateOf(DrawingColor.Yellow) }
    var drawing by rememberSaveable { mutableStateOf(true) }
    var flash by rememberSaveable { mutableStateOf(false) }
    var notice by rememberSaveable { mutableStateOf(false) }
    val action = { notice = true }
    val dark = sample in listOf(BcSample.Mode, BcSample.Ar, BcSample.Camera, BcSample.Editor)
    val view = LocalView.current
    if (!view.isInEditMode) {
        DisposableEffect(dark, view) {
            val window = (view.context as? Activity)?.window
            val controller = window?.let { WindowCompat.getInsetsController(it, view) }
            val previousStatus = controller?.isAppearanceLightStatusBars ?: true
            val previousNavigation = controller?.isAppearanceLightNavigationBars ?: true
            controller?.isAppearanceLightStatusBars = !dark
            controller?.isAppearanceLightNavigationBars = !dark
            onDispose {
                controller?.isAppearanceLightStatusBars = previousStatus
                controller?.isAppearanceLightNavigationBars = previousNavigation
            }
        }
    }
    Box(modifier.fillMaxSize()) {
        when (sample) {
            BcSample.Visit, BcSample.Outside -> VisitCheckScreen(
                VisitCheckUiState(place, author, 1, if (sample == BcSample.Visit) VisitCheckStatus.Available else VisitCheckStatus.Outside),
                onBack, action, { onNavigate(BcSample.Mode) }, action,
                mapContent = { VisitMapIllustration(place, sample == BcSample.Outside) })
            BcSample.Mode -> CaptureModeScreen(1, onBack, action, { onNavigate(if (it == CaptureMode.Ar) BcSample.Ar else BcSample.Camera) },
                background = { CameraSampleBackground() }, character = { PuangSample(false) })
            BcSample.Ar -> ArCaptureScreen(place, onBack, action, action, { onNavigate(BcSample.Camera) },
                background = { CameraSampleBackground() }, arContent = { PuangSample(true) })
            BcSample.Camera -> PhotoCaptureScreen(place, onBack, action, action, { flash = !flash }, flashEnabled = flash,
                background = { CameraSampleBackground() })
            BcSample.Photo -> PhotoPreviewScreen(place, painterResource(R.drawable.bc_photo_sample), onBack, { onNavigate(BcSample.Mode) }, action,
                { onNavigate(BcSample.Editor) }, { onNavigate(BcSample.Publish) })
            BcSample.Editor -> PhotoEditorScreen(PhotoEditorUiState(place, tool, color), onBack, { drawing = false }, { tool = it }, { color = it }, action,
                { onNavigate(BcSample.Publish) }, canvas = { DecoratedPhotoSample(Modifier.fillMaxWidth().height(336.dp), DecorationSample.Editor, drawing) })
            BcSample.Publish, BcSample.Failure -> PublishScreen(
                PublishUiState(place, author, message, 1, if (sample == BcSample.Failure) PublishStatus.Failed else PublishStatus.Ready),
                onBack, { message = it }, { onNavigate(BcSample.Editor) }, action, action,
                photo = { DecoratedPhotoSample(it, if (sample == BcSample.Failure) DecorationSample.Failure else DecorationSample.Publish) })
            BcSample.Success -> PublishSuccessScreen(PublishSuccessUiState(place, author, stringResource(R.string.bc_sample_date), initialMessage, 2, true), onBack, action, action,
                photo = { DecoratedPhotoSample(it, DecorationSample.Success) })
        }
        Text(stringResource(R.string.bc_sample_label), Modifier.align(Alignment.TopCenter).clip(RoundedCornerShape(4.dp)).background(if (dark) RecordingNavy else WattagamColors.PrimarySoft).padding(horizontal = 6.dp),
            color = if (dark) Color.White else WattagamColors.Primary, style = MaterialTheme.typography.labelSmall)
    }
    if (notice) AlertDialog(onDismissRequest = { notice = false }, title = { Text(stringResource(R.string.bc_sample_dialog_title)) },
        text = { Text(stringResource(R.string.bc_sample_dialog_body)) },
        confirmButton = { TextButton(onClick = { notice = false }) { Text(stringResource(R.string.bc_confirm)) } })
}

@Composable
private fun BoxScope.CameraSampleBackground() {
    Image(painterResource(R.drawable.bc_camera_sample), stringResource(R.string.bc_camera_sample_description), Modifier.matchParentSize(), contentScale = ContentScale.Crop)
    Box(Modifier.matchParentSize().background(RecordingNavy.copy(alpha = .2f)))
}

@Composable
private fun PuangSample(ar: Boolean) {
    Box(Modifier.size(280.dp, 296.dp)) {
        Image(painterResource(R.drawable.bc_puang), stringResource(R.string.bc_puang_sample_description),
            if (ar) Modifier.offset(44.dp, 3.dp).size(192.dp, 238.dp) else Modifier.size(280.dp), contentScale = ContentScale.Fit)
        Image(painterResource(R.drawable.bc_ring), null, Modifier.offset(y = 204.dp).size(280.dp, 58.dp))
        RecordingCaption(stringResource(if (ar) R.string.bc_ar_position else R.string.bc_puang_place), Modifier.align(Alignment.BottomCenter), color = Color.White)
    }
}

private enum class DecorationSample { Editor, Publish, Failure, Success }

@Composable
private fun DecoratedPhotoSample(modifier: Modifier, mode: DecorationSample, drawing: Boolean = true) {
    BoxWithConstraints(modifier.clip(WattagamShapes.Card)) {
        val scale = maxWidth / 364
        val innerHeight = if (mode == DecorationSample.Editor) 420.dp else maxHeight
        Image(painterResource(R.drawable.bc_photo_sample), stringResource(R.string.bc_selected_photo), Modifier.fillMaxWidth().height(innerHeight), contentScale = ContentScale.Crop)
        if (drawing) {
            val asset = when (mode) {
                DecorationSample.Editor -> R.drawable.bc_drawing_editor
                DecorationSample.Publish -> R.drawable.bc_drawing_publish
                DecorationSample.Failure -> R.drawable.bc_drawing_failure
                DecorationSample.Success -> R.drawable.bc_drawing_success
            }
            val y = when (mode) { DecorationSample.Editor -> 60.3f; DecorationSample.Publish -> 21.15f; DecorationSample.Failure -> 28.5f; DecorationSample.Success -> 38.07f }
            val h = when (mode) { DecorationSample.Editor -> 227.877f; DecorationSample.Publish -> 102.006f; DecorationSample.Failure -> 120.634f; DecorationSample.Success -> 173.625f }
            Image(painterResource(asset), null, Modifier.offset(x = scale * if (mode == DecorationSample.Failure) 26 else 24, y = y.dp).size(scale * if (mode == DecorationSample.Failure) 285.011f else 287f, h.dp), contentScale = ContentScale.FillBounds)
        }
        val captionY = when (mode) { DecorationSample.Editor -> 291.dp; DecorationSample.Publish -> 116.dp; DecorationSample.Failure -> 152.dp; DecorationSample.Success -> 164.dp }
        Text(stringResource(R.string.bc_sample_caption), Modifier.offset(12.dp, captionY).clip(RoundedCornerShape(8.dp)).background(RecordingNavy).padding(10.dp, 5.dp), color = Color.White,
            style = if (mode == DecorationSample.Editor) MaterialTheme.typography.labelLarge else MaterialTheme.typography.labelSmall)
    }
}

@Preview(name = "04 · GPS 확인", group = "B · 촬영", widthDp = 412, heightDp = 916, showSystemUi = true)
@Preview(name = "04 · 큰 글자", group = "B · 접근성", widthDp = 320, heightDp = 720, fontScale = 1.5f)
@Composable private fun VisitPreview() { WattagamTheme { BcSampleScreen(BcSample.Visit, {}, {}) } }
@Preview(name = "04-E · 범위 밖", group = "B · 촬영", widthDp = 412, heightDp = 916, showSystemUi = true)
@Composable private fun OutsidePreview() { WattagamTheme { BcSampleScreen(BcSample.Outside, {}, {}) } }
@Preview(name = "05 · 촬영 선택", group = "B · 촬영", widthDp = 412, heightDp = 916, showSystemUi = true)
@Composable private fun ModePreview() { WattagamTheme { BcSampleScreen(BcSample.Mode, {}, {}) } }
@Preview(name = "05-A · AR 촬영", group = "B · 촬영", widthDp = 412, heightDp = 916, showSystemUi = true)
@Composable private fun ArPreview() { WattagamTheme { BcSampleScreen(BcSample.Ar, {}, {}) } }
@Preview(name = "05-B · 일반 촬영", group = "B · 촬영", widthDp = 412, heightDp = 916, showSystemUi = true)
@Composable private fun CameraPreview() { WattagamTheme { BcSampleScreen(BcSample.Camera, {}, {}) } }
@Preview(name = "06 · 사진 확인", group = "B · 촬영", widthDp = 412, heightDp = 916, showSystemUi = true)
@Composable private fun PhotoPreview() { WattagamTheme { BcSampleScreen(BcSample.Photo, {}, {}) } }
@Preview(name = "06-A · 큰 글자", group = "C · 접근성", widthDp = 320, heightDp = 720, fontScale = 1.5f)
@Preview(name = "06-A · 사진 꾸미기", group = "C · 업로드", widthDp = 412, heightDp = 916, showSystemUi = true)
@Composable private fun EditorPreview() { WattagamTheme { BcSampleScreen(BcSample.Editor, {}, {}) } }
@Preview(name = "07 · 공개 업로드", group = "C · 업로드", widthDp = 412, heightDp = 916, showSystemUi = true)
@Preview(name = "07 · 큰 글자", group = "C · 접근성", widthDp = 320, heightDp = 720, fontScale = 1.5f)
@Composable private fun PublishPreview() { WattagamTheme { BcSampleScreen(BcSample.Publish, {}, {}) } }
@Preview(name = "07-E · 업로드 실패", group = "C · 업로드", widthDp = 412, heightDp = 916, showSystemUi = true)
@Composable private fun FailurePreview() { WattagamTheme { BcSampleScreen(BcSample.Failure, {}, {}) } }
@Preview(name = "08 · 업로드 성공", group = "C · 업로드", widthDp = 412, heightDp = 916, showSystemUi = true)
@Composable private fun SuccessPreview() { WattagamTheme { BcSampleScreen(BcSample.Success, {}, {}) } }
