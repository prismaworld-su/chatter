package su.prismaworld.chatter.command

import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.entity.Player
import su.prismaworld.chatter.config.ChatSettings
import su.prismaworld.chatter.preference.PreferenceManager
import su.prismaworld.chatter.privatemessage.PrivateMessenger

/**
 * Команда `/socialspy` - включает и выключает слежку за чужими личными сообщениями.
 * Состояние сохраняется между перезаходами; при потере права слежка перестаёт работать.
 */
class SocialSpyCommand(
    private val settings: () -> ChatSettings,
    private val preferences: PreferenceManager,
) : BasicCommand {

    private val miniMessage = MiniMessage.miniMessage()

    override fun execute(source: CommandSourceStack, args: Array<String>) {
        val settings = settings()
        val sender = source.sender

        if (sender !is Player) {
            sender.sendMessage(miniMessage.deserialize(settings.playerOnlyMessage))
            return
        }

        val pm = settings.privateMessages
        val message = if (preferences.toggleSocialSpy(sender)) pm.spyEnabledMessage else pm.spyDisabledMessage
        if (message.isNotEmpty()) {
            sender.sendMessage(miniMessage.deserialize(message))
        }
    }

    override fun permission(): String = PrivateMessenger.SPY_PERMISSION
}
