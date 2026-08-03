package moe.yumeyuka.lilimax.protocol

object SpiConstants {

    const val SPP_UUID = "158627bc-0547-8787-87ba-435ad8571238"

    const val HEADER_FIXED_0: Byte = 0x01
    const val HEADER_FIXED_1: Byte = 0x00
    const val RESPONSE_HEADER_FIXED_0: Byte = 0x02

    const val CMD_GET_INFO: Byte = 0x27
    const val CMD_SOUND_EFFECTS: Byte = 0x20
    const val CMD_POWER_OFF: Byte = 0x23
    const val CMD_RESET: Byte = 0x24
    const val CMD_WORK_MODE: Byte = 0x25
    const val CMD_LANGUAGE: Byte = 0x29
    const val CMD_FINDER: Byte = 0x2A
    const val CMD_AUDIO_PROTOCOL: Byte = 0x2B
    const val CMD_NOISE_CONTROL: Byte = 0x2C
    const val CMD_CLEAR_PAIR: Byte = 0x2F
    const val CMD_ALERT_VOLUME: Byte = 0x32
    const val CMD_PUSH_NOTIFY: Byte = 0x28

    const val SUB_PACKAGE_LEN: Byte = 0xFF.toByte()
    const val SUB_BATTERY: Byte = 0x01
    const val SUB_VERSION: Byte = 0x02
    const val SUB_NAME: Byte = 0x03
    const val SUB_EQ_MODE: Byte = 0x04
    const val SUB_KEY_MODE: Byte = 0x05
    const val SUB_ANC_SWITCH: Byte = 0x07
    const val SUB_WORK_MODE: Byte = 0x08
    const val SUB_LANGUAGE: Byte = 0x0A
    const val SUB_AUDIO_PROTOCOL: Byte = 0x0B
    const val SUB_AUDIO_PROTOCOL_PUSH: Byte = 0x28
    const val SUB_NOISE_CONTROL: Byte = 0x0C
    const val SUB_ALERT_VOLUME: Byte = 0x13
    const val SUB_FINDER: Byte = 0x14
    const val SUB_TOUCH_SWITCH: Byte = 0x16

    const val AUDIO_DUAL_DEVICE: Byte = 0x00
    const val AUDIO_LDAC: Byte = 0x01
    const val AUDIO_AAC: Byte = 0x02
    const val AUDIO_LHDC: Byte = 0x03
    const val AUDIO_LC3: Byte = 0x04

    val AUDIO_PROTOCOL_NAMES =
        mapOf(
            AUDIO_DUAL_DEVICE to "双设备连接",
            AUDIO_LDAC to "LDAC",
            AUDIO_AAC to "AAC",
            AUDIO_LHDC to "LHDC",
            AUDIO_LC3 to "LC3",
        )

    const val NOISE_WIND: Byte = 0x00
    const val NOISE_REDUCTION: Byte = 0x01
    const val NOISE_TRANSPARENT: Byte = 0x02
    const val NOISE_NORMAL: Byte = 0x03

    const val WORK_MUSIC: Byte = 0x00
    const val WORK_GAME: Byte = 0x01

    const val EQ_HIFI: Byte = 0x00
    const val EQ_POP: Byte = 0x01
    const val EQ_ROCK: Byte = 0x02
    const val EQ_FPS: Byte = 0x03
    const val EQ_LC: Byte = 0x04
    const val EQ_CUSTOM: Byte = 0x05

    const val LANG_CHINESE: Byte = 0x00
    const val LANG_ENGLISH: Byte = 0x01

    const val FINDER_OFF: Byte = 0x00
    const val FINDER_LEFT: Byte = 0x01
    const val FINDER_RIGHT: Byte = 0x02
    const val FINDER_BOTH: Byte = 0x03
}
