package su.prismaworld.chatter.roleplay

import io.papermc.paper.chat.ChatRenderer
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.entity.Player
import su.prismaworld.chatter.channel.ChatChannel
import su.prismaworld.chatter.config.RoleplaySettings
import java.util.concurrent.ThreadLocalRandom

/**
 * Оформляет сообщение отыгрыша по формату из конфига. Используется и чатом, и командами.
 *
 * Исход `/try` определяется один раз при создании, поэтому все получатели видят одинаковый результат.
 */
class RoleplayRenderer(action: RoleplayAction, channel: ChatChannel, settings: RoleplaySettings) : ChatRenderer.ViewerUnaware {

    private val format = settings.format(channel, action)
    private val result = when (action) {
        RoleplayAction.TRY -> if (ThreadLocalRandom.current().nextBoolean()) settings.trySuccess else settings.tryFailure
        RoleplayAction.ME, RoleplayAction.DO -> ""
    }

    override fun render(source: Player, sourceDisplayName: Component, message: Component): Component =
        MINI_MESSAGE.deserialize(
            format,
            Placeholder.component("player", sourceDisplayName),
            Placeholder.component("message", message),
            Placeholder.parsed("result", result),
        )

    private companion object {
        val MINI_MESSAGE = MiniMessage.miniMessage()
    }
}
