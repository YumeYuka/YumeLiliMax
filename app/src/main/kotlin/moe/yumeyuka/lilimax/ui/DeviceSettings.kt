package moe.yumeyuka.lilimax.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import moe.yumeyuka.lilimax.protocol.SpiConstants
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.utils.overScrollVertical

private val FINDER_MODES =
    listOf(
        SpiConstants.FINDER_OFF to "关闭",
        SpiConstants.FINDER_LEFT to "左耳",
        SpiConstants.FINDER_RIGHT to "右耳",
        SpiConstants.FINDER_BOTH to "双耳",
    )

@Composable
fun DeviceSettingsScreen(viewModel: ViewModel, onBack: () -> Unit) {

    val scrollBehavior = MiuixScrollBehavior()
    val earbudState by viewModel.earbudState.collectAsState()
    var finderIndex by remember {
        mutableStateOf(
            FINDER_MODES.indexOfFirst { it.first.toInt() == earbudState.finderMode }
                .coerceAtLeast(0)
        )
    }
    LaunchedEffect(earbudState.finderMode) {
        val index = FINDER_MODES.indexOfFirst { it.first.toInt() == earbudState.finderMode }
        if (index >= 0) finderIndex = index
    }

    Scaffold(
        topBar = {
            TopAppBar(
                "更多设置",
                scrollBehavior = scrollBehavior,
            )
        },
        content = { paddingValues ->
            LazyColumn(
                modifier =
                    Modifier.fillMaxSize()
                        .padding(horizontal = 12.dp)
                        .overScrollVertical()
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                contentPadding =
                    PaddingValues(
                        top = paddingValues.calculateTopPadding() + 20.dp,
                        bottom = 32.dp,
                    ),
            ) {
                item {
                    Card {
                        ArrowPreference(
                            title = "清除配对",
                            onClick = { viewModel.clearPairing() },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        ArrowPreference(
                            title = "恢复出厂设置",
                            onClick = { viewModel.resetDevice() },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        ArrowPreference(
                            title = "关机",
                            onClick = { viewModel.powerOff() },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OverlayDropdownPreference(
                            title = "耳机查找",
                            items = FINDER_MODES.map { it.second },
                            selectedIndex = finderIndex,
                            onSelectedIndexChange = { index ->
                                finderIndex = index
                                viewModel.switchFinder(FINDER_MODES[index].first)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        },
    )
}
