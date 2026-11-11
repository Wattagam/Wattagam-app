package com.wattagam.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wattagam.app.R
import com.wattagam.app.ui.theme.*

/** [Failed] 는 위치는 쟀지만 서버 확인 결과를 받지 못한 경우 */
enum class LocationUiState { Available, Outside, Checking, Denied, Unavailable, Failed }

@Composable
fun StatusCard(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    action: @Composable (() -> Unit)? = null,
) {
    Surface(modifier, shape = WattagamShapes.Card, color = if (highlighted) WattagamColors.PrimarySoft else WattagamColors.Soft) {
        Column(Modifier.padding(WattagamSpacing.Content), verticalArrangement = Arrangement.spacedBy(WattagamSpacing.Medium)) {
            Text(title, style = MaterialTheme.typography.labelMedium)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = WattagamColors.Muted)
            action?.invoke()
        }
    }
}

@Composable
fun LocationStatus(state: LocationUiState, modifier: Modifier = Modifier) {
    val (title, body) = when (state) {
        LocationUiState.Available -> R.string.location_available to R.string.location_available_body
        LocationUiState.Outside -> R.string.location_outside to R.string.location_outside_body
        LocationUiState.Checking -> R.string.location_checking to R.string.location_checking_body
        LocationUiState.Denied -> R.string.location_denied to R.string.location_denied_body
        LocationUiState.Unavailable -> R.string.location_unavailable to R.string.location_unavailable_body
        LocationUiState.Failed -> R.string.location_failed to R.string.location_failed_body
    }
    StatusCard(stringResource(title), stringResource(body), modifier, highlighted = state == LocationUiState.Available)
}

@Preview(showBackground = true, widthDp = 320, fontScale = 1.5f)
@Composable
private fun LocationStatusPreview() {
    WattagamTheme { LocationStatus(LocationUiState.Outside, Modifier.fillMaxWidth().padding(16.dp)) }
}
