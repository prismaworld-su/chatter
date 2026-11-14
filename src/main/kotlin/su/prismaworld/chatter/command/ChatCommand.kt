package su.prismaworld.chatter.command

import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.entity.Player
import su.prismaworld.chatter.channel.ChannelStorage
import su.prismaworld.chatter.channel.ChatChannel
import su.prismaworld.chatter.config.ChatSettings

/**
 * Команда `/chat [local|global]` - выбор канала для сообщений без префикса.
 * Без аргументов показывает текущий канал.
 */
class ChatCommand(
    private val settings: () -> ChatSettings,
    private val storage: ChannelStorage,
) : BasicCommand {

    private val miniMessage = MiniMessage.miniMessage()

    override fun execute(source: CommandSourceStack, args: Array<String>) {
        val settings = settings()
        val sender = source.sender

        if (sender !is Player) {
            sender.sendMessage(miniMessage.deserialize(settings.playerOnlyMessage))
            return
        }

        if (args.isEmpty()) {
            sender.sendMessage(render(settings.channelCurrentMessage, settings, storage.get(sender)))
            return
        }

        val channel = args.singleOrNull()?.let(ChatChannel::fromId)
        if (channel == null) {
            sender.sendMessage(miniMessage.deserialize(settings.channelUsageMessage))
            return
        }

        storage.set(sender, channel)
        sender.sendMessage(render(settings.channelSwitchedMessage, settings, channel))
    }

    override fun suggest(source: CommandSourceStack, args: Array<String>): Collection<String> {
        if (args.size > 1) return emptyList()
        val input = args.firstOrNull().orEmpty()
        return ChatChannel.entries.map { it.id }.filter { it.startsWith(input, ignoreCase = true) }
    }

    private fun render(message: String, settings: ChatSettings, channel: ChatChannel) =
        miniMessage.deserialize(
            message,
            Placeholder.unparsed("channel", settings.channelNames.getValue(channel)),
            Placeholder.unparsed("prefix", settings.globalPrefix),
        )
}
