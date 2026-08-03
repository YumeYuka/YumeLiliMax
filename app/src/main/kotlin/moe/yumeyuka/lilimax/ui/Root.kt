package moe.yumeyuka.lilimax.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay

sealed interface Route {
    data object Home : Route

    data object DeviceSettings : Route
}

@Composable
fun LilimaxApp(viewModel: ViewModel) {
    val backStack = remember { mutableStateListOf<Route>(Route.Home) }

    NavDisplay(
        backStack = backStack,
        entryProvider =
            entryProvider {
                entry(Route.Home) {
                    HomeScreen(
                        viewModel = viewModel,
                        onOpenSettings = { backStack.add(Route.DeviceSettings) },
                    )
                }
                entry(Route.DeviceSettings) {
                    DeviceSettingsScreen(
                        viewModel = viewModel,
                        onBack = { backStack.removeLast() },
                    )
                }
            },
    )
}
