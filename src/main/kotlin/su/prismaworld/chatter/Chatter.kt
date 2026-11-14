package su.prismaworld.chatter

import org.bukkit.plugin.java.JavaPlugin

class Chatter : JavaPlugin() {

    companion object {
        lateinit var instance: Chatter
            private set;
    }

    private fun loadConfig(): Unit {
        this.saveDefaultConfig()
        this.reloadConfig()
    }

    override fun onEnable() {
        instance = this;

        this.loadConfig()
    }

    override fun onDisable() {}
}
