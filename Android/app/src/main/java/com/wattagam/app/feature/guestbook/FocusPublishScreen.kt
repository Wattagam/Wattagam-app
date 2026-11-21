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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.wattagam.app.R

@Composable
internal fun FocusPublishScreen(
    state: PublishUiState, onBack: () -> Unit, onMessageChange: (String) -> Unit,
    onEditPhoto: () -> Unit, onRecheck: () -> Unit, onPublish: () -> Unit,
    modifier: Modifier = Modifier, photo: @Composable (Modifier) -> Unit,
) {
    val busy = state.status == PublishStatus.CheckingLocation || state.status == PublishStatus.Uploading
    val count = state.message.codePointCount(0, state.message.length)
    val overLimit = count > state.messageLimit
    FocusStudioScaffold(stringResource(R.string.bc_publish_title), onBack, modifier,
        action = { TextButton(onClick = onEditPhoto, enabled = !busy) { Text(stringResource(R.string.bc_studio_edit), style = MaterialTheme.typography.bodySmall) } },
        bottom = {
            StudioPrimaryButton(stringResource(when (state.status) {
                PublishStatus.CheckingLocation -> R.string.bc_checking_location
                PublishStatus.Uploading -> R.string.bc_uploading
                else -> R.string.bc_publish
            }), onPublish, enabled = !overLimit && state.location != PublishLocation.Outside, loading = busy)
            Text(stringResource(R.string.bc_studio_stamp), style = MaterialTheme.typography.bodySmall, color = StudioColors.Muted)
        },
    ) {
        StudioLocation(state.placeName)
        photo(Modifier.fillMaxWidth().aspectRatio(1.4f))
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val label = stringResource(R.string.bc_message_label)
            Text(label, style = MaterialTheme.typography.labelMedium)
            BasicTextField(value = state.message, onValueChange = onMessageChange, enabled = !busy,
                modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp).clip(RoundedCornerShape(12.dp))
                    .background(StudioColors.Surface)
                    .then(if (overLimit) Modifier.border(1.dp, StudioColors.Error, RoundedCornerShape(12.dp)) else Modifier)
                    .padding(16.dp).semantics { contentDescription = label },
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White), cursorBrush = SolidColor(Color.White))
            Text(stringResource(R.string.bc_message_count, count, state.messageLimit), Modifier.align(Alignment.End),
                color = if (overLimit) StudioColors.Error else StudioColors.Muted, style = MaterialTheme.typography.bodySmall)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(painterResource(R.drawable.bc_globe), null, Modifier.size(18.dp), tint = StudioColors.Muted)
                Text(stringResource(R.string.bc_studio_public, state.authorName), style = MaterialTheme.typography.bodySmall, color = StudioColors.Muted)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(painterResource(if (state.location == PublishLocation.Verified) R.drawable.bc_check else R.drawable.bc_pin_off), null,
                    Modifier.size(18.dp), tint = if (state.location == PublishLocation.Outside) StudioColors.Error else StudioColors.Muted)
                Text(stringResource(when (state.location) {
                    PublishLocation.Verified -> R.string.bc_studio_location_verified
                    PublishLocation.Unverified -> R.string.bc_location_required
                    PublishLocation.Outside -> R.string.bc_studio_location_outside
                }), Modifier.weight(1f), style = MaterialTheme.typography.bodySmall,
                    color = if (state.location == PublishLocation.Outside) StudioColors.Error else StudioColors.Muted)
                TextButton(onClick = onRecheck, enabled = !busy) { Text(stringResource(R.string.bc_recheck), style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}
