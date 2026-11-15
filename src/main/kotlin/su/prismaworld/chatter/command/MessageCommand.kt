package su.prismaworld.chatter.command

import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.entity.Player
import su.prismaworld.chatter.config.ChatSettings
import su.prismaworld.chatter.privatemessage.PrivateMessenger

/** Команда `/w <игрок> <сообщение>` (синонимы `/msg`, `/tell`, `/m`) - личное сообщение игроку в сети. */
class MessageCommand(
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

        val recipient = args.firstOrNull().orEmpty()
        val text = args.drop(1).joinToString(" ").trim()
        if (recipient.isEmpty() || text.isEmpty()) {
            val usage = settings.privateMessages.usageMessage
            if (usage.isNotEmpty()) sender.sendMessage(miniMessage.deserialize(usage))
            return
        }

        messenger.send(sender, recipient, text)
    }

    override fun suggest(source: CommandSourceStack, args: Array<String>): Collection<String> =
        if (args.size <= 1) messenger.suggestRecipients(source.sender, args.firstOrNull().orEmpty())
        else emptyList()
}
