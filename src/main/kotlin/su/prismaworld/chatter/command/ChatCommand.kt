package su.prismaworld.chatter.command

import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.entity.Player
import su.prismaworld.chatter.channel.ChatChannel
import su.prismaworld.chatter.config.ChatSettings
import su.prismaworld.chatter.menu.ChatMenu
import su.prismaworld.chatter.preference.PreferenceManager

/**
 * Команда `/chat [local|global]`.
 *
 * Без аргументов открывает меню настроек чата, с аргументом выбирает канал для сообщений без префикса.
 */
class ChatCommand(
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

        if (args.isEmpty()) {
            ChatMenu(sender, preferences, this.settings).open()
            return
        }

        val channel = args.singleOrNull()?.let(ChatChannel::fromId)
        if (channel == null) {
            sender.sendMessage(miniMessage.deserialize(settings.channelUsageMessage))
            return
        }

        if (preferences.selectChannel(sender, channel)) {
            sender.sendMessage(render(settings.channelSwitchedMessage, settings, channel))
        } else {
            sender.sendMessage(render(settings.globalDisabledMessage, settings, channel))
        }
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
