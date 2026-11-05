package com.wattagam.app.feature.guestbook

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.wattagam.app.R
import com.wattagam.app.ui.components.*
import com.wattagam.app.ui.theme.*

enum class PublishStatus { Ready, CheckingLocation, Uploading, Failed }
enum class PublishLocation { Verified, Unverified, Outside }

data class PublishUiState(
    val placeName: String,
    val authorName: String,
    val message: String,
    val stampCount: Int,
    val status: PublishStatus = PublishStatus.Ready,
    val location: PublishLocation = PublishLocation.Verified,
    val messageLimit: Int = 80,
)

@Composable
fun PublishScreen(state: PublishUiState, onBack: () -> Unit, onMessageChange: (String) -> Unit, onEditPhoto: () -> Unit, onRecheck: () -> Unit, onPublish: () -> Unit,
    modifier: Modifier = Modifier, photo: @Composable (Modifier) -> Unit,
) {
    val largeText = LocalDensity.current.fontScale > 1.2f
    val failed = state.status == PublishStatus.Failed
    val busy = state.status == PublishStatus.CheckingLocation || state.status == PublishStatus.Uploading
    val messageLength = state.message.codePointCount(0, state.message.length)
    RecordingScaffold(stringResource(R.string.bc_publish_title), onBack, modifier, contentSpacing = 8.dp, bottom = {
        if (failed) RecordingCaption(stringResource(R.string.bc_retry_help))
        WattagamButton(stringResource(when (state.status) {
            PublishStatus.CheckingLocation -> R.string.bc_checking_location
            PublishStatus.Uploading -> R.string.bc_uploading
            PublishStatus.Failed -> R.string.bc_retry_publish
            PublishStatus.Ready -> R.string.bc_publish
        }), onPublish, Modifier.fillMaxWidth(), enabled = state.location != PublishLocation.Outside && messageLength <= state.messageLimit, loading = busy)
        if (failed) WattagamButton(stringResource(R.string.bc_return_draft), onEditPhoto, Modifier.fillMaxWidth(), kind = ButtonKind.Secondary)
    }) {
        if (failed) {
            RecordingNotice(stringResource(R.string.bc_publish_failed), stringResource(R.string.bc_publish_failed_body), R.drawable.bc_alert, danger = true)
            RecordingAuthor(state.authorName, state.placeName, stringResource(R.string.bc_draft_badge))
        } else {
            CreationSteps(2)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RecordingIcon(R.drawable.bc_pin, size = 19.dp)
                Text(state.placeName, Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                if (!largeText) RecordingCaption(stringResource(if (state.location == PublishLocation.Verified) R.string.bc_visit_verified else R.string.bc_location_required))
            }
            if (largeText) RecordingCaption(stringResource(if (state.location == PublishLocation.Verified) R.string.bc_visit_verified else R.string.bc_location_required))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                RecordingCaption(state.authorName, Modifier.weight(1f))
                TextButton(onClick = onEditPhoto, enabled = !busy) { Text(stringResource(R.string.bc_edit_photo), style = MaterialTheme.typography.bodySmall) }
            }
        }
        photo(Modifier.fillMaxWidth().height(if (failed) 192.dp else 170.dp))
        val label = stringResource(R.string.bc_message_label)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label, Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                if (failed) RecordingCaption(stringResource(R.string.bc_message_count, messageLength, state.messageLimit))
            }
            BasicTextField(value = state.message, onValueChange = onMessageChange, enabled = !busy,
                modifier = Modifier.fillMaxWidth().heightIn(min = if (failed) 48.dp else 96.dp).clip(WattagamShapes.Field)
                    .background(WattagamColors.Soft).border(1.dp, if (messageLength > state.messageLimit) WattagamColors.Danger else WattagamColors.Border, WattagamShapes.Field)
                    .padding(14.dp).semantics { contentDescription = label },
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = WattagamColors.Ink), cursorBrush = SolidColor(WattagamColors.Primary))
            if (!failed) RecordingCaption(stringResource(R.string.bc_message_count_optional, messageLength, state.messageLimit), color = if (messageLength > state.messageLimit) WattagamColors.Danger else WattagamColors.Muted)
        }
        if (failed) {
            RecordingNotice(stringResource(R.string.bc_stamp_missing, state.stampCount, state.placeName), stringResource(R.string.bc_stamp_on_success), R.drawable.bc_stamp)
        } else {
            Row(Modifier.fillMaxWidth().clip(WattagamShapes.Field).background(WattagamColors.PrimarySoft).padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RecordingIcon(if (state.location == PublishLocation.Verified) R.drawable.bc_check_circle else R.drawable.bc_pin_off)
                Column(Modifier.weight(1f)) {
                    Text(stringResource(if (state.location == PublishLocation.Verified) R.string.bc_location_rechecked else R.string.bc_location_required), style = MaterialTheme.typography.labelMedium)
                    RecordingCaption(if (state.location == PublishLocation.Verified) stringResource(R.string.bc_location_recent, state.placeName) else stringResource(R.string.bc_location_unverified))
                }
                TextButton(onClick = onRecheck, enabled = !busy, contentPadding = PaddingValues(0.dp)) { Text(stringResource(R.string.bc_recheck), style = MaterialTheme.typography.bodySmall) }
            }
            RecordingNotice(stringResource(R.string.bc_public_title), stringResource(R.string.bc_public_body), R.drawable.bc_globe)
            Row(Modifier.fillMaxWidth().border(1.dp, WattagamColors.Border, RoundedCornerShape(12.dp)).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RecordingIcon(R.drawable.bc_info, size = 16.dp)
                RecordingCaption(stringResource(R.string.bc_publish_draft_help))
            }
        }
    }
}

