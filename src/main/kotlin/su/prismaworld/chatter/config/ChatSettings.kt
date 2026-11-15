package su.prismaworld.chatter.config

import net.kyori.adventure.sound.Sound
import org.bukkit.configuration.ConfigurationSection
import su.prismaworld.chatter.channel.ChatChannel
import su.prismaworld.chatter.preference.ChatToggle
import java.util.logging.Logger

/**
 * Неизменяемый снимок настроек плагина.
 *
 * Объект пересоздаётся целиком при каждой загрузке конфигурации, поэтому его
 * можно безопасно читать из асинхронного потока чата.
 */
data class ChatSettings(
    val defaultChannel: ChatChannel,
    val defaultToggles: Map<ChatToggle, Boolean>,
    val localRadius: Double,
    val localFormat: String,
    val globalPrefix: String,
    val globalFormat: String,
    val mentionSound: Sound,
    val nobodyHeardMessage: String,
    val emptyGlobalMessage: String,
    val emptyMessage: String,
    val globalDisabledMessage: String,
    val reloadedMessage: String,
    val usageMessage: String,
    val channelSwitchedMessage: String,
    val channelUsageMessage: String,
    val playerOnlyMessage: String,
    val channelNames: Map<ChatChannel, String>,
    val menu: MenuSettings,
    val modules: ModuleSettings,
) {
    /** Квадрат радиуса, чтобы сравнивать расстояния без извлечения корня. */
    val localRadiusSquared: Double = localRadius * localRadius

    companion object {
        const val DEFAULT_LOCAL_RADIUS = 128.0
        const val DEFAULT_GLOBAL_PREFIX = "!"
        const val DEFAULT_MENTION_SOUND = "minecraft:entity.experience_orb.pickup"
        val DEFAULT_CHANNEL = ChatChannel.LOCAL

        fun load(config: ConfigurationSection, logger: Logger): ChatSettings {
            val defaultChannelId = config.getString("default-channel", DEFAULT_CHANNEL.id).orEmpty()
            val defaultChannel = ChatChannel.fromId(defaultChannelId) ?: DEFAULT_CHANNEL.also {
                logger.warning("default-channel \"$defaultChannelId\" не существует, используется \"${it.id}\"")
            }

            var radius = config.getDouble("local.radius", DEFAULT_LOCAL_RADIUS)
            if (!radius.isFinite() || radius <= 0.0) {
                logger.warning("local.radius должен быть положительным числом, используется $DEFAULT_LOCAL_RADIUS")
                radius = DEFAULT_LOCAL_RADIUS
            }

            var prefix = config.getString("global.prefix", DEFAULT_GLOBAL_PREFIX).orEmpty()
            if (prefix.isBlank()) {
                logger.warning("global.prefix не может быть пустым, используется \"$DEFAULT_GLOBAL_PREFIX\"")
                prefix = DEFAULT_GLOBAL_PREFIX
            }

            return ChatSettings(
                defaultChannel = defaultChannel,
                defaultToggles = ChatToggle.entries.associateWith { config.getBoolean("default-preferences.${it.id}", true) },
                localRadius = radius,
                localFormat = config.getString("local.format", "<player>: <message>").orEmpty(),
                globalPrefix = prefix,
                globalFormat = config.getString("global.format", "<player>: <message>").orEmpty(),
                mentionSound = loadSound(config, "mention-sound", DEFAULT_MENTION_SOUND, logger),
                nobodyHeardMessage = config.getString("messages.nobody-heard").orEmpty(),
                emptyGlobalMessage = config.getString("messages.empty-global").orEmpty(),
                emptyMessage = config.getString("messages.empty-message").orEmpty(),
                globalDisabledMessage = config.getString("messages.global-disabled").orEmpty(),
                reloadedMessage = config.getString("messages.reloaded").orEmpty(),
                usageMessage = config.getString("messages.usage").orEmpty(),
                channelSwitchedMessage = config.getString("messages.channel-switched").orEmpty(),
                channelUsageMessage = config.getString("messages.channel-usage").orEmpty(),
                playerOnlyMessage = config.getString("messages.player-only").orEmpty(),
                channelNames = ChatChannel.entries.associateWith { channel ->
                    config.getString("messages.channel-names.${channel.id}") ?: channel.id
                },
                menu = MenuSettings.load(config),
                modules = ModuleSettings.load(config, logger),
            )
        }
    }
}
