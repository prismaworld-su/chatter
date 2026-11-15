package su.prismaworld.chatter.channel

import su.prismaworld.chatter.config.ChatSettings

/**
 * Разобранное сообщение из чата: канал и текст без префиксов.
 *
 * - [ChatSettings.globalPrefix] отправляет сообщение в канал, противоположный выбранному.
 *   Если после него нет текста, [content] пуст.
 * - Сообщение без префиксов уходит в выбранный канал без изменений.
 */
data class ChatRoute(val channel: ChatChannel, val content: String) {

    companion object {
        fun parse(text: String, selected: ChatChannel, settings: ChatSettings): ChatRoute {
            val globalPrefix = settings.globalPrefix

            return if (text.startsWith(globalPrefix)) {
                ChatRoute(selected.opposite, text.removePrefix(globalPrefix).trim())
            } else {
                ChatRoute(selected, text)
            }
        }
    }
}
