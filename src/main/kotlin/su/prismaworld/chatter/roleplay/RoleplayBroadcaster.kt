package su.prismaworld.chatter.roleplay

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Server
import org.bukkit.entity.Player
import su.prismaworld.chatter.channel.ChatAudience
import su.prismaworld.chatter.channel.ChatChannel
import su.prismaworld.chatter.config.ChatSettings
import su.prismaworld.chatter.module.ChatModules

/**
 * Отправляет отыгрыш из команд (`/me`, `/gme` и т.д.) по тем же правилам, что и отыгрыш из чата:
 * получателей определяет [ChatAudience], в тексте работают модули, сообщение попадает в консоль.
 *
 * Вызывается из основного потока.
 */
class RoleplayBroadcaster(
    private val server: Server,
    private val settings: () -> ChatSettings,
    private val audience: ChatAudience,
    private val modules: ChatModules,
) {

    private val miniMessage = MiniMessage.miniMessage()
    private val plainText = PlainTextComponentSerializer.plainText()

    fun send(sender: Player, action: RoleplayAction, channel: ChatChannel, text: String) {
        val settings = settings()

        if (!audience.canSend(sender, channel)) {
            sendMessage(sender, settings.globalDisabledMessage, settings)
            return
        }

        val message = modules.apply(sender, Component.text(text), settings.modules)
        // Сообщение могло опустеть, например если в нём был только :item: при пустой руке.
        if (plainText.serialize(message).isBlank()) {
            sendMessage(sender, settings.emptyMessage, settings)
            return
        }

        val rendered = RoleplayRenderer(action, channel, settings.roleplay).render(sender, sender.displayName(), message)
        val canHear = audience.canHear(sender, channel, settings)
        val recipients = server.onlinePlayers.filter { it != sender && canHear(it) }

        server.consoleSender.sendMessage(rendered)
        sender.sendMessage(rendered)
        recipients.forEach { it.sendMessage(rendered) }
        audience.onDelivered(sender, recipients, channel, plainText.serialize(message), settings)
    }

    /** Отправляет служебное сообщение. Пустая строка в конфиге отключает его. */
    private fun sendMessage(player: Player, message: String, settings: ChatSettings) {
        if (message.isEmpty()) return
        player.sendMessage(miniMessage.deserialize(message, Placeholder.unparsed("prefix", settings.globalPrefix)))
    }
}
