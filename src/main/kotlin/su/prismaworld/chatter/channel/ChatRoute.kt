package su.prismaworld.chatter.channel

import su.prismaworld.chatter.config.ChatSettings
import su.prismaworld.chatter.roleplay.RoleplayAction

/**
 * Разобранное сообщение из чата: канал, вид отыгрыша (`null` для обычного сообщения) и текст без префиксов.
 *
 * - Префикс отыгрыша (`*`, `?`, `=`) отправляет отыгрыш в локальный канал, а вместе с [ChatSettings.globalPrefix]
 *   перед ним (`!*`) - в глобальный, независимо от канала, выбранного игроком. Префикс отыгрыша без текста
 *   префиксом не считается, и сообщение разбирается как обычное.
 * - [ChatSettings.globalPrefix] отправляет обычное сообщение в канал, противоположный выбранному.
 *   Если после него нет текста, [content] пуст.
 * - Сообщение без префиксов уходит в выбранный канал без изменений.
 */
data class ChatRoute(val channel: ChatChannel, val roleplay: RoleplayAction?, val content: String) {

    companion object {
        fun parse(text: String, selected: ChatChannel, settings: ChatSettings): ChatRoute {
            val globalPrefix = settings.globalPrefix

            settings.roleplay.prefixes.forEach { (action, prefix) ->
                roleplay(text, globalPrefix + prefix, action, ChatChannel.GLOBAL)?.let { return it }
                roleplay(text, prefix, action, ChatChannel.LOCAL)?.let { return it }
            }

            return if (text.startsWith(globalPrefix)) {
                ChatRoute(selected.opposite, null, text.removePrefix(globalPrefix).trim())
            } else {
                ChatRoute(selected, null, text)
            }
        }

        private fun roleplay(text: String, prefix: String, action: RoleplayAction, channel: ChatChannel): ChatRoute? {
            if (!text.startsWith(prefix)) return null
            val content = text.removePrefix(prefix).trim()
            return if (content.isEmpty()) null else ChatRoute(channel, action, content)
        }
    }
}
