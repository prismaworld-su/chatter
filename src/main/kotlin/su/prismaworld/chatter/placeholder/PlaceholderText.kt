package su.prismaworld.chatter.placeholder

/**
 * Поиск плейсхолдеров `%identifier_params%` в тексте по тем же правилам, что и в PlaceholderAPI:
 *
 * - плейсхолдер - текст между двумя ближайшими знаками `%`, содержащий `_`;
 * - если пробел встречается раньше первого `_`, открывающий `%` считается обычным текстом,
 *   а закрывающий может открывать следующий плейсхолдер (так `50% скидка для %player_name%` разбирается верно);
 * - текст между `%` без `_` плейсхолдером не считается и остаётся как есть.
 */
internal object PlaceholderText {

    /**
     * Заменяет плейсхолдеры в [text] результатом [replacement]. Ему передаётся плейсхолдер без знаков `%`.
     * Если [replacement] вернул `null`, плейсхолдер остаётся в тексте как есть.
     */
    fun replace(text: String, replacement: (String) -> String?): String {
        var start = text.indexOf('%')
        if (start == -1) return text

        val result = StringBuilder(text.length)
        // Текст до cursor уже перенесён в result. Он переносится только при замене, остальной текст копируется в конце.
        var cursor = 0
        while (start != -1) {
            val end = text.indexOf('%', start + 1)
            if (end == -1) break

            val placeholder = text.substring(start + 1, end)
            val underscore = placeholder.indexOf('_')
            val space = placeholder.indexOf(' ')
            if (space != -1 && (underscore == -1 || space < underscore)) {
                start = end
                continue
            }

            if (underscore != -1) {
                val value = replacement(placeholder)
                if (value != null) {
                    result.append(text, cursor, start).append(value)
                    cursor = end + 1
                }
            }
            start = text.indexOf('%', end + 1)
        }

        if (cursor == 0) return text
        return result.append(text, cursor, text.length).toString()
    }
}
