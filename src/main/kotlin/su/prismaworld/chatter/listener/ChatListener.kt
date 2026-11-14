package su.prismaworld.chatter.listener

import io.papermc.paper.chat.ChatRenderer
import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import su.prismaworld.chatter.channel.ChannelStorage
import su.prismaworld.chatter.channel.ChatChannel
import su.prismaworld.chatter.config.ChatSettings

/**
 * Разделяет чат на локальный и глобальный.
 *
 * - Сообщение без префикса уходит в канал, выбранный игроком через `/chat`
 *   (по умолчанию [ChatSettings.defaultChannel]).
 * - Сообщение с префиксом [ChatSettings.globalPrefix] уходит в противоположный канал, префикс при этом вырезается.
 * - Локальное сообщение видят только игроки в том же мире в пределах [ChatSettings.localRadius] блоков,
 *   глобальное - все.
 */
class ChatListener(
    private val settings: () -> ChatSettings,
    private val storage: ChannelStorage,
) : Listener {

    private val miniMessage = MiniMessage.miniMessage()
    private val plainText = PlainTextComponentSerializer.plainText()

    /**
     * Выполняется раньше остальных обработчиков, чтобы другие плагины (модерация, фильтры)
     * получали уже очищенный от префикса текст и итоговый список получателей.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onChat(event: AsyncChatEvent) {
        val settings = settings()
        val text = plainText.serialize(event.message())

        if (text.startsWith(settings.globalPrefix)) {
            val content = text.removePrefix(settings.globalPrefix).trim()
            if (content.isEmpty()) {
                rejectEmpty(event, settings)
                return
            }
            event.message(Component.text(content))
        }

        when (resolveChannel(event.player, text, settings)) {
            ChatChannel.LOCAL -> routeLocal(event, settings)
            ChatChannel.GLOBAL -> routeGlobal(event, settings)
        }
    }

    /**
     * Сообщает игроку, что его локальное сообщение никто не услышал. Выполняется последним,
     * чтобы учесть получателей, отфильтрованных другими плагинами, и не срабатывать на отменённые сообщения.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onChatMonitor(event: AsyncChatEvent) {
        val settings = settings()
        if (settings.nobodyHeardMessage.isEmpty()) return

        // Канал определяется по исходному сообщению, т.к. из текущего префикс уже вырезан.
        val sender = event.player
        val channel = resolveChannel(sender, plainText.serialize(event.originalMessage()), settings)
        if (channel != ChatChannel.LOCAL) return

        val heardBySomeone = event.viewers().any { it is Player && it != sender }
        if (!heardBySomeone) {
            sender.sendMessage(
                miniMessage.deserialize(
                    settings.nobodyHeardMessage,
                    Placeholder.unparsed("prefix", settings.globalPrefix),
                )
            )
        }
    }

    /** Префикс отправляет сообщение в канал, противоположный выбранному игроком. */
    private fun resolveChannel(player: Player, text: String, settings: ChatSettings): ChatChannel {
        val selected = storage.get(player)
        return if (text.startsWith(settings.globalPrefix)) selected.opposite else selected
    }

    private fun rejectEmpty(event: AsyncChatEvent, settings: ChatSettings) {
        event.isCancelled = true
        if (settings.emptyGlobalMessage.isNotEmpty()) {
            event.player.sendMessage(
                miniMessage.deserialize(
                    settings.emptyGlobalMessage,
                    Placeholder.unparsed("prefix", settings.globalPrefix),
                )
            )
        }
    }

    private fun routeGlobal(event: AsyncChatEvent, settings: ChatSettings) {
        event.renderer(ChatRenderer.viewerUnaware(FormatRenderer(settings.globalFormat)))
    }

    private fun routeLocal(event: AsyncChatEvent, settings: ChatSettings) {
        val sender = event.player
        val origin = sender.location
        val world = origin.world

        // Консоль и прочие не-игровые получатели остаются, чтобы локальный чат попадал в логи.
        event.viewers().removeIf { viewer ->
            viewer is Player && viewer != sender &&
                (viewer.world != world || viewer.location.distanceSquared(origin) > settings.localRadiusSquared)
        }
        event.renderer(ChatRenderer.viewerUnaware(FormatRenderer(settings.localFormat)))
    }

    private inner class FormatRenderer(private val format: String) : ChatRenderer.ViewerUnaware {
        override fun render(source: Player, sourceDisplayName: Component, message: Component): Component =
            miniMessage.deserialize(
                format,
                Placeholder.component("player", sourceDisplayName),
                Placeholder.component("message", message),
            )
    }
}
