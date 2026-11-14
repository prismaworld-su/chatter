package su.prismaworld.chatter

import org.bukkit.plugin.java.JavaPlugin
import su.prismaworld.chatter.config.ChatSettings

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
    }
}
