package com.wattagam.app.feature.capture

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wattagam.app.R
import com.wattagam.app.ui.components.*
import com.wattagam.app.ui.theme.*

enum class CaptureMode { Ar, Photo }

@Composable
private fun CameraLayout(title: String, onBack: () -> Unit, modifier: Modifier, background: @Composable BoxScope.() -> Unit, onHelp: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Box(modifier.fillMaxSize().background(RecordingNavy)) {
        background()
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            RecordingTopBar(title, onBack, dark = true, action = {
                if (onHelp != null) TextButton(onClick = onHelp) { Text(stringResource(R.string.bc_help), color = Color.White) }
            })
            content()
        }
    }
}

@Composable
fun CaptureModeScreen(stampCount: Int, onBack: () -> Unit, onHelp: () -> Unit, onSelectMode: (CaptureMode) -> Unit, modifier: Modifier = Modifier,
    background: @Composable BoxScope.() -> Unit, character: @Composable () -> Unit,
) {
    CameraLayout(stringResource(R.string.bc_create_title), onBack, modifier, background, onHelp) {
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val contentHeight = maxHeight
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).heightIn(min = contentHeight).padding(24.dp), verticalArrangement = Arrangement.SpaceBetween, horizontalAlignment = Alignment.CenterHorizontally) {
                RecordingBadge(stringResource(R.string.bc_capture_verified, stampCount), dark = true, solidDark = true, icon = R.drawable.bc_check_light)
                Box(Modifier.padding(vertical = 24.dp)) { character() }
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Color.White).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.bc_choose_mode), color = WattagamColors.Ink, style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(R.string.bc_choose_mode_body), style = MaterialTheme.typography.bodyMedium, color = WattagamColors.Muted)
                    WattagamButton(stringResource(R.string.bc_choose_ar), { onSelectMode(CaptureMode.Ar) }, Modifier.fillMaxWidth())
                    WattagamButton(stringResource(R.string.bc_choose_photo), { onSelectMode(CaptureMode.Photo) }, Modifier.fillMaxWidth(), kind = ButtonKind.Secondary)
                }
            }
        }
    }
}

@Composable
fun ArCaptureScreen(placeName: String, onBack: () -> Unit, onHelp: () -> Unit, onCapture: () -> Unit, onSwitchToPhoto: () -> Unit,
    modifier: Modifier = Modifier, background: @Composable BoxScope.() -> Unit, arContent: @Composable () -> Unit,
) {
    CameraLayout(placeName, onBack, modifier, background, onHelp) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            RecordingBadge(stringResource(R.string.bc_ar_placed, placeName), dark = true, solidDark = true, icon = R.drawable.bc_scan)
            arContent()
            Row(Modifier.clip(WattagamShapes.Pill).background(RecordingNavy).padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.bc_ar_mode), Modifier.clip(WattagamShapes.Pill).background(WattagamColors.Primary).padding(16.dp, 12.dp), color = Color.White, style = MaterialTheme.typography.labelMedium)
                TextButton(onClick = onSwitchToPhoto) { Text(stringResource(R.string.bc_photo_mode), color = Color.White) }
            }
            WattagamButton(stringResource(R.string.bc_take_photo), onCapture, Modifier.fillMaxWidth())
            RecordingCaption(stringResource(R.string.bc_ar_reposition), color = Color.White)
        }
    }
}

@Composable
fun PhotoCaptureScreen(placeName: String, onBack: () -> Unit, onCapture: () -> Unit, onGallery: () -> Unit, onFlash: () -> Unit,
    modifier: Modifier = Modifier, flashEnabled: Boolean = false, background: @Composable BoxScope.() -> Unit,
) {
    CameraLayout(stringResource(R.string.bc_photo_title, placeName), onBack, modifier, background) {
        RecordingBadge(stringResource(R.string.bc_photo_verified), Modifier.padding(24.dp), icon = R.drawable.bc_pin)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            Box(Modifier.align(Alignment.Center).size(40.dp)) {
                Box(Modifier.align(Alignment.Center).size(40.dp, 2.dp).background(Color.White.copy(alpha = .5f)))
                Box(Modifier.align(Alignment.Center).size(2.dp, 40.dp).background(Color.White.copy(alpha = .5f)))
            }
            Column(Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp), verticalArrangement = Arrangement.spacedBy(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                    IconButton(onClick = onGallery, modifier = Modifier.size(56.dp).background(Color.White.copy(alpha = .2f), CircleShape)) {
                        Image(painterResource(R.drawable.bc_image_light), stringResource(R.string.bc_gallery_change), Modifier.size(24.dp))
                    }
                    IconButton(onClick = onCapture, modifier = Modifier.size(80.dp)) { Image(painterResource(R.drawable.bc_shutter), stringResource(R.string.bc_take_photo), Modifier.requiredSize(104.dp)) }
                    IconToggleButton(checked = flashEnabled, onCheckedChange = { onFlash() }, modifier = Modifier.size(56.dp).background(if (flashEnabled) WattagamColors.Primary else Color.White.copy(alpha = .2f), CircleShape)) {
                        Image(painterResource(R.drawable.bc_flash), stringResource(R.string.bc_flash), Modifier.size(24.dp))
                    }
                }
                RecordingCaption(stringResource(R.string.bc_capture_next), color = Color.White)
            }
        }
    }
}

@Composable
fun PhotoPreviewScreen(placeName: String, photo: Painter, onBack: () -> Unit, onRetake: () -> Unit, onGallery: () -> Unit, onEdit: () -> Unit, onNext: () -> Unit, modifier: Modifier = Modifier) {
    RecordingScaffold(stringResource(R.string.bc_preview_title), onBack, modifier, bottom = {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(4.dp, 24.dp).background(WattagamColors.Primary, RoundedCornerShape(2.dp)))
            RecordingCaption(stringResource(R.string.bc_preview_next))
        }
        WattagamButton(stringResource(R.string.bc_decorate), onEdit, Modifier.fillMaxWidth())
        WattagamButton(stringResource(R.string.bc_skip_decorate), onNext, Modifier.fillMaxWidth(), kind = ButtonKind.Secondary)
    }) {
        CreationSteps(0)
        RecordingBadge(stringResource(R.string.bc_one_photo, placeName), icon = R.drawable.bc_image)
        Text(stringResource(R.string.bc_preview_heading), style = MaterialTheme.typography.headlineLarge)
        RecordingPhoto(photo, stringResource(R.string.bc_selected_photo), Modifier.fillMaxWidth().height(320.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            WattagamButton(stringResource(R.string.bc_retake), onRetake, Modifier.weight(1f), kind = ButtonKind.Secondary)
            WattagamButton(stringResource(R.string.bc_gallery_change), onGallery, Modifier.weight(1f), kind = ButtonKind.Secondary)
        }
        RecordingCaption(stringResource(R.string.bc_decoration_optional))
    }
}
