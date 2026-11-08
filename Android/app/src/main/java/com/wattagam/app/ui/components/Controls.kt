package com.wattagam.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wattagam.app.R
import com.wattagam.app.ui.theme.*

enum class ButtonKind { Primary, Secondary, Text }

@Composable
fun WattagamButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.Primary,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val active = enabled && !loading
    // 강조색은 주요 버튼에만. 보조 버튼은 중립 표면, 글자 버튼만 강조색 글씨
    val container = when (kind) {
        ButtonKind.Primary -> WattagamColors.Primary
        ButtonKind.Secondary -> WattagamColors.Soft
        ButtonKind.Text -> androidx.compose.ui.graphics.Color.Transparent
    }
    val ink = when (kind) {
        ButtonKind.Primary -> WattagamColors.Surface
        ButtonKind.Secondary -> WattagamColors.Ink
        ButtonKind.Text -> WattagamColors.Primary
    }
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = WattagamSizes.Button),
        enabled = active,
        shape = WattagamShapes.Button,
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = ink,
            disabledContainerColor = if (loading) container else WattagamColors.Border,
            disabledContentColor = if (loading) ink else WattagamColors.Muted,
        ),
        elevation = null,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(WattagamSpacing.Small), verticalAlignment = Alignment.CenterVertically) {
            if (loading) CircularProgressIndicator(Modifier.size(18.dp), color = ink, strokeWidth = 2.dp)
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun WattagamTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    isError: Boolean = false,
    singleLine: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text(label) },
        supportingText = supportingText?.let { { Text(it) } },
        isError = isError,
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        shape = WattagamShapes.Field,
        textStyle = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
fun AppTopBar(
    title: String,
    backLabel: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    Row(
        modifier.fillMaxWidth().heightIn(min = WattagamSizes.TopBar).padding(horizontal = WattagamSpacing.Content),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(WattagamSpacing.Small),
    ) {
        // 뒤로는 촬영·업로드 화면(RecordingTopBar)과 같은 화살표 아이콘. 글자는 화면 읽기용 설명으로 쓴다
        if (backLabel != null) {
            IconButton(onClick = onBack, modifier = Modifier.size(WattagamSizes.TouchTarget)) {
                Icon(painterResource(R.drawable.bc_arrow), backLabel, Modifier.size(24.dp), tint = WattagamColors.Ink)
            }
        }
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        if (actionLabel != null) TextButton(onClick = onAction, modifier = Modifier.heightIn(min = WattagamSizes.TouchTarget)) { Text(actionLabel) }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 320, fontScale = 1.5f)
@Composable
private fun ButtonsPreview() {
    WattagamTheme {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            WattagamButton("방명록 남기기", {}, Modifier.fillMaxWidth())
            WattagamButton("위치를 먼저 확인해주세요", {}, Modifier.fillMaxWidth(), enabled = false)
            WattagamButton("업로드 중…", {}, Modifier.fillMaxWidth(), loading = true)
        }
    }
}
