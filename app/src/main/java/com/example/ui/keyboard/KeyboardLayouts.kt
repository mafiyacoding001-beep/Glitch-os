package com.example.ui.keyboard

object KeyboardLayouts {

    val QWERTY = KeyboardLayout(
        rows = listOf(
            KeyboardRow(
                listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0").map { KeyModel(it, it) }
            ),
            KeyboardRow(
                listOf(
                    KeyModel("q", "q", subLabel = "1", longPressOptions = listOf("1", "q")),
                    KeyModel("w", "w", subLabel = "2", longPressOptions = listOf("2", "w")),
                    KeyModel("e", "e", subLabel = "3", longPressOptions = listOf("3", "é", "è", "ê", "ë")),
                    KeyModel("r", "r", subLabel = "4", longPressOptions = listOf("4", "r")),
                    KeyModel("t", "t", subLabel = "5", longPressOptions = listOf("5", "t")),
                    KeyModel("y", "y", subLabel = "6", longPressOptions = listOf("6", "y")),
                    KeyModel("u", "u", subLabel = "7", longPressOptions = listOf("7", "û", "ü", "ù", "ú")),
                    KeyModel("i", "i", subLabel = "8", longPressOptions = listOf("8", "î", "ï", "í", "ì")),
                    KeyModel("o", "o", subLabel = "9", longPressOptions = listOf("9", "ô", "ö", "ò", "ó", "œ")),
                    KeyModel("p", "p", subLabel = "0", longPressOptions = listOf("0", "p"))
                )
            ),
            KeyboardRow(
                listOf(
                    KeyModel("a", "a", subLabel = "@", longPressOptions = listOf("@", "à", "á", "â", "ä", "æ")),
                    KeyModel("s", "s", subLabel = "#", longPressOptions = listOf("#", "ß", "s")),
                    KeyModel("d", "d", subLabel = "$", longPressOptions = listOf("$", "d")),
                    KeyModel("f", "f", subLabel = "%", longPressOptions = listOf("%", "f")),
                    KeyModel("g", "g", subLabel = "&", longPressOptions = listOf("&", "g")),
                    KeyModel("h", "h", subLabel = "-", longPressOptions = listOf("-", "h")),
                    KeyModel("j", "j", subLabel = "+", longPressOptions = listOf("+", "j")),
                    KeyModel("k", "k", subLabel = "(", longPressOptions = listOf("(", "k")),
                    KeyModel("l", "l", subLabel = ")", longPressOptions = listOf(")", "l"))
                )
            ),
            KeyboardRow(
                listOf(
                    KeyModel("SHIFT", "⇧", type = KeyType.SHIFT, weight = 1.3f),
                    KeyModel("z", "z", subLabel = "*", longPressOptions = listOf("*", "z")),
                    KeyModel("x", "x", subLabel = "\"", longPressOptions = listOf("\"", "x")),
                    KeyModel("c", "c", subLabel = "'", longPressOptions = listOf("'", "ç")),
                    KeyModel("v", "v", subLabel = ":", longPressOptions = listOf(":", "v")),
                    KeyModel("b", "b", subLabel = ";", longPressOptions = listOf(";", "b")),
                    KeyModel("n", "n", subLabel = "!", longPressOptions = listOf("!", "ñ")),
                    KeyModel("m", "m", subLabel = "?", longPressOptions = listOf("?", "m")),
                    KeyModel("DELETE", "⌫", type = KeyType.BACKSPACE, weight = 1.3f)
                )
            ),
            KeyboardRow(
                listOf(
                    KeyModel("123", "?123", type = KeyType.MODE_SWITCH, weight = 1.3f),
                    KeyModel("EMOJI", "😊", type = KeyType.EMOJI_SWITCH, weight = 1.0f),
                    KeyModel(",", ",", longPressOptions = listOf(",", "<", "settings")),
                    KeyModel("SPACE", "English", type = KeyType.SPACE, weight = 3.5f),
                    KeyModel(".", ".", longPressOptions = listOf(".", ">", "/", "?", "!", "@", "com")),
                    KeyModel("ENTER", "↵", type = KeyType.ENTER, weight = 1.5f)
                )
            )
        )
    )

    val NUMERIC = KeyboardLayout(
        rows = listOf(
            KeyboardRow(
                listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0").map { KeyModel(it, it) }
            ),
            KeyboardRow(
                listOf("@", "#", "$", "_", "&", "-", "+", "(", ")", "/").map { KeyModel(it, it) }
            ),
            KeyboardRow(
                listOf(
                    KeyModel("SYM", "=\\<", type = KeyType.MODE_SWITCH, weight = 1.3f),
                    KeyModel("*", "*"), KeyModel("\"", "\""), KeyModel("'", "'"),
                    KeyModel(":", ":"), KeyModel(";", ";"), KeyModel("!", "!"), KeyModel("?", "?"),
                    KeyModel("DELETE", "⌫", type = KeyType.BACKSPACE, weight = 1.3f)
                )
            ),
            KeyboardRow(
                listOf(
                    KeyModel("ABC", "ABC", type = KeyType.MODE_SWITCH, weight = 1.3f),
                    KeyModel("EMOJI", "😊", type = KeyType.EMOJI_SWITCH, weight = 1.0f),
                    KeyModel(",", ","),
                    KeyModel("SPACE", "Space", type = KeyType.SPACE, weight = 3.5f),
                    KeyModel(".", "."),
                    KeyModel("ENTER", "↵", type = KeyType.ENTER, weight = 1.5f)
                )
            )
        )
    )

    val SYMBOLS = KeyboardLayout(
        rows = listOf(
            KeyboardRow(
                listOf("~", "`", "|", "•", "√", "π", "÷", "×", "¶", "∆").map { KeyModel(it, it) }
            ),
            KeyboardRow(
                listOf("£", "¢", "€", "¥", "^", "°", "=", "{", "}", "\\").map { KeyModel(it, it) }
            ),
            KeyboardRow(
                listOf(
                    KeyModel("123", "123", type = KeyType.MODE_SWITCH, weight = 1.3f),
                    KeyModel("%", "%"), KeyModel("©", "©"), KeyModel("®", "®"),
                    KeyModel("™", "™"), KeyModel("✓", "✓"), KeyModel("[", "["), KeyModel("]", "]"),
                    KeyModel("DELETE", "⌫", type = KeyType.BACKSPACE, weight = 1.3f)
                )
            ),
            KeyboardRow(
                listOf(
                    KeyModel("ABC", "ABC", type = KeyType.MODE_SWITCH, weight = 1.3f),
                    KeyModel("EMOJI", "😊", type = KeyType.EMOJI_SWITCH, weight = 1.0f),
                    KeyModel("<", "<"),
                    KeyModel("SPACE", "Space", type = KeyType.SPACE, weight = 3.5f),
                    KeyModel(">", ">"),
                    KeyModel("ENTER", "↵", type = KeyType.ENTER, weight = 1.5f)
                )
            )
        )
    )
}
