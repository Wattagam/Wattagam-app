package com.wattagam.app.feature.guestbook

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
    RecordingScaffold(stringResource(R.string.bc_editor_title), onBack, modifier, dark = true, contentSpacing = 16.dp,
        topAction = { TextButton(onClick = onClear) { Text(stringResource(R.string.bc_clear_drawing), color = Color.White, style = MaterialTheme.typography.bodyMedium) } },
        bottom = {
            RecordingCaption(stringResource(R.string.bc_editor_optional), color = Color(0xFFAFBDD0))
            WattagamButton(stringResource(R.string.bc_editor_done), onDone, Modifier.fillMaxWidth())
        },
    ) {
        CreationSteps(1, dark = true)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                RecordingIcon(R.drawable.bc_pin_light, size = 16.dp)
                RecordingCaption(state.placeName, color = Color(0xFFAFBDD0))
            }
        }
        canvas()
        Row(Modifier.fillMaxWidth().selectableGroup().clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = .06f)).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            DrawingTool.entries.forEach { tool ->
                Row(Modifier.weight(1f).heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp)).background(if (tool == state.tool) Color.White.copy(alpha = .16f) else Color.Transparent)
                    .selectable(tool == state.tool, role = Role.RadioButton, onClick = { onTool(tool) }).padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                    RecordingIcon(if (tool == DrawingTool.Pen) R.drawable.bc_pen else R.drawable.bc_eraser)
                    Text(stringResource(if (tool == DrawingTool.Pen) R.string.bc_pen else R.string.bc_eraser), color = Color.White, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.SpaceBetween) {
            DrawingColor.entries.forEach { color ->
                val (fill, label) = when (color) {
                    DrawingColor.Blue -> WattagamColors.Primary to R.string.bc_blue
                    DrawingColor.Black -> Color(0xFF20252D) to R.string.bc_black
                    DrawingColor.Red -> Color(0xFFEF7771) to R.string.bc_red
                    DrawingColor.Yellow -> Color(0xFFFFD76B) to R.string.bc_yellow
                    DrawingColor.Green -> Color(0xFF7BB99E) to R.string.bc_green
                }
                val description = stringResource(label)
                Box(Modifier.size(48.dp).clip(CircleShape)
                    .selectable(color == state.color, role = Role.RadioButton, onClick = { onColor(color) })
                    .semantics { contentDescription = description }, contentAlignment = Alignment.Center) {
                    if (color == state.color) Box(Modifier.size(44.dp).border(2.dp, Color.White, CircleShape))
                    Box(Modifier.size(30.dp).background(fill, CircleShape).border(1.dp, Color.White.copy(alpha = .3f), CircleShape), contentAlignment = Alignment.Center) {
                        if (color == state.color) Icon(painterResource(R.drawable.bc_check), null, Modifier.size(18.dp),
                            tint = if (color == DrawingColor.Black || color == DrawingColor.Blue) Color.White else RecordingNavy)
                    }
                }
            }
        }
        TextButton(onClick = onAddText, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).border(1.dp, Color.White.copy(alpha = .22f), RoundedCornerShape(16.dp))) {
            Text(stringResource(R.string.bc_add_text), color = Color.White)
        }
    }
}
