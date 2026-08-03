package moe.yumeyuka.lilimax.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import moe.yumeyuka.lilimax.R
import moe.yumeyuka.lilimax.model.BatteryState
import moe.yumeyuka.lilimax.model.ConnectionState
import moe.yumeyuka.lilimax.model.EarbudState
import moe.yumeyuka.lilimax.protocol.SpiConstants
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SliderPreference
import top.yukonga.miuix.kmp.utils.overScrollVertical

private val WORK_MODES =
    listOf(
        SpiConstants.WORK_MUSIC to "音乐",
        SpiConstants.WORK_GAME to "游戏",
    )

private val EQ_MODES =
    listOf(
        SpiConstants.EQ_HIFI to "HiFi",
        SpiConstants.EQ_POP to "流行",
        SpiConstants.EQ_ROCK to "摇滚",
        SpiConstants.EQ_FPS to "游戏",
        SpiConstants.EQ_LC to "低音增强",
        SpiConstants.EQ_CUSTOM to "自定义",
    )

private val AUDIO_PROTOCOLS =
    listOf(
        SpiConstants.AUDIO_DUAL_DEVICE to "双设备连接",
        SpiConstants.AUDIO_LDAC to "LDAC",
        SpiConstants.AUDIO_AAC to "AAC",
        SpiConstants.AUDIO_LHDC to "LHDC",
        SpiConstants.AUDIO_LC3 to "LC3",
    )

private val LANGUAGE_MODES =
    listOf(
        SpiConstants.LANG_CHINESE to "中文播报",
        SpiConstants.LANG_ENGLISH to "英文播报",
    )

private fun indexOf(modes: List<Pair<Byte, String>>, value: Int?): Int =
    value?.let { current -> modes.indexOfFirst { it.first.toInt() == current } }?.takeIf { it >= 0 }
        ?: 0

private fun needsReconnect(state: ConnectionState): Boolean =
    state is ConnectionState.Idle ||
        state is ConnectionState.Failed ||
        state is ConnectionState.Closed

private val NOISE_BLUE = Color(0xFF0D84FF)

@Composable
fun HomeScreen(viewModel: ViewModel, onOpenSettings: () -> Unit) {

    val scrollBehavior = MiuixScrollBehavior()
    val earbudState by viewModel.earbudState.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val rawRx by viewModel.rawRx.collectAsState()
    val isConnected = connectionState is ConnectionState.Connected

    var showReconnectDialog by rememberSaveable { mutableStateOf(false) }
    var dialogShownOnce by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(700)
        if (!dialogShownOnce && needsReconnect(viewModel.connectionState.value)) {
            dialogShownOnce = true
            showReconnectDialog = true
        }
    }
    LaunchedEffect(isConnected) {
        if (isConnected) showReconnectDialog = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                "琉璃 MAX",
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
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding =
                    PaddingValues(
                        top = paddingValues.calculateTopPadding(),
                        bottom = 32.dp,
                    ),
            ) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(R.drawable.m8_black),
                            contentDescription = "琉璃 MAX 耳机",
                            modifier = Modifier.size(200.dp),
                        )
                    }
                }

                if (needsReconnect(connectionState)) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                "未连接，点击重连",
                                fontSize = 14.sp,
                                color = NOISE_BLUE,
                                textDecoration = TextDecoration.Underline,
                                modifier =
                                    Modifier.clickable(onClick = viewModel::connectToHeadset)
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                            )
                        }
                    }
                }

                item {
                    Card {
                        ConnectionStatusCard(
                            state = connectionState,
                            battery = earbudState.battery,
                        )
                    }
                }

                item {
                    Card {
                        NoiseSelector(earbudState.noiseControl, viewModel::switchNoiseControl)
                    }
                }

                item {
                    Card {
                        ModeDropdown(
                            "工作模式",
                            WORK_MODES,
                            earbudState.workMode,
                            viewModel::switchWorkMode,
                        )
                        ModeDropdown("音效", EQ_MODES, earbudState.eqMode, viewModel::switchEq)
                        ModeDropdown(
                            "音频协议",
                            AUDIO_PROTOCOLS,
                            earbudState.audioProtocol,
                            viewModel::switchAudioProtocol,
                        )
                    }
                }

                item {
                    Card {
                        ModeDropdown(
                            "耳机播报语言",
                            LANGUAGE_MODES,
                            earbudState.language,
                            viewModel::switchLanguage,
                        )
                        PromptLevelSlider(viewModel, earbudState)
                    }
                }

                item {
                    Card {
                        ArrowPreference(
                            title = "更多设置",
                            onClick = onOpenSettings,
                        )
                    }
                }
            }
        },
    )

    OverlayDialog(
        show = showReconnectDialog,
        title = "设备未连接",
        summary = "没有连接上耳机，是否重新连接？",
        onDismissRequest = { showReconnectDialog = false },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                text = "取消",
                onClick = { showReconnectDialog = false },
            )
            TextButton(
                text = "重连",
                onClick = {
                    showReconnectDialog = false
                    viewModel.connectToHeadset()
                },
            )
        }
    }
}

