package su.prismaworld.chatter

import org.bukkit.plugin.java.JavaPlugin
import su.prismaworld.chatter.command.ChatCommand
import su.prismaworld.chatter.command.ChatterCommand
import su.prismaworld.chatter.command.MessageCommand
import su.prismaworld.chatter.command.ReplyCommand
import su.prismaworld.chatter.command.SocialSpyCommand
import su.prismaworld.chatter.config.ChatSettings
import su.prismaworld.chatter.listener.ChatListener
import su.prismaworld.chatter.listener.ServerMessageListener
import su.prismaworld.chatter.menu.ChatMenu
import su.prismaworld.chatter.menu.ChatMenuListener
import su.prismaworld.chatter.module.ChatModules
import su.prismaworld.chatter.preference.PreferenceManager
import su.prismaworld.chatter.privatemessage.PrivateMessenger

class Chatter : JavaPlugin() {

    companion object {
        lateinit var instance: Chatter
            private set
    }

    /** Текущие настройки. Читаются из асинхронного потока чата, поэтому поле volatile. */
    @Volatile
    lateinit var settings: ChatSettings
        private set

    fun loadSettings() {
        saveDefaultConfig()
        reloadConfig()
        settings = ChatSettings.load(config, logger)
    }

    override fun onEnable() {
        instance = this

        loadSettings()

        val preferences = PreferenceManager(this) { settings }
        // Игроки, уже находящиеся на сервере (например, после перезагрузки плагина), не вызовут PlayerJoinEvent.
        server.onlinePlayers.forEach(preferences::load)

        val modules = ChatModules()
        val messenger = PrivateMessenger(server, { settings }, preferences, modules)

        server.pluginManager.registerEvents(preferences, this)
        server.pluginManager.registerEvents(ChatListener({ settings }, preferences, modules), this)
        server.pluginManager.registerEvents(ServerMessageListener(server, preferences), this)
        server.pluginManager.registerEvents(ChatMenuListener(), this)
        server.pluginManager.registerEvents(messenger, this)
        registerCommand("chat", "Настройки чата", ChatCommand({ settings }, preferences))
        registerCommand("chatter", "Управление плагином Chatter", ChatterCommand(this))
        // Одноимённые команды ванильного сервера (/w, /msg, /tell) заменяются, они остаются доступны с префиксом minecraft:.
        registerCommand(
            PrivateMessenger.COMMAND,
            "Личное сообщение игроку",
            listOf("msg", "tell", "m"),
            MessageCommand({ settings }, messenger),
        )
        registerCommand("r", "Ответ последнему собеседнику в личных сообщениях", ReplyCommand({ settings }, messenger))
        registerCommand("socialspy", "Слежка за личными сообщениями", SocialSpyCommand({ settings }, preferences))
    }

    override fun onDisable() {
        // После отключения плагина клики по меню никто не обработает, поэтому открытые меню закрываются.
        server.onlinePlayers
            .filter { it.openInventory.topInventory.getHolder(false) is ChatMenu }
            .forEach { it.closeInventory() }
    }
}
