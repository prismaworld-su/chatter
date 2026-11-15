package su.prismaworld.chatter.placeholder

/**
 * Переводит legacy-коды оформления в теги MiniMessage, оставляя остальной текст (в том числе теги MiniMessage) как есть.
 *
 * Поддерживаются коды с `&` и `§`:
 * - цвета `&0`-`&f`, оформление `&k`-`&o`, сброс `&r`;
 * - HEX-цвета `&#RRGGBB` и `&x&R&R&G&G&B&B`.
 *
 * Как и в legacy-формате, цвет сбрасывает оформление, заданное до него. Все открытые теги закрываются в конце текста,
 * поэтому оформление из legacy-кодов не выходит за его пределы. Символы `&` и `§`, за которыми не следует код,
 * остаются обычным текстом.
 */
internal object LegacyFormatting {

    private val COLORS = mapOf(
        '0' to "black",
        '1' to "dark_blue",
        '2' to "dark_green",
        '3' to "dark_aqua",
        '4' to "dark_red",
        '5' to "dark_purple",
        '6' to "gold",
        '7' to "gray",
        '8' to "dark_gray",
        '9' to "blue",
        'a' to "green",
        'b' to "aqua",
        'c' to "red",
        'd' to "light_purple",
        'e' to "yellow",
        'f' to "white",
    )

    private val DECORATIONS = mapOf(
        'k' to "obfuscated",
        'l' to "bold",
        'm' to "strikethrough",
        'n' to "underlined",
        'o' to "italic",
    )

    private const val RESET = 'r'
    private const val COLOR_TAG = "color"
    private const val HEX_LENGTH = 6

    fun toMiniMessage(text: String): String {
        if (text.none(::isMarker)) return text

        val result = StringBuilder(text.length + 32)
        // Открытые теги, последний - самый вложенный.
        val open = ArrayDeque<String>()

        fun closeAll() {
            while (open.isNotEmpty()) result.append("</").append(open.removeLast()).append('>')
        }

        fun openTag(name: String, argument: String? = null) {
            result.append('<').append(name)
            if (argument != null) result.append(':').append(argument)
            result.append('>')
            open.addLast(name)
        }

        var index = 0
        while (index < text.length) {
            val char = text[index]
            val code = text.getOrNull(index + 1)?.lowercaseChar()
            if (!isMarker(char) || code == null) {
                result.append(char)
                index++
                continue
            }

            val hex = hexColor(text, index)
            val color = COLORS[code]
            val decoration = DECORATIONS[code]
            when {
                hex != null -> {
                    closeAll()
                    openTag(COLOR_TAG, "#${hex.first}")
                    index += hex.second
                }
                color != null -> {
                    closeAll()
                    openTag(COLOR_TAG, color)
                    index += 2
                }
                decoration != null -> {
                    openTag(decoration)
                    index += 2
                }
                code == RESET -> {
                    closeAll()
                    index += 2
                }
                else -> {
                    result.append(char)
                    index++
                }
            }
        }
        closeAll()
        return result.toString()
    }

    /**
     * HEX-цвет, начинающийся с маркера в позиции [index]: `&#RRGGBB` или `&x&R&R&G&G&B&B`.
     *
     * @return цвет в нижнем регистре и длина кода или `null`, если в позиции нет HEX-цвета
     */
    private fun hexColor(text: String, index: Int): Pair<String, Int>? {
        when (text.getOrNull(index + 1)?.lowercaseChar()) {
            '#' -> {
                val end = index + 2 + HEX_LENGTH
                if (end > text.length) return null
                val hex = text.substring(index + 2, end)
                return if (hex.all(::isHexDigit)) hex.lowercase() to end - index else null
            }
            'x' -> {
                val end = index + 2 + HEX_LENGTH * 2
                if (end > text.length) return null
                val hex = StringBuilder(HEX_LENGTH)
                for (position in index + 2 until end step 2) {
                    if (!isMarker(text[position]) || !isHexDigit(text[position + 1])) return null
                    hex.append(text[position + 1].lowercaseChar())
                }
                return hex.toString() to end - index
            }
            else -> return null
        }
    }

    private fun isMarker(char: Char) = char == '&' || char == '§'

    private fun isHexDigit(char: Char) = char in '0'..'9' || char.lowercaseChar() in 'a'..'f'
}
