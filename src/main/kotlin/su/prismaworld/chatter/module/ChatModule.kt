package su.prismaworld.chatter.module

import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import su.prismaworld.chatter.config.ModuleSettings

/**
 * Модуль сообщения: заменяет все вхождения [token] в тексте игрока на интерактивный компонент.
 *
 * Модули вызываются из асинхронного потока чата.
 */
interface ChatModule {
    /** Текст, который игрок пишет в сообщении, например `:loc:`. */
    val token: String

    fun isEnabled(settings: ModuleSettings): Boolean

    /** Компонент, которым заменяется [token]. Пустой компонент удаляет [token] из сообщения. */
    fun render(player: Player, settings: ModuleSettings): Component
}
