package moe.yumeyuka.lilimax.model

data class BatteryState(
    val leftPercent: Int,
    val rightPercent: Int,
    val casePercent: Int,
    val leftCharging: Boolean,
    val rightCharging: Boolean,
)

data class EarbudState(
    val battery: BatteryState? = null,
    val firmwareVersion: Long? = null,
    val bluetoothName: String? = null,
    val eqMode: Int? = null,
    val workMode: Int? = null,
    val language: Int? = null,
    val audioProtocol: Int? = null,
    val noiseControl: Int? = null,
    val ancEnabled: Boolean? = null,
    val touchEnabled: Boolean? = null,
    val finderMode: Int? = null,
    val alertVolume: Int? = null,
)
