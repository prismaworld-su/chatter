package su.prismaworld.chatter.command

import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import net.kyori.adventure.text.minimessage.MiniMessage
import su.prismaworld.chatter.Chatter

/** Команда `/chatter reload` - перезагружает конфигурацию без перезапуска сервера. */
class ChatterCommand(private val plugin: Chatter) : BasicCommand {

    private val miniMessage = MiniMessage.miniMessage()

    override fun execute(source: CommandSourceStack, args: Array<String>) {
        val sender = source.sender

        if (args.size != 1 || !args[0].equals(RELOAD, ignoreCase = true)) {
            sender.sendMessage(miniMessage.deserialize(plugin.settings.usageMessage))
            return
        }

        plugin.loadSettings()
        sender.sendMessage(miniMessage.deserialize(plugin.settings.reloadedMessage))
    }

    override fun suggest(source: CommandSourceStack, args: Array<String>): Collection<String> =
        if (args.size <= 1 && RELOAD.startsWith(args.firstOrNull().orEmpty(), ignoreCase = true)) listOf(RELOAD)
        else emptyList()

    override fun permission(): String = PERMISSION

    private companion object {
        const val RELOAD = "reload"
        const val PERMISSION = "chatter.reload"
    }
}
