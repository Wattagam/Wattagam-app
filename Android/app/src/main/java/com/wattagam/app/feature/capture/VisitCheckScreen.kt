package com.wattagam.app.feature.capture

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.wattagam.app.R
import com.wattagam.app.ui.components.*
import com.wattagam.app.ui.theme.*

enum class VisitCheckStatus { Available, Outside }

/** [placeOrder] 코스 안 순서, [placeStamped] 이 장소 도장을 이미 받았는지 (안내 문구가 달라진다) */
data class VisitCheckUiState(
    val placeName: String,
    val visitorName: String,
    val stamps: Int,
    val status: VisitCheckStatus,
    val placeOrder: Int = 1,
    val placeStamped: Boolean = false,
    /** 등록된 장소 수 (서버 기준) */
    val stampTotal: Int = 4,
)

@Composable
fun VisitCheckScreen(state: VisitCheckUiState, onBack: () -> Unit, onRecheck: () -> Unit, onContinue: () -> Unit, onMap: () -> Unit, modifier: Modifier = Modifier,
    mapContent: @Composable () -> Unit,
) {
    val available = state.status == VisitCheckStatus.Available
    RecordingScaffold(stringResource(R.string.bc_visit_title), onBack, modifier, bottom = {
        if (available) {
            RecordingCaption(stringResource(R.string.bc_ar_support))
            WattagamButton(stringResource(R.string.bc_start), onContinue, Modifier.fillMaxWidth())
        } else {
            WattagamButton(stringResource(R.string.bc_recheck_location), onRecheck, Modifier.fillMaxWidth())
            WattagamButton(stringResource(R.string.bc_return_map), onMap, Modifier.fillMaxWidth(), kind = ButtonKind.Secondary)
        }
    }) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                RecordingBadge(stringResource(if (available) R.string.bc_available else R.string.bc_outside), danger = !available, accent = available, icon = if (available) R.drawable.bc_check else null)
                RecordingHeading(stringResource(if (available) R.string.bc_visit_done else R.string.bc_visit_closer))
                Text(if (available) stringResource(R.string.bc_visit_welcome, state.visitorName) else stringResource(R.string.bc_visit_outside_body, state.placeName), style = MaterialTheme.typography.bodyMedium, color = WattagamColors.Muted)
            }
            androidx.compose.foundation.Image(androidx.compose.ui.res.painterResource(R.drawable.bc_puang), stringResource(R.string.bc_puang), Modifier.size(70.dp, 88.dp))
        }
        Spacer(Modifier.height(2.dp))
        mapContent()
        val stampLine = stringResource(if (state.placeStamped) R.string.bc_stamp_owned else R.string.bc_stamp_missing, state.stamps, state.placeName, state.stampTotal)
        Column(Modifier.fillMaxWidth().clip(WattagamShapes.Card).background(WattagamColors.Soft).padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RecordingIcon(R.drawable.bc_pin, tint = WattagamColors.Ink)
                Column { Text(state.placeName, style = MaterialTheme.typography.titleLarge); RecordingCaption(if (available) stringResource(R.string.bc_course, state.placeOrder) else stampLine) }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RecordingIcon(if (available) R.drawable.bc_check_circle else R.drawable.bc_pin_off, size = 16.dp)
                RecordingCaption(stringResource(if (available) R.string.bc_visit_inside else R.string.bc_visit_outside), Modifier.weight(1f), color = if (available) WattagamColors.Primary else WattagamColors.Danger)
                if (available) TextButton(onClick = onRecheck, contentPadding = PaddingValues(0.dp)) { Text(stringResource(R.string.bc_recheck), style = MaterialTheme.typography.bodySmall) }
            }
        }
        when {
            !available -> RecordingNotice(stringResource(R.string.bc_draft_kept), stringResource(R.string.bc_draft_kept_body), R.drawable.bc_draft)
            // 이미 받은 장소에 '아직 도장은 찍히지 않았어요'라고 하지 않는다
            state.placeStamped -> RecordingNotice(stampLine, stringResource(R.string.bc_stamp_owned_body), R.drawable.bc_stamp)
            else -> RecordingNotice(stringResource(R.string.bc_stamp_pending), stringResource(R.string.bc_stamp_pending_body, state.stamps, state.placeName, state.stampTotal), R.drawable.bc_stamp, highlighted = true)
        }
        RecordingCaption(stringResource(if (available) R.string.bc_gps_help else R.string.bc_outside_help))
    }
}

@Composable
fun VisitMapIllustration(placeName: String, outside: Boolean, modifier: Modifier = Modifier) {
    val largeText = LocalDensity.current.fontScale > 1.2f
    BoxWithConstraints(modifier.fillMaxWidth().height(if (largeText) 260.dp else 196.dp).clip(WattagamShapes.Card).background(Color(0xFFF0F4F0))) {
        val unit = maxWidth / 364
        Box(Modifier.offset(y = 132.dp).fillMaxWidth().height(26.dp).background(Color.White))
        Box(Modifier.offset(x = unit * 86).width(unit * 24).fillMaxHeight().background(Color.White))
        Box(Modifier.offset(x = unit * 22, y = 26.dp).size(unit * 64, 65.dp).background(Color(0xFFD8E0DF), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) { RecordingCaption(stringResource(R.string.bc_main_building)) }
        Box(Modifier.offset(x = unit * 152, y = 26.dp).size(unit * 137, 80.dp).background(Color(0xFFCCD9E9), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Text(placeName, Modifier.padding(4.dp), style = if (largeText) MaterialTheme.typography.bodySmall else MaterialTheme.typography.labelMedium) }
        Box(Modifier.offset(x = unit * 192, y = 107.dp).size(unit * 58, 18.dp).background(WattagamColors.Border, RoundedCornerShape(4.dp)))
        RecordingIcon(R.drawable.bc_position, Modifier.offset(x = unit * if (outside) 31 else 207, y = 132.dp), size = 26.dp)
        RecordingCaption(stringResource(R.string.bc_current_location), Modifier.offset(x = unit * if (outside) 12 else 235, y = if (outside) 103.dp else 138.dp).background(Color.White, RoundedCornerShape(8.dp)).padding(horizontal = 4.dp), color = WattagamColors.Primary)
        RecordingCaption(stringResource(R.string.bc_map_caption), Modifier.align(Alignment.BottomStart).padding(16.dp), color = WattagamColors.Muted)
    }
}
