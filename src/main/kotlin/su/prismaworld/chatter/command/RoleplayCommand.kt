package su.prismaworld.chatter.command

import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.entity.Player
import su.prismaworld.chatter.channel.ChatChannel
import su.prismaworld.chatter.config.ChatSettings
import su.prismaworld.chatter.roleplay.RoleplayAction
import su.prismaworld.chatter.roleplay.RoleplayBroadcaster

/**
 * Команда отыгрыша `/<action> <текст>` в заданный канал: `/me`, `/try`, `/do` - в локальный чат,
 * `/gme`, `/gtry`, `/gdo` - в глобальный. Название команды задаёт [RoleplayAction.command].
 */
class RoleplayCommand(
    private val action: RoleplayAction,
    private val channel: ChatChannel,
    private val settings: () -> ChatSettings,
    private val broadcaster: RoleplayBroadcaster,
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
            val usage = settings.roleplay.usageMessage
            if (usage.isNotEmpty()) {
                sender.sendMessage(miniMessage.deserialize(usage, Placeholder.unparsed("command", action.command(channel))))
            }
            return
        }

        broadcaster.send(sender, action, channel, text)
    }
}
