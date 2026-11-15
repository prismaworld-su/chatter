package su.prismaworld.chatter.channel

import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.entity.Player
import su.prismaworld.chatter.config.ChatSettings
import su.prismaworld.chatter.preference.ChatToggle
import su.prismaworld.chatter.preference.PreferenceManager

/**
 * Правила каналов, общие для обычного чата и команд отыгрыша: кто может писать в канал, кто его слышит
 * и что происходит после доставки сообщения. Безопасно вызывать из любого потока.
 */
class ChatAudience(private val preferences: PreferenceManager) {

    private val miniMessage = MiniMessage.miniMessage()

    /** Может ли игрок писать в канал: в глобальный - только если глобальный чат у него не отключён. */
    fun canSend(sender: Player, channel: ChatChannel): Boolean =
        channel == ChatChannel.LOCAL || preferences.isEnabled(sender, ChatToggle.GLOBAL_CHAT)

    /**
     * Условие, по которому игрок слышит сообщение [sender] в [channel]. Отправитель слышит себя всегда.
     * Локальное сообщение слышат игроки в том же мире в пределах [ChatSettings.localRadius] блоков,
     * глобальное - все, у кого глобальный чат не отключён.
     */
    fun canHear(sender: Player, channel: ChatChannel, settings: ChatSettings): (Player) -> Boolean {
        if (channel == ChatChannel.GLOBAL) {
            return { it == sender || preferences.isEnabled(it, ChatToggle.GLOBAL_CHAT) }
        }

        val origin = sender.location
        val world = origin.world
        return { it == sender || (it.world == world && it.location.distanceSquared(origin) <= settings.localRadiusSquared) }
    }

    /**
     * Завершает доставку сообщения: уведомляет отправителя, если его локальное сообщение никто не услышал,
     * и проигрывает звук упоминания получателям, чей ник есть в [text].
     *
     * @param recipients игроки, получившие сообщение, кроме отправителя
     */
    fun onDelivered(sender: Player, recipients: Collection<Player>, channel: ChatChannel, text: String, settings: ChatSettings) {
        if (channel == ChatChannel.LOCAL && recipients.isEmpty() && settings.nobodyHeardMessage.isNotEmpty()) {
            sender.sendMessage(
                miniMessage.deserialize(settings.nobodyHeardMessage, Placeholder.unparsed("prefix", settings.globalPrefix))
            )
        }

        recipients
            .filter { preferences.isEnabled(it, ChatToggle.MENTION_SOUND) && mentions(text, it.name) }
            .forEach { it.playSound(settings.mentionSound, Sound.Emitter.self()) }
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
}
