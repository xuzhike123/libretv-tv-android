package io.github.xuzhike123.libretvtv.data

enum class AccentTheme { TEAL, DARK_GREY, RED }

enum class CardStyle { SINGLE_LINE_SCROLL, TWO_LINE }

enum class BufferLevel { NONE, LOW, MEDIUM, HIGH }

/**
 * 界面与播放偏好。本版本只保存在内存中（进程结束即还原），
 * 尚未接入 DataStore / SharedPreferences。
 */
data class UiSettings(
    val accent: AccentTheme = AccentTheme.TEAL,
    val uiScale: Float = 1.0f,
    val cardStyle: CardStyle = CardStyle.SINGLE_LINE_SCROLL,
    val animatedPreview: Boolean = true,
    val autoHideUi: Boolean = false,
    val playbackSpeed: Float = 1.0f,
    val bufferLevel: BufferLevel = BufferLevel.MEDIUM,
    val afrEnabled: Boolean = false,
    val autoPlayNext: Boolean = true,
    val healthBasedDisable: Boolean = true,
    val keepHistory: Boolean = true
)

const val MIN_UI_SCALE = 0.8f
const val MAX_UI_SCALE = 1.4f
const val UI_SCALE_STEP = 0.05f

val PLAYBACK_SPEEDS = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
