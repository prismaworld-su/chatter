package su.prismaworld.chatter.config

import org.bukkit.configuration.ConfigurationSection
import su.prismaworld.chatter.preference.ChatToggle

/** Тексты предмета меню в формате MiniMessage. */
data class MenuItemText(val name: String, val lore: List<String>)

/** Тексты меню настроек чата (`/chat`). */
data class MenuSettings(
    val title: String,
    val stateEnabled: String,
    val stateDisabled: String,
    val channelItem: MenuItemText,
    val toggleItems: Map<ChatToggle, MenuItemText>,
) {
    companion object {
        fun load(config: ConfigurationSection): MenuSettings = MenuSettings(
            title = config.getString("menu.title").orEmpty(),
            stateEnabled = config.getString("menu.state-enabled").orEmpty(),
            stateDisabled = config.getString("menu.state-disabled").orEmpty(),
            channelItem = loadItem(config, "channel"),
            toggleItems = ChatToggle.entries.associateWith { loadItem(config, it.id) },
        )

        private fun loadItem(config: ConfigurationSection, id: String) = MenuItemText(
            name = config.getString("menu.items.$id.name") ?: id,
            lore = config.getStringList("menu.items.$id.lore"),
        )
    }
}
