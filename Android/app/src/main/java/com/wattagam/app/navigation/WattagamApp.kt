package com.wattagam.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.wattagam.app.feature.showcase.ShowcaseScreen
import com.wattagam.app.feature.showcase.ShowcaseSection
import kotlinx.serialization.Serializable

@Serializable
private data object ShowcaseHome : NavKey

@Serializable
private data class ShowcaseDetail(val section: ShowcaseSection) : NavKey

@Composable
fun WattagamApp() {
    val backStack = rememberNavBackStack(ShowcaseHome)
    NavDisplay(
        backStack = backStack,
        onBack = { if (backStack.size > 1) backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<ShowcaseHome> {
                ShowcaseScreen(section = null, onSection = { backStack.add(ShowcaseDetail(it)) })
            }
            entry<ShowcaseDetail> { key ->
                ShowcaseScreen(section = key.section, onBack = { backStack.removeLastOrNull() })
            }
        },
    )
}
