package com.wattagam.app.feature.guestbook

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wattagam.app.R
import com.wattagam.app.ui.components.RecordingTopBar

internal object StudioColors {
    val Background = Color(0xFF111111)
    val Surface = Color(0xFF242424)
    val Muted = Color(0xFFB7B7B7)
    val Error = Color(0xFFFFB4AB)
}

@Composable
internal fun FocusStudioScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    action: @Composable RowScope.() -> Unit = {},
    bottom: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    MaterialTheme(colorScheme = darkColorScheme(
        primary = Color.White, onPrimary = StudioColors.Background,
        background = StudioColors.Background, surface = StudioColors.Surface,
        onBackground = Color.White, onSurface = Color.White,
        onSurfaceVariant = StudioColors.Muted, error = StudioColors.Error,
    ), typography = MaterialTheme.typography, shapes = MaterialTheme.shapes) {
        Surface(modifier.fillMaxSize(), color = StudioColors.Background) {
            Column(Modifier.safeDrawingPadding().imePadding()) {
                RecordingTopBar(title, onBack, dark = true, action = action)
                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), content = content)
                Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp), content = bottom)
            }
        }
    }
}

@Composable
internal fun StudioPrimaryButton(text: String, onClick: () -> Unit, enabled: Boolean = true, loading: Boolean = false) {
    Button(onClick = onClick, enabled = enabled && !loading,
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = StudioColors.Background,
            disabledContainerColor = StudioColors.Surface, disabledContentColor = StudioColors.Muted),
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(18.dp), color = StudioColors.Muted, strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
internal fun StudioLocation(place: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(painterResource(R.drawable.bc_pin), null, Modifier.size(16.dp), tint = StudioColors.Muted)
        Text(place, color = StudioColors.Muted, style = MaterialTheme.typography.bodySmall)
    }
}
