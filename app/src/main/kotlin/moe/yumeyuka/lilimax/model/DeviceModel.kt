package moe.yumeyuka.lilimax.model

enum class ProtocolFamily {
    M8,
    M3,
    AB,
}

enum class DeviceModel(
    val displayName: String,
    val protocolFamily: ProtocolFamily,
) {
    GLAZE_MAX("MISHUAI Glaze Max", ProtocolFamily.M8),
    AWAKEN_MAX("MISHUAI Awaken Max", ProtocolFamily.M3),
    MS_M30("MISHUAI MS-M30", ProtocolFamily.AB),
    MS_M88("MISHUAI MS-M88", ProtocolFamily.AB),
    MS_MP10("MISHUAI MS-MP10", ProtocolFamily.AB),
    MS_MP12("MISHUAI MS-MP12", ProtocolFamily.AB),
    MS_M2("MISHUAI MS-M2", ProtocolFamily.AB),
    MS_M3("MISHUAI MS-M3", ProtocolFamily.AB),
    MS_R3("MISHUAI MS-R3", ProtocolFamily.AB),
    MS_MP16("MISHUAI MS-MP16", ProtocolFamily.AB),
    MS_R3C("MISHUAI MS-R3c", ProtocolFamily.AB),
    MS_MP10_V2("MISHUAI MS-MP10", ProtocolFamily.AB);

    companion object {
        fun looksLikeMishuaiName(name: String?): Boolean {
            val normalized = name?.trim()?.lowercase() ?: return false
            return normalized.contains("mishuai") ||
                normalized.contains("glaze max") ||
                normalized.contains("awaken max")
        }

        fun fromBluetoothName(name: String?): DeviceModel? {
            val normalized = name?.trim()?.lowercase() ?: return null
            return entries.firstOrNull { model ->
                normalized == model.displayName.lowercase() ||
                    when (model) {
                        GLAZE_MAX ->
                            normalized.contains("glaze max") ||
                                normalized == "mishuai m8" ||
                                (normalized.contains("m8") &&
                                    !normalized.contains("m88") &&
                                    !normalized.contains("mp8"))
                        AWAKEN_MAX ->
                            normalized.contains("awaken max") || normalized == "mishuai m3"
                        MS_M30 -> normalized.contains("ms-m30")
                        MS_M88 -> normalized.contains("ms-m88")
                        MS_MP10,
                        MS_MP10_V2 -> normalized.contains("ms-mp10")
                        MS_MP12 -> normalized.contains("ms-mp12")
                        MS_M2 -> normalized.contains("ms-m2")
                        MS_M3 -> normalized.contains("ms-m3")
                        MS_R3 -> normalized.contains("ms-r3") && !normalized.contains("r3c")
                        MS_MP16 -> normalized.contains("ms-mp16")
                        MS_R3C -> normalized.contains("ms-r3c") || normalized == "mishuai r3c"
                    }
            }
        }
    }
}
