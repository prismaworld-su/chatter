package su.prismaworld.chatter

import org.bukkit.plugin.java.JavaPlugin
import su.prismaworld.chatter.command.ChatCommand
import su.prismaworld.chatter.command.ChatterCommand
import su.prismaworld.chatter.config.ChatSettings
import su.prismaworld.chatter.listener.ChatListener
import su.prismaworld.chatter.listener.ServerMessageListener
import su.prismaworld.chatter.menu.ChatMenu
import su.prismaworld.chatter.menu.ChatMenuListener
import su.prismaworld.chatter.module.ChatModules
import su.prismaworld.chatter.preference.PreferenceManager

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

        server.pluginManager.registerEvents(preferences, this)
        server.pluginManager.registerEvents(ChatListener({ settings }, preferences, ChatModules()), this)
        server.pluginManager.registerEvents(ServerMessageListener(server, preferences), this)
        server.pluginManager.registerEvents(ChatMenuListener(), this)
        registerCommand("chat", "Настройки чата", ChatCommand({ settings }, preferences))
        registerCommand("chatter", "Управление плагином Chatter", ChatterCommand(this))
    }

    override fun onDisable() {
        // После отключения плагина клики по меню никто не обработает, поэтому открытые меню закрываются.
        server.onlinePlayers
            .filter { it.openInventory.topInventory.getHolder(false) is ChatMenu }
            .forEach { it.closeInventory() }
    }
}