data class PublishSuccessUiState(val placeName: String, val authorName: String, val createdAt: String, val message: String, val stampCount: Int, val newlyEarnedStamp: Boolean)

@Composable
fun PublishSuccessScreen(state: PublishSuccessUiState, onBack: () -> Unit, onGallery: () -> Unit, onMyPage: () -> Unit, modifier: Modifier = Modifier, photo: @Composable (Modifier) -> Unit) {
    RecordingScaffold(stringResource(R.string.bc_success_title), onBack, modifier, bottom = {
        WattagamButton(stringResource(R.string.bc_open_gallery), onGallery, Modifier.fillMaxWidth(), kind = ButtonKind.Secondary)
        WattagamButton(stringResource(R.string.bc_open_mypage), onMyPage, Modifier.fillMaxWidth())
    }) {
        RecordingNotice(stringResource(R.string.bc_published), stringResource(R.string.bc_published_body), R.drawable.bc_check_circle, highlighted = true)
        RecordingAuthor(state.authorName, state.createdAt, stringResource(R.string.bc_place_public))
        photo(Modifier.fillMaxWidth().height(210.dp))
        if (state.message.isNotBlank()) Text(state.message, style = MaterialTheme.typography.bodyLarge)
        HorizontalDivider(color = WattagamColors.Border)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            RecordingIcon(R.drawable.bc_pin, size = 18.dp)
            Text(stringResource(R.string.bc_record_place, state.placeName), style = MaterialTheme.typography.bodyMedium, color = WattagamColors.Muted)
        }
        Column(Modifier.fillMaxWidth().clip(WattagamShapes.Card).background(WattagamColors.PrimarySoft).padding(20.dp, 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    RecordingCaption(stringResource(if (state.newlyEarnedStamp) R.string.bc_stamp_awarded else R.string.bc_stamp_existing), color = WattagamColors.Primary)
                    Text(stringResource(if (state.newlyEarnedStamp) R.string.bc_stamp_awarded_title else R.string.bc_stamp_existing_title, state.placeName), style = MaterialTheme.typography.bodyLarge)
                }
                RecordingBadge(stringResource(R.string.bc_stamp_count, state.stampCount))
            }
            RecordingCaption(stringResource(if (state.newlyEarnedStamp) R.string.bc_stamp_awarded_body else R.string.bc_stamp_existing_body))
        }
    }
}
