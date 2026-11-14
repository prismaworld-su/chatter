package su.prismaworld.chatter.module

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.TextReplacementConfig
import org.bukkit.entity.Player
import su.prismaworld.chatter.config.ModuleSettings

/** Применяет к сообщению все включённые модули. */
class ChatModules(private val modules: List<ChatModule> = listOf(LocationModule(), ItemModule())) {

    fun apply(player: Player, message: Component, settings: ModuleSettings): Component =
        modules.filter { it.isEnabled(settings) }.fold(message) { result, module ->
            // Компонент строится один раз на сообщение и только если токен в нём действительно есть.
            val rendered by lazy { module.render(player, settings) }
            result.replaceText(
                TextReplacementConfig.builder()
                    .matchLiteral(module.token)
                    .replacement { _: TextComponent.Builder -> rendered }
                    .build()
            )
        }
}
