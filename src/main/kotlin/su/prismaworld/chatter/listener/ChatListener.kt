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
import su.prismaworld.chatter.config.ChatSettings

/**
 * Разделяет чат на локальный и глобальный.
 *
 * - Сообщение без префикса уходит в локальный чат: его видят только игроки
 *   в том же мире в пределах [ChatSettings.localRadius] блоков.
 * - Сообщение с префиксом [ChatSettings.globalPrefix] уходит всем, префикс при этом вырезается.
 */
class ChatListener(private val settings: () -> ChatSettings) : Listener {

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
            routeGlobal(event, settings, text.removePrefix(settings.globalPrefix).trim())
        } else {
            routeLocal(event, settings)
        }
    }

    /**
     * Сообщает игроку, что его локальное сообщение никто не услышал. Выполняется последним,
     * чтобы учесть получателей, отфильтрованных другими плагинами, и не срабатывать на отменённые сообщения.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onChatMonitor(event: AsyncChatEvent) {
        val settings = settings()
        if (settings.nobodyHeardMessage.isEmpty() || isGlobal(event, settings)) return

        val sender = event.player
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

    private fun routeGlobal(event: AsyncChatEvent, settings: ChatSettings, content: String) {
        if (content.isEmpty()) {
            event.isCancelled = true
            if (settings.emptyGlobalMessage.isNotEmpty()) {
                event.player.sendMessage(
                    miniMessage.deserialize(
                        settings.emptyGlobalMessage,
                        Placeholder.unparsed("prefix", settings.globalPrefix),
                    )
                )
            }
            return
        }

        event.message(Component.text(content))
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

    /** Глобальность определяется по исходному сообщению, т.к. из текущего префикс уже вырезан. */
    private fun isGlobal(event: AsyncChatEvent, settings: ChatSettings): Boolean =
        plainText.serialize(event.originalMessage()).startsWith(settings.globalPrefix)

    private inner class FormatRenderer(private val format: String) : ChatRenderer.ViewerUnaware {
        override fun render(source: Player, sourceDisplayName: Component, message: Component): Component =
            miniMessage.deserialize(
                format,
                Placeholder.component("player", sourceDisplayName),
                Placeholder.component("message", message),
            )
    }
}
