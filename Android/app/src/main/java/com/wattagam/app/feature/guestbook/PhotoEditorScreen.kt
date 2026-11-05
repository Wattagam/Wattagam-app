package com.wattagam.app.feature.guestbook

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.wattagam.app.R
import com.wattagam.app.ui.components.*
import com.wattagam.app.ui.theme.*

enum class DrawingTool { Pen, Eraser }
enum class DrawingColor { Blue, Black, Red, Yellow, Green }
data class PhotoEditorUiState(val placeName: String, val tool: DrawingTool = DrawingTool.Pen, val color: DrawingColor = DrawingColor.Yellow)

@Composable
fun PhotoEditorScreen(state: PhotoEditorUiState, onBack: () -> Unit, onClear: () -> Unit, onTool: (DrawingTool) -> Unit, onColor: (DrawingColor) -> Unit,
    onAddText: () -> Unit, onDone: () -> Unit, modifier: Modifier = Modifier, canvas: @Composable () -> Unit,
) {
    RecordingScaffold(stringResource(R.string.bc_editor_title), onBack, modifier, dark = true, contentSpacing = 10.dp,
        topAction = { TextButton(onClick = onClear) { Text(stringResource(R.string.bc_clear_drawing), color = Color.White, style = MaterialTheme.typography.bodyMedium) } },
        bottom = { WattagamButton(stringResource(R.string.bc_editor_done), onDone, Modifier.fillMaxWidth()) },
    ) {
        CreationSteps(1)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            RecordingBadge(state.placeName, dark = true, icon = R.drawable.bc_pin_light)
            RecordingCaption(stringResource(R.string.bc_draw_freely), color = Color.White)
        }
        canvas()
        Row(Modifier.fillMaxWidth().clip(WattagamShapes.Pill).background(Color.White.copy(alpha = .1f)).padding(6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DrawingTool.entries.forEach { tool ->
                Row(Modifier.weight(1f).heightIn(min = 48.dp).clip(WattagamShapes.Pill).background(if (tool == state.tool) WattagamColors.Primary else Color.White.copy(alpha = .08f))
                    .selectable(tool == state.tool, role = Role.RadioButton, onClick = { onTool(tool) }).padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                    RecordingIcon(if (tool == DrawingTool.Pen) R.drawable.bc_pen else R.drawable.bc_eraser)
                    Text(stringResource(if (tool == DrawingTool.Pen) R.string.bc_pen else R.string.bc_eraser), color = Color.White, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            DrawingColor.entries.forEach { color ->
                val (asset, label) = when (color) {
                    DrawingColor.Blue -> R.drawable.bc_swatch_blue to R.string.bc_blue
                    DrawingColor.Black -> R.drawable.bc_swatch_black to R.string.bc_black
                    DrawingColor.Red -> R.drawable.bc_swatch_red to R.string.bc_red
                    DrawingColor.Yellow -> R.drawable.bc_swatch_yellow to R.string.bc_yellow
                    DrawingColor.Green -> R.drawable.bc_swatch_green to R.string.bc_green
                }
                Box(Modifier.size(48.dp).clip(CircleShape).then(if (color == state.color) Modifier.border(2.dp, WattagamColors.Primary, CircleShape) else Modifier)
                    .selectable(color == state.color, role = Role.RadioButton, onClick = { onColor(color) }), contentAlignment = Alignment.Center) {
                    Image(painterResource(asset), stringResource(label), Modifier.size(if (color == DrawingColor.Yellow) 28.dp else 48.dp))
                    if (color == state.color) RecordingIcon(R.drawable.bc_color_check)
                }
            }
        }
        TextButton(onClick = onAddText, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).background(Color.White.copy(alpha = .1f), RoundedCornerShape(30.dp))) {
            Text(stringResource(R.string.bc_add_text), color = Color.White)
        }
        RecordingCaption(stringResource(R.string.bc_editor_optional), color = Color(0xFFA6B8CF))
    }
}
