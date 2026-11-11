package com.wattagam.app.feature.guestbook

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
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
    val stampTotal: Int = 4,
    val location: PublishLocation = PublishLocation.Verified,
    val messageLimit: Int = 80,
)

@Composable
fun PublishScreen(state: PublishUiState, onBack: () -> Unit, onMessageChange: (String) -> Unit, onEditPhoto: () -> Unit, onRecheck: () -> Unit, onPublish: () -> Unit,
    modifier: Modifier = Modifier, photo: @Composable (Modifier) -> Unit,
) {
    if (state.status != PublishStatus.Failed) {
        FocusPublishScreen(state, onBack, onMessageChange, onEditPhoto, onRecheck, onPublish, modifier, photo)
        return
    }
    val messageLength = state.message.codePointCount(0, state.message.length)
    RecordingScaffold(stringResource(R.string.bc_publish_title), onBack, modifier, contentSpacing = 8.dp, bottom = {
        RecordingCaption(stringResource(R.string.bc_retry_help))
        WattagamButton(stringResource(R.string.bc_retry_publish), onPublish, Modifier.fillMaxWidth(),
            enabled = state.location != PublishLocation.Outside && messageLength <= state.messageLimit)
        WattagamButton(stringResource(R.string.bc_return_draft), onEditPhoto, Modifier.fillMaxWidth(), kind = ButtonKind.Secondary)
    }) {
        RecordingNotice(stringResource(R.string.bc_publish_failed), stringResource(R.string.bc_publish_failed_body), R.drawable.bc_alert, danger = true)
        RecordingAuthor(state.authorName, state.placeName, stringResource(R.string.bc_draft_badge))
        photo(Modifier.fillMaxWidth().height(192.dp))
        val label = stringResource(R.string.bc_message_label)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label, Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                RecordingCaption(stringResource(R.string.bc_message_count, messageLength, state.messageLimit))
            }
            BasicTextField(value = state.message, onValueChange = onMessageChange,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(WattagamShapes.Field)
                    .background(WattagamColors.Soft).border(1.dp, if (messageLength > state.messageLimit) WattagamColors.Danger else WattagamColors.Border, WattagamShapes.Field)
                    .padding(16.dp).semantics { contentDescription = label },
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = WattagamColors.Ink), cursorBrush = SolidColor(WattagamColors.Primary))
        }
        RecordingNotice(stringResource(R.string.bc_stamp_missing, state.stampCount, state.placeName, state.stampTotal), stringResource(R.string.bc_stamp_on_success), R.drawable.bc_stamp)
    }
}

data class PublishSuccessUiState(
    val placeName: String,
    val authorName: String,
    val createdAt: String,
    val message: String,
    val stampCount: Int,
    val newlyEarnedStamp: Boolean,
    val stampTotal: Int = 4,
)

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
            RecordingIcon(R.drawable.bc_pin, tint = WattagamColors.Muted)
            Text(stringResource(R.string.bc_record_place, state.placeName), style = MaterialTheme.typography.bodyMedium, color = WattagamColors.Muted)
        }
        // 이 화면의 주인공은 방금 찍힌 장소 도장
        Row(Modifier.fillMaxWidth().clip(WattagamShapes.Card).background(WattagamColors.Soft).padding(20.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            PlaceStamp(state.placeName, earned = true, size = 72.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                RecordingCaption(stringResource(if (state.newlyEarnedStamp) R.string.bc_stamp_awarded else R.string.bc_stamp_existing) + " · " +
                    stringResource(R.string.bc_stamp_count, state.stampCount, state.stampTotal))
                Text(stringResource(if (state.newlyEarnedStamp) R.string.bc_stamp_awarded_title else R.string.bc_stamp_existing_title, state.placeName), style = MaterialTheme.typography.bodyLarge)
                RecordingCaption(stringResource(if (state.newlyEarnedStamp) R.string.bc_stamp_awarded_body else R.string.bc_stamp_existing_body))
            }
        }
    }
}
