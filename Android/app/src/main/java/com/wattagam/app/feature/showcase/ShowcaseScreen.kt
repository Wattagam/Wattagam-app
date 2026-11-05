package com.wattagam.app.feature.showcase

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.wattagam.app.R
import com.wattagam.app.ui.components.*
import com.wattagam.app.ui.theme.*
import kotlinx.serialization.Serializable

@Serializable
enum class ShowcaseSection(@param:StringRes val title: Int) {
    Buttons(R.string.buttons), Inputs(R.string.inputs), States(R.string.states), Stamps(R.string.stamps),
}

@Composable
fun ShowcaseScreen(
    section: ShowcaseSection?,
    onSection: (ShowcaseSection) -> Unit = {},
    onBack: () -> Unit = {},
    onBcSamples: () -> Unit = {},
    onStartFlow: () -> Unit = {},
) {
    Scaffold { insets ->
        Column(Modifier.fillMaxSize().padding(insets).imePadding()) {
            AppTopBar(stringResource(section?.title ?: R.string.app_name), if (section != null) stringResource(R.string.back) else null, onBack)
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(WattagamSpacing.Screen),
                verticalArrangement = Arrangement.spacedBy(WattagamSpacing.Content),
            ) {
                Text(stringResource(R.string.sample_notice), style = MaterialTheme.typography.bodySmall, color = WattagamColors.Muted)
                when (section) {
                    null -> {
                        Text(stringResource(R.string.showcase_title), style = MaterialTheme.typography.headlineLarge)
                        Text(stringResource(R.string.showcase_description), style = MaterialTheme.typography.bodyMedium)
                        WattagamButton(stringResource(R.string.bc_samples_title), onBcSamples, Modifier.fillMaxWidth())
                        ShowcaseSection.entries.forEach { item ->
                            WattagamButton(stringResource(item.title), { onSection(item) }, Modifier.fillMaxWidth(), kind = ButtonKind.Secondary)
                        }
                        WattagamButton(stringResource(R.string.showcase_start_flow), onStartFlow, Modifier.fillMaxWidth())
                    }
                    ShowcaseSection.Buttons -> ButtonSamples()
                    ShowcaseSection.Inputs -> InputSamples()
                    ShowcaseSection.States -> StateSamples()
                    ShowcaseSection.Stamps -> StampSamples()
                }
            }
        }
    }
}

@Composable
private fun ButtonSamples() {
    var clicked by rememberSaveable { mutableStateOf(false) }
    WattagamButton(stringResource(R.string.primary_action), { clicked = true }, Modifier.fillMaxWidth())
    WattagamButton(stringResource(R.string.secondary_action), { clicked = true }, Modifier.fillMaxWidth(), kind = ButtonKind.Secondary)
    WattagamButton(stringResource(R.string.disabled_action), { clicked = true }, Modifier.fillMaxWidth(), enabled = false)
    WattagamButton(stringResource(R.string.loading_action), { clicked = true }, Modifier.fillMaxWidth(), loading = true)
    WattagamButton(stringResource(R.string.text_action), { clicked = true }, Modifier.fillMaxWidth(), kind = ButtonKind.Text)
    if (clicked) Text(stringResource(R.string.sample_clicked), style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun InputSamples() {
    var nickname by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf("") }
    var invalid by rememberSaveable { mutableStateOf("") }
    WattagamTextField(nickname, { nickname = it }, stringResource(R.string.nickname), Modifier.fillMaxWidth(), stringResource(R.string.nickname_hint))
    WattagamTextField(invalid, { invalid = it }, stringResource(R.string.error_example), Modifier.fillMaxWidth(), stringResource(R.string.nickname_error), isError = true)
    WattagamTextField(message, { message = it }, stringResource(R.string.message), Modifier.fillMaxWidth(), stringResource(R.string.message_hint), singleLine = false)
}

@Composable
private fun StateSamples() {
    var clicked by rememberSaveable { mutableStateOf(false) }
    LocationUiState.entries.forEach { LocationStatus(it, Modifier.fillMaxWidth()) }
    StatusCard(stringResource(R.string.empty_title), stringResource(R.string.empty_body), Modifier.fillMaxWidth())
    StatusCard(stringResource(R.string.error_title), stringResource(R.string.error_body), Modifier.fillMaxWidth(), action = {
        WattagamButton(stringResource(R.string.retry), { clicked = true }, kind = ButtonKind.Text)
    })
    if (clicked) Text(stringResource(R.string.sample_clicked))
    StatusCard(stringResource(R.string.public_title), stringResource(R.string.public_body), Modifier.fillMaxWidth())
}

@Composable
private fun StampSamples() {
    val stamps = listOf(
        StampUiState("gate", stringResource(R.string.gate), true),
        StampUiState("library", stringResource(R.string.library), true),
        StampUiState("pond", stringResource(R.string.pond), false),
        StampUiState("310", stringResource(R.string.building), false),
    )
    StampBoard(stamps, Modifier.fillMaxWidth())
    Text(stringResource(R.string.stamp_sample), style = MaterialTheme.typography.bodyMedium)
    Text(stringResource(R.string.stamp_rule), style = MaterialTheme.typography.bodyMedium)
}

@Preview(showBackground = true, widthDp = 412, heightDp = 916)
@Preview(showBackground = true, widthDp = 320, heightDp = 720, fontScale = 1.5f)
@Composable
private fun ShowcasePreview() {
    WattagamTheme { ShowcaseScreen(null) }
}