@Composable
private fun ConnectionStatusCard(
    state: ConnectionState,
    battery: BatteryState?,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(96.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BatteryColumn("左耳", battery?.leftPercent, battery?.leftCharging, Modifier.weight(1f))
        BatteryDivider(Modifier.fillMaxHeight().padding(vertical = 10.dp))
        BatteryColumn("右耳", battery?.rightPercent, battery?.rightCharging, Modifier.weight(1f))
        BatteryDivider(Modifier.fillMaxHeight().padding(vertical = 10.dp))
        BatteryColumn("充电盒", battery?.casePercent, false, Modifier.weight(1f))
    }
}

@Composable
private fun BatteryColumn(
    label: String,
    percent: Int?,
    charging: Boolean?,
    modifier: Modifier,
) {
    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.fillMaxWidth().height(22.dp), contentAlignment = Alignment.Center) {
            Text(label, fontSize = 14.sp)
        }
        Box(Modifier.fillMaxWidth().height(22.dp), contentAlignment = Alignment.Center) {
            Text(
                text = if (percent != null) "$percent%" else "-",
                fontSize = 12.sp,
                color = Color(0x80000000),
            )
        }
        Box(
            modifier = Modifier.fillMaxWidth().height(25.dp).padding(top = 5.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            BatteryGlyph(
                percent = percent ?: 0,
                charging = charging == true,
                valid = percent != null,
            )
        }
    }
}

@Composable
private fun BatteryDivider(modifier: Modifier) {
    VerticalDivider(
        modifier = modifier,
        thickness = 0.7.dp,
    )
}

@Composable
private fun BatteryGlyph(percent: Int, charging: Boolean, valid: Boolean) {
    Canvas(Modifier.size(width = 26.dp, height = 14.dp)) {
        val outline = Color(0xFF777777)
        val bodyLeft = 0f
        val bodyTop = 1.dp.toPx()
        val bodyWidth = 20.dp.toPx()
        val bodyHeight = 10.dp.toPx()
        val terminalWidth = 1.5.dp.toPx()
        val stroke = 1.dp.toPx()
        drawRoundRect(
            color = outline,
            topLeft = androidx.compose.ui.geometry.Offset(bodyLeft, bodyTop),
            size = androidx.compose.ui.geometry.Size(bodyWidth, bodyHeight),
            cornerRadius = CornerRadius(1.7.dp.toPx(), 1.7.dp.toPx()),
            style = Stroke(width = stroke),
        )
        drawRoundRect(
            color = outline,
            topLeft =
                androidx.compose.ui.geometry.Offset(bodyLeft + bodyWidth, bodyTop + 3.3.dp.toPx()),
            size = androidx.compose.ui.geometry.Size(terminalWidth, 3.4.dp.toPx()),
            cornerRadius = CornerRadius(0.7.dp.toPx(), 0.7.dp.toPx()),
        )
        if (valid && percent > 0) {
            val fillWidth = (bodyWidth - 2.dp.toPx()) * (percent.coerceIn(0, 100) / 100f)
            drawRoundRect(
                color = if (percent > 20) Color(0xFF05C575) else Color(0xFFF22424),
                topLeft =
                    androidx.compose.ui.geometry.Offset(
                        bodyLeft + 1.dp.toPx(),
                        bodyTop + 1.dp.toPx(),
                    ),
                size = androidx.compose.ui.geometry.Size(fillWidth, bodyHeight - 2.dp.toPx()),
                cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx()),
            )
        }
        if (valid && charging) {
            val centerX = bodyLeft + bodyWidth / 2f
            val boltWidth = 6.dp.toPx()
            val bolt =
                Path().apply {
                    moveTo(centerX + boltWidth * 0.20f, bodyTop + 0.5.dp.toPx())
                    lineTo(centerX - boltWidth * 0.55f, bodyTop + bodyHeight * 0.54f)
                    lineTo(centerX - boltWidth * 0.05f, bodyTop + bodyHeight * 0.54f)
                    lineTo(centerX - boltWidth * 0.30f, bodyTop + bodyHeight - 0.5.dp.toPx())
                    lineTo(centerX + boltWidth * 0.65f, bodyTop + bodyHeight * 0.35f)
                    lineTo(centerX + boltWidth * 0.10f, bodyTop + bodyHeight * 0.35f)
                    close()
                }
            drawPath(bolt, color = Color.White)
        }
    }
}

