package com.wattagam.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wattagam.app.R
import com.wattagam.app.ui.theme.*

val RecordingNavy = Color(0xFF102139)

@Composable
fun RecordingIcon(@DrawableRes asset: Int, modifier: Modifier = Modifier, size: Dp = 20.dp) {
    Image(painterResource(asset), contentDescription = null, modifier = modifier.size(size))
}

@Composable
fun RecordingTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    dark: Boolean = false,
    action: @Composable RowScope.() -> Unit = {},
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val separateAction = maxWidth < 360.dp || LocalDensity.current.fontScale > 1.2f
        Column {
            Row(Modifier.fillMaxWidth().heightIn(min = 60.dp).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp).background(if (dark) RecordingNavy else WattagamColors.Soft, CircleShape)) {
                    Image(painterResource(if (dark) R.drawable.bc_arrow_light else R.drawable.bc_arrow), stringResource(R.string.back), Modifier.size(22.dp))
                }
                Text(title, Modifier.weight(1f), color = if (dark) Color.White else WattagamColors.Ink, style = MaterialTheme.typography.titleLarge)
                if (!separateAction) action()
            }
            if (separateAction) Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.End, content = action)
        }
    }
}

@Composable
fun RecordingScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    dark: Boolean = false,
    topAction: @Composable RowScope.() -> Unit = {},
    bottom: @Composable ColumnScope.() -> Unit = {},
    contentSpacing: Dp = 12.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val background = if (dark) RecordingNavy else WattagamColors.Surface
    CompositionLocalProvider(LocalContentColor provides if (dark) Color.White else WattagamColors.Ink) {
    Column(modifier.fillMaxSize().background(background).safeDrawingPadding().imePadding()) {
        RecordingTopBar(title, onBack, dark = dark, action = topAction)
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(contentSpacing), content = content)
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = bottom)
    }
    }
}

@Composable
fun RecordingCaption(text: String, modifier: Modifier = Modifier, color: Color = WattagamColors.Muted) {
    Text(text, modifier, color = color, style = MaterialTheme.typography.bodySmall)
}

@Composable
fun RecordingBadge(text: String, modifier: Modifier = Modifier, danger: Boolean = false, dark: Boolean = false, solidDark: Boolean = false, @DrawableRes icon: Int? = null) {
    Row(modifier.clip(RoundedCornerShape(if (solidDark) 22.dp else 8.dp)).background(if (solidDark) RecordingNavy else if (danger) WattagamColors.DangerSoft else if (dark) Color.White.copy(alpha = .1f) else WattagamColors.PrimarySoft).padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        icon?.let { RecordingIcon(it, size = 13.dp) }
        Text(text, style = MaterialTheme.typography.labelSmall, color = if (danger) WattagamColors.Danger else if (dark) Color.White else WattagamColors.Primary)
    }
}

@Composable
fun RecordingNotice(title: String, body: String, @DrawableRes icon: Int, modifier: Modifier = Modifier, highlighted: Boolean = false, danger: Boolean = false) {
    Row(modifier.fillMaxWidth().clip(WattagamShapes.Field).background(if (danger) WattagamColors.DangerSoft else if (highlighted) WattagamColors.PrimarySoft else WattagamColors.Soft).padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        RecordingIcon(icon)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium)
            RecordingCaption(body)
        }
    }
}

@Composable
fun CreationSteps(active: Int, modifier: Modifier = Modifier) {
    val labels = listOf(R.string.bc_step_photo, R.string.bc_step_draw, R.string.bc_step_message)
    Row(modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        labels.forEachIndexed { index, label ->
            Text(stringResource(label), Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(if (active == index) WattagamColors.PrimarySoft else Color.Transparent).padding(horizontal = 10.dp, vertical = 6.dp),
                color = if (active == index) WattagamColors.Primary else WattagamColors.Muted,
                style = MaterialTheme.typography.bodySmall, fontWeight = if (active == index) FontWeight.Medium else FontWeight.Normal)
        }
    }
}

@Composable
fun RecordingPhoto(
    photo: Painter,
    description: String,
    modifier: Modifier = Modifier,
    overlay: @Composable BoxScope.() -> Unit = {},
) {
    Box(modifier.clip(WattagamShapes.Card)) {
        Image(photo, description, Modifier.matchParentSize(), contentScale = ContentScale.Crop)
        overlay()
    }
}

@Composable
fun RecordingHeading(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, style = MaterialTheme.typography.headlineLarge.copy(fontSize = 24.sp, lineHeight = 36.sp))
}

@Composable
fun RecordingAuthor(name: String, subtitle: String, badge: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(38.dp).background(WattagamColors.PrimarySoft, CircleShape), contentAlignment = Alignment.Center) {
            Text(name.take(1), color = WattagamColors.Primary, style = MaterialTheme.typography.labelMedium)
        }
        Column(Modifier.weight(1f)) { Text(name, style = MaterialTheme.typography.labelMedium); RecordingCaption(subtitle) }
        RecordingBadge(badge)
    }
}
