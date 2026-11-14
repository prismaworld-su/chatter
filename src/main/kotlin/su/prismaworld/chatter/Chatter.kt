package su.prismaworld.chatter

import org.bukkit.plugin.java.JavaPlugin
import su.prismaworld.chatter.channel.ChannelStorage
import su.prismaworld.chatter.command.ChatCommand
import su.prismaworld.chatter.command.ChatterCommand
import su.prismaworld.chatter.config.ChatSettings
import su.prismaworld.chatter.listener.ChatListener

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

        val channelStorage = ChannelStorage(this) { settings.defaultChannel }
        // Игроки, уже находящиеся на сервере (например, после перезагрузки плагина), не вызовут PlayerJoinEvent.
        server.onlinePlayers.forEach(channelStorage::load)

        server.pluginManager.registerEvents(channelStorage, this)
        server.pluginManager.registerEvents(ChatListener({ settings }, channelStorage), this)
        registerCommand("chat", "Выбор канала для сообщений без префикса", ChatCommand({ settings }, channelStorage))
        registerCommand("chatter", "Управление плагином Chatter", ChatterCommand(this))
    }
}
