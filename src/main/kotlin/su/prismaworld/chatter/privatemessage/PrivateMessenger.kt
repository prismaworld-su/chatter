package su.prismaworld.chatter.privatemessage

import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Server
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import su.prismaworld.chatter.config.ChatSettings
import su.prismaworld.chatter.module.ChatModules
import su.prismaworld.chatter.preference.ChatToggle
import su.prismaworld.chatter.preference.PreferenceManager
import java.util.UUID

/**
 * Личные сообщения между игроками в сети.
 *
 * - Игроки, скрытые от отправителя (например, в ванише), считаются не в сети.
 * - Отправить или получить сообщение можно, только если личные сообщения включены в настройках `/chat`.
 * - В сообщениях работают модули (`:loc:`, `:item:`), см. [ChatModules].
 * - Игроки с правом [SPY_PERMISSION] и включённой слежкой видят чужие личные сообщения.
 * - Для `/r` запоминается последний собеседник каждого игрока: им становятся и получатель, и отправитель.
 *   Собеседник игрока забывается при его выходе с сервера.
 *
 * Все методы вызываются из основного потока.
 */
class PrivateMessenger(
    private val server: Server,
    private val settings: () -> ChatSettings,
    private val preferences: PreferenceManager,
    private val modules: ChatModules,
) : Listener {

    /** Ник хранится, чтобы назвать собеседника, даже когда он уже вышел с сервера. */
    private data class Interlocutor(val id: UUID, val name: String)

    private val miniMessage = MiniMessage.miniMessage()
    private val plainText = PlainTextComponentSerializer.plainText()
    private val interlocutors = HashMap<UUID, Interlocutor>()

    /** Отправляет сообщение игроку с ником [recipientName] (без учёта регистра). */
    fun send(sender: Player, recipientName: String, text: String) {
        val settings = settings()
        if (!canSend(sender, settings)) return

        val recipient = findVisible(sender, server.getPlayerExact(recipientName))
        if (recipient == null) {
            sendMessage(sender, settings.privateMessages.playerNotFoundMessage, recipientName)
            return
        }
        deliver(sender, recipient, text, settings)
    }

    /** Отправляет сообщение последнему собеседнику игрока. */
    fun reply(sender: Player, text: String) {
        val settings = settings()
        if (!canSend(sender, settings)) return

        val interlocutor = interlocutors[sender.uniqueId]
        if (interlocutor == null) {
            sendMessage(sender, settings.privateMessages.noReplyTargetMessage)
            return
        }

        val recipient = findVisible(sender, server.getPlayer(interlocutor.id))
        if (recipient == null) {
            sendMessage(sender, settings.privateMessages.playerNotFoundMessage, interlocutor.name)
            return
        }
        deliver(sender, recipient, text, settings)
    }

    /** Ники игроков в сети, которым [sender] может написать, начинающиеся с [input]. */
    fun suggestRecipients(sender: CommandSender, input: String): List<String> =
        server.onlinePlayers
            .filter { sender !is Player || (it != sender && sender.canSee(it)) }
            .map { it.name }
            .filter { it.startsWith(input, ignoreCase = true) }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onQuit(event: PlayerQuitEvent) {
        interlocutors.remove(event.player.uniqueId)
    }

    private fun canSend(sender: Player, settings: ChatSettings): Boolean {
        if (preferences.isEnabled(sender, ChatToggle.PRIVATE_MESSAGES)) return true
        sendMessage(sender, settings.privateMessages.disabledMessage)
        return false
    }

    private fun findVisible(sender: Player, player: Player?): Player? = player?.takeIf { sender.canSee(it) }

    private fun deliver(sender: Player, recipient: Player, text: String, settings: ChatSettings) {
        val pm = settings.privateMessages

        if (recipient == sender) {
            sendMessage(sender, pm.selfMessage)
            return
        }
        if (!preferences.isEnabled(recipient, ChatToggle.PRIVATE_MESSAGES)) {
            sendMessage(sender, pm.recipientDisabledMessage, recipient.name)
            return
        }

        val message = modules.apply(sender, Component.text(text), settings.modules)
        // Сообщение могло опустеть, например если в нём был только :item: при пустой руке.
        if (plainText.serialize(message).isBlank()) {
            sendMessage(sender, settings.emptyMessage)
            return
        }

        val senderName = sender.displayName()
        val recipientName = recipient.displayName()
        // Каждый участник может нажать на ник собеседника, чтобы написать ему.
        sendFormatted(sender, pm.formatSent, senderName, clickableName(recipient, pm.replyHover), message)
        sendFormatted(recipient, pm.formatReceived, clickableName(sender, pm.replyHover), recipientName, message)
        if (preferences.isEnabled(recipient, ChatToggle.PRIVATE_MESSAGE_SOUND)) {
            recipient.playSound(pm.sound, Sound.Emitter.self())
        }

        server.onlinePlayers
            .filter { it != sender && it != recipient && it.hasPermission(SPY_PERMISSION) && preferences.isSocialSpy(it) }
            .forEach { sendFormatted(it, pm.formatSpy, senderName, recipientName, message) }

        interlocutors[sender.uniqueId] = Interlocutor(recipient.uniqueId, recipient.name)
        interlocutors[recipient.uniqueId] = Interlocutor(sender.uniqueId, sender.name)
    }

    private fun sendFormatted(viewer: Player, format: String, sender: Component, recipient: Component, message: Component) {
        if (format.isEmpty()) return
        viewer.sendMessage(
            miniMessage.deserialize(
                format,
                Placeholder.component("sender", sender),
                Placeholder.component("recipient", recipient),
                Placeholder.component("message", message),
            )
        )
    }

    /** Отображаемое имя игрока, по нажатию на которое в чат подставляется команда, чтобы написать ему. */
    private fun clickableName(player: Player, hover: String): Component {
        val name = player.displayName().clickEvent(ClickEvent.suggestCommand("/$COMMAND ${player.name} "))
        return if (hover.isEmpty()) name else name.hoverEvent(HoverEvent.showText(miniMessage.deserialize(hover)))
    }

    /** Отправляет служебное сообщение. Пустая строка в конфиге отключает его. */
    private fun sendMessage(player: Player, message: String, playerName: String = "") {
        if (message.isEmpty()) return
        player.sendMessage(miniMessage.deserialize(message, Placeholder.unparsed("player", playerName)))
    }

    companion object {
        /** Основная команда личных сообщений. */
        const val COMMAND = "w"

        /** Право на слежку за чужими личными сообщениями. */
        const val SPY_PERMISSION = "chatter.spy"
    }
}