private data class NoiseOption(
    val mode: Byte,
    val label: String,
    val iconRes: Int,
    val iconCheckedRes: Int,
)

@Composable
private fun NoiseSelector(value: Int?, onSelect: (Byte) -> Unit) {
    val options =
        listOf(
            NoiseOption(
                SpiConstants.NOISE_REDUCTION,
                "降噪",
                R.drawable.noise_btn_open,
                R.drawable.noise_btn_open_checked,
            ),
            NoiseOption(
                SpiConstants.NOISE_TRANSPARENT,
                "通透",
                R.drawable.noise_btn_trans,
                R.drawable.noise_btn_trans_checked,
            ),
            NoiseOption(
                SpiConstants.NOISE_NORMAL,
                "关闭",
                R.drawable.noise_btn_close,
                R.drawable.noise_btn_close_checked,
            ),
        )
    var selectedMode by remember { mutableStateOf(value) }
    LaunchedEffect(value) {
        if (value != null) selectedMode = value
    }
    Box(
        modifier = Modifier.fillMaxWidth().height(96.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            options.forEach { option ->
                NoiseButton(
                    option = option,
                    selected = selectedMode == option.mode.toInt(),
                    onClick = {
                        selectedMode = option.mode.toInt()
                        onSelect(option.mode)
                    },
                )
            }
        }
    }
}

@Composable
private fun NoiseButton(
    option: NoiseOption,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
            Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                )
                .padding(horizontal = 4.dp),
    ) {
        Image(
            painter = painterResource(if (selected) option.iconCheckedRes else option.iconRes),
            contentDescription = option.label,
            modifier = Modifier.size(50.dp),
        )
        Text(
            text = option.label,
            fontSize = 12.sp,
            color = if (selected) NOISE_BLUE else Color(0xFF757575),
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun ModeDropdown(
    title: String,
    modes: List<Pair<Byte, String>>,
    value: Int?,
    onSelect: (Byte) -> Unit,
) {
    var selectedIndex by remember { mutableStateOf(indexOf(modes, value)) }
    LaunchedEffect(value) {
        val idx = indexOf(modes, value)
        if (idx >= 0) selectedIndex = idx
    }
    OverlayDropdownPreference(
        title = title,
        items = modes.map { it.second },
        selectedIndex = selectedIndex,
        onSelectedIndexChange = { index ->
            selectedIndex = index
            onSelect(modes[index].first)
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun PromptLevelSlider(viewModel: ViewModel, earbudState: EarbudState) {
    var volume by remember { mutableStateOf((earbudState.alertVolume ?: 3).toFloat()) }
    LaunchedEffect(earbudState.alertVolume) {
        earbudState.alertVolume?.let { volume = it.toFloat() }
    }
    SliderPreference(
        title = "提示等级",
        value = volume,
        valueRange = 1f..5f,
        keyPoints = listOf(1f, 2f, 3f, 4f, 5f),
        steps = 3,
        valueText = volume.toInt().toString(),
        onValueChange = { volume = it },
        onValueChangeFinished = {
            viewModel.setAlertVolume(volume.toInt().toByte())
        },
    )
}
