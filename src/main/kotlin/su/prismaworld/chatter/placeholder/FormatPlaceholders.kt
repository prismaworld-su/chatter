package su.prismaworld.chatter.placeholder

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.entity.Player

/**
 * Подставляет плейсхолдеры (PlaceholderAPI) в форматы MiniMessage из конфига.
 *
 * - `%identifier_params%` заменяется значением для автора сообщения до разбора MiniMessage, поэтому работает
 *   в любом месте формата, в том числе в аргументах тегов (`<hover>`, `<click>`), см. [apply].
 * - Тег `<имя:identifier_params>` подставляет значение для другого игрока, например получателя личного сообщения, см. [tag].
 * - Значение может содержать теги MiniMessage и legacy-коды (`&c`, `§c`, `&#RRGGBB`), см. [LegacyFormatting].
 * - Нераспознанный плейсхолдер остаётся в тексте как есть.
 *
 * Текст игроков сюда не передаётся: он вставляется в формат готовым компонентом, поэтому плейсхолдеры в нём
 * не раскрываются. Вызывается в том потоке, где оформляется сообщение, в том числе в асинхронном потоке чата.
 */
class FormatPlaceholders(private val source: PlaceholderSource) {

    /** Подставляет в [format] значения плейсхолдеров `%identifier_params%` для [player]. */
    fun apply(format: String, player: Player): String = PlaceholderText.replace(format) { value(player, it) }

    /**
     * Тег `<[name]:identifier_params>`, подставляющий значение плейсхолдера для [player].
     * Двоеточия в плейсхолдере допустимы: `<[name]:server_time_HH:mm>`.
     */
    fun tag(name: String, player: Player): TagResolver = TagResolver.resolver(name) { arguments, context ->
        val placeholder = buildList {
            while (arguments.hasNext()) add(arguments.pop().value())
        }.joinToString(":")
        if (placeholder.isEmpty()) {
            throw context.newException("Не указан плейсхолдер: <$name:плейсхолдер>", arguments)
        }

        val value = value(player, placeholder)
        if (value == null) {
            Tag.selfClosingInserting(Component.text("%$placeholder%"))
        } else {
            // Значение разбирается вместе с форматом, как и при подстановке %identifier_params%.
            Tag.preProcessParsed(value)
        }
    }

    private fun value(player: Player, placeholder: String): String? =
        source.value(player, placeholder)?.let(LegacyFormatting::toMiniMessage)
}
