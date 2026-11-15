package su.prismaworld.chatter.command

import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.entity.Player
import su.prismaworld.chatter.config.ChatSettings
import su.prismaworld.chatter.privatemessage.PrivateMessenger

/**
 * Команда `/r <сообщение>` - ответ последнему собеседнику: тому, кто последним написал игроку,
 * или тому, кому последним написал он сам.
 */
class ReplyCommand(
    private val settings: () -> ChatSettings,
    private val messenger: PrivateMessenger,
) : BasicCommand {

    private val miniMessage = MiniMessage.miniMessage()

    override fun execute(source: CommandSourceStack, args: Array<String>) {
        val settings = settings()
        val sender = source.sender

        if (sender !is Player) {
            sender.sendMessage(miniMessage.deserialize(settings.playerOnlyMessage))
            return
        }

        val text = args.joinToString(" ").trim()
        if (text.isEmpty()) {
            val usage = settings.privateMessages.replyUsageMessage
            if (usage.isNotEmpty()) sender.sendMessage(miniMessage.deserialize(usage))
            return
        }

        messenger.reply(sender, text)
    }
}
