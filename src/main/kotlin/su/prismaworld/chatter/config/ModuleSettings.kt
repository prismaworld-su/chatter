package su.prismaworld.chatter.config

import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import org.bukkit.World
import org.bukkit.configuration.ConfigurationSection
import java.util.logging.Logger

/** Настройки модулей сообщений (`:loc:`). */
data class ModuleSettings(
    val locationEnabled: Boolean,
    val locationFormat: String,
    val locationClipboard: String,
    val locationHover: String,
    val dimensionColors: Map<World.Environment, TextColor>,
) {
    companion object {
        private val DEFAULT_DIMENSION_COLORS = mapOf(
            World.Environment.NORMAL to NamedTextColor.GREEN,
            World.Environment.NETHER to NamedTextColor.RED,
            World.Environment.THE_END to NamedTextColor.DARK_PURPLE,
            World.Environment.CUSTOM to NamedTextColor.WHITE,
        )

        private val DIMENSION_IDS = mapOf(
            World.Environment.NORMAL to "overworld",
            World.Environment.NETHER to "nether",
            World.Environment.THE_END to "end",
            World.Environment.CUSTOM to "custom",
        )

        fun load(config: ConfigurationSection, logger: Logger) = ModuleSettings(
            locationEnabled = config.getBoolean("modules.loc.enabled", true),
            locationFormat = config.getString("modules.loc.format", "[<x>, <y>, <z>]").orEmpty(),
            locationClipboard = config.getString("modules.loc.clipboard", "<x> <y> <z>").orEmpty(),
            locationHover = config.getString("modules.loc.hover").orEmpty(),
            dimensionColors = World.Environment.entries.associateWith { environment ->
                val default = DEFAULT_DIMENSION_COLORS[environment] ?: NamedTextColor.WHITE
                loadColor(config, "modules.loc.colors.${DIMENSION_IDS[environment]}", default, logger)
            },
        )

        /** Принимает название цвета (`green`) или HEX (`#55FF55`). */
        private fun loadColor(config: ConfigurationSection, path: String, default: TextColor, logger: Logger): TextColor {
            val value = config.getString(path) ?: return default
            return NamedTextColor.NAMES.value(value.lowercase())
                ?: TextColor.fromHexString(value)
                ?: default.also { logger.warning("$path: \"$value\" не является цветом, используется ${it.asHexString()}") }
        }
    }
}
