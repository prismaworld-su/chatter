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
import su.prismaworld.chatter.channel.ChatAudience
import su.prismaworld.chatter.channel.ChatChannel
import su.prismaworld.chatter.channel.ChatRoute
import su.prismaworld.chatter.config.ChatSettings
import su.prismaworld.chatter.module.ChatModules
import su.prismaworld.chatter.preference.PreferenceManager

/**
 * Разделяет чат на локальный и глобальный.
 *
 * - Канал и текст без префиксов определяются по правилам [ChatRoute]: сообщение без префикса уходит
 *   в канал, выбранный игроком через `/chat` (по умолчанию [ChatSettings.defaultChannel]).
 * - Кто слышит сообщение, определяет [ChatAudience]: локальное - игроки поблизости,
 *   глобальное - все, у кого глобальный чат не отключён в настройках.
 * - Модули (`:loc:`, `:item:`) заменяются интерактивными компонентами, см. [ChatModules].
 * - Игроки с включённым звуком упоминания слышат звук, когда их ник есть в сообщении.
 */
class ChatListener(
    private val settings: () -> ChatSettings,
    private val preferences: PreferenceManager,
    private val audience: ChatAudience,
    private val modules: ChatModules,
) : Listener {

    private val miniMessage = MiniMessage.miniMessage()
    private val plainText = PlainTextComponentSerializer.plainText()

    /**
     * Выполняется раньше остальных обработчиков, чтобы другие плагины (модерация, фильтры)
     * получали уже очищенный от префиксов текст и итоговый список получателей.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onChat(event: AsyncChatEvent) {
        val settings = settings()
        val sender = event.player
        val text = plainText.serialize(event.message())
        val route = ChatRoute.parse(text, preferences.channel(sender), settings)

        // Непустой текст опустел после разбора, только если в нём был один префикс глобального чата.
        if (text.isNotEmpty() && route.content.isEmpty()) {
            reject(event, settings.emptyGlobalMessage, settings)
            return
        }
        // Сообщение без префиксов не пересоздаётся, чтобы сохранить его как есть.
        if (route.content != text) {
            event.message(Component.text(route.content))
        }

        event.message(modules.apply(sender, event.message(), settings.modules))
        // Сообщение могло опустеть, например если в нём был только :item: при пустой руке.
        if (plainText.serialize(event.message()).isBlank()) {
            reject(event, settings.emptyMessage, settings)
            return
        }

        if (!audience.canSend(sender, route.channel)) {
            reject(event, settings.globalDisabledMessage, settings)
            return
        }

        // Консоль и прочие не-игровые получатели остаются, чтобы чат попадал в логи.
        val canHear = audience.canHear(sender, route.channel, settings)
        event.viewers().removeIf { viewer -> viewer is Player && !canHear(viewer) }

        event.renderer(ChatRenderer.viewerUnaware(FormatRenderer(format(route.channel, settings))))
    }

    /**
     * Выполняется последним, чтобы учесть получателей, отфильтрованных другими плагинами,
     * и не срабатывать на отменённые сообщения.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onChatMonitor(event: AsyncChatEvent) {
        val settings = settings()
        val sender = event.player
        val recipients = event.viewers().filterIsInstance<Player>().filter { it != sender }

        // Канал определяется по исходному сообщению, т.к. из текущего префиксы уже вырезаны.
        val route = ChatRoute.parse(plainText.serialize(event.originalMessage()), preferences.channel(sender), settings)
        audience.onDelivered(sender, recipients, route.channel, plainText.serialize(event.message()), settings)
    }

    private fun format(channel: ChatChannel, settings: ChatSettings): String = when (channel) {
        ChatChannel.LOCAL -> settings.localFormat
        ChatChannel.GLOBAL -> settings.globalFormat
    }

    private fun reject(event: AsyncChatEvent, message: String, settings: ChatSettings) {
        event.isCancelled = true
        if (message.isNotEmpty()) {
            event.player.sendMessage(
                miniMessage.deserialize(message, Placeholder.unparsed("prefix", settings.globalPrefix))
            )
        }
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
