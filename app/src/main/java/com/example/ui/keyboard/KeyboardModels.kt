package com.example.ui.keyboard

enum class KeyType {
    LETTER,
    SPACE,
    BACKSPACE,
    ENTER,
    SHIFT,
    MODE_SWITCH, // Switch between QWERTY, 123, Symbol
    EMOJI_SWITCH,
    CLIPBOARD_SWITCH,
    CURSOR_PAD_SWITCH,
    ACTION
}

enum class ShiftState {
    OFF,
    ONCE,
    CAPS_LOCK
}

enum class KeyboardMode {
    QWERTY,
    NUMERIC,
    SYMBOLS
}

enum class ActivePanel {
    KEYBOARD,
    EMOJI,
    CLIPBOARD,
    CURSOR_PAD,
    AUTO_TYPE,
    AI_MAGIC,
    CALCULATOR,
    SNIPPETS,
    KAOMOJI_SYMBOLS,
    APK_EXPLORER,
    SETTINGS
}

enum class AutoTypeState {
    IDLE,
    RUNNING,
    STOPPING,
    COMPLETED
}

data class KeyModel(
    val code: String,
    val label: String,
    val shiftedLabel: String = label.uppercase(),
    val subLabel: String? = null,
    val type: KeyType = KeyType.LETTER,
    val longPressOptions: List<String> = emptyList(),
    val weight: Float = 1.0f
)

data class KeyboardRow(
    val keys: List<KeyModel>
)

data class KeyboardLayout(
    val rows: List<KeyboardRow>
)

data class WordSuggestion(
    val word: String,
    val score: Int = 0,
    val isAutoCorrect: Boolean = false
)

data class ClipboardItem(
    val id: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false
)

data class SnippetItem(
    val id: String,
    val title: String,
    val category: String,
    val content: String,
    val isFavorite: Boolean = false
)

data class CalculatorHistoryItem(
    val expression: String,
    val result: String,
    val timestamp: Long = System.currentTimeMillis()
)
