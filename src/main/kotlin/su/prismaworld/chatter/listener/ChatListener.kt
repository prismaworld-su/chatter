package su.prismaworld.chatter.listener

import io.papermc.paper.chat.ChatRenderer
import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import su.prismaworld.chatter.channel.ChatChannel
import su.prismaworld.chatter.config.ChatSettings
import su.prismaworld.chatter.module.ChatModules
import su.prismaworld.chatter.preference.ChatToggle
import su.prismaworld.chatter.preference.PreferenceManager

/**
 * Разделяет чат на локальный и глобальный.
 *
 * - Сообщение без префикса уходит в канал, выбранный игроком через `/chat`
 *   (по умолчанию [ChatSettings.defaultChannel]).
 * - Сообщение с префиксом [ChatSettings.globalPrefix] уходит в противоположный канал, префикс при этом вырезается.
 * - Локальное сообщение видят только игроки в том же мире в пределах [ChatSettings.localRadius] блоков,
 *   глобальное - все, у кого глобальный чат не отключён в настройках.
 * - Модули (`:loc:`) заменяются интерактивными компонентами, см. [ChatModules].
 * - Игроки с включённым звуком упоминания слышат звук, когда их ник есть в сообщении.
 */
class ChatListener(
    private val settings: () -> ChatSettings,
    private val preferences: PreferenceManager,
    private val modules: ChatModules,
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
                reject(event, settings.emptyGlobalMessage, settings)
                return
            }
            event.message(Component.text(content))
        }

        event.message(modules.apply(event.player, event.message(), settings.modules))
        // Сообщение могло опустеть после замены модулей.
        if (plainText.serialize(event.message()).isBlank()) {
            reject(event, settings.emptyMessage, settings)
            return
        }

        when (resolveChannel(event.player, text, settings)) {
            ChatChannel.LOCAL -> routeLocal(event, settings)
            ChatChannel.GLOBAL -> routeGlobal(event, settings)
        }
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

        // Канал определяется по исходному сообщению, т.к. из текущего префикс уже вырезан.
        val channel = resolveChannel(sender, plainText.serialize(event.originalMessage()), settings)
        if (channel == ChatChannel.LOCAL && recipients.isEmpty() && settings.nobodyHeardMessage.isNotEmpty()) {
            sender.sendMessage(
                miniMessage.deserialize(
                    settings.nobodyHeardMessage,
                    Placeholder.unparsed("prefix", settings.globalPrefix),
                )
            )
        }

        val text = plainText.serialize(event.message())
        recipients
            .filter { preferences.isEnabled(it, ChatToggle.MENTION_SOUND) && mentions(text, it.name) }
            .forEach { it.playSound(settings.mentionSound, Sound.Emitter.self()) }
    }

    /** Префикс отправляет сообщение в канал, противоположный выбранному игроком. */
    private fun resolveChannel(player: Player, text: String, settings: ChatSettings): ChatChannel {
        val selected = preferences.channel(player)
        return if (text.startsWith(settings.globalPrefix)) selected.opposite else selected
    }

    private fun reject(event: AsyncChatEvent, message: String, settings: ChatSettings) {
        event.isCancelled = true
        if (message.isNotEmpty()) {
            event.player.sendMessage(
                miniMessage.deserialize(message, Placeholder.unparsed("prefix", settings.globalPrefix))
            )
        }
    }

    private fun routeGlobal(event: AsyncChatEvent, settings: ChatSettings) {
        val sender = event.player
        if (!preferences.isEnabled(sender, ChatToggle.GLOBAL_CHAT)) {
            reject(event, settings.globalDisabledMessage, settings)
            return
        }

        event.viewers().removeIf { viewer ->
            viewer is Player && viewer != sender && !preferences.isEnabled(viewer, ChatToggle.GLOBAL_CHAT)
        }
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

    /** Ищет ник отдельным словом без учёта регистра, чтобы "Steve" не находился внутри "Steven". */
    private fun mentions(text: String, name: String): Boolean {
        var index = text.indexOf(name, ignoreCase = true)
        while (index >= 0) {
            val end = index + name.length
            if (!isNameChar(text.getOrNull(index - 1)) && !isNameChar(text.getOrNull(end))) return true
            index = text.indexOf(name, index + 1, ignoreCase = true)
        }
        return false
    }

    private fun isNameChar(char: Char?): Boolean = char != null && (char.isLetterOrDigit() || char == '_')

    private inner class FormatRenderer(private val format: String) : ChatRenderer.ViewerUnaware {
        override fun render(source: Player, sourceDisplayName: Component, message: Component): Component =
            miniMessage.deserialize(
                format,
                Placeholder.component("player", sourceDisplayName),
                Placeholder.component("message", message),
            )
    }
}
