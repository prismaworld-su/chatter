package su.prismaworld.chatter.config

import org.bukkit.configuration.ConfigurationSection
import su.prismaworld.chatter.channel.ChatChannel
import java.util.logging.Logger

/**
 * Неизменяемый снимок настроек плагина.
 *
 * Объект пересоздаётся целиком при каждой загрузке конфигурации, поэтому его
 * можно безопасно читать из асинхронного потока чата.
 */
data class ChatSettings(
    val defaultChannel: ChatChannel,
    val localRadius: Double,
    val localFormat: String,
    val globalPrefix: String,
    val globalFormat: String,
    val nobodyHeardMessage: String,
    val emptyGlobalMessage: String,
    val reloadedMessage: String,
    val usageMessage: String,
    val channelCurrentMessage: String,
    val channelSwitchedMessage: String,
    val channelUsageMessage: String,
    val playerOnlyMessage: String,
    val channelNames: Map<ChatChannel, String>,
) {
    /** Квадрат радиуса, чтобы сравнивать расстояния без извлечения корня. */
    val localRadiusSquared: Double = localRadius * localRadius

    companion object {
        const val DEFAULT_LOCAL_RADIUS = 128.0
        const val DEFAULT_GLOBAL_PREFIX = "!"
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
                localRadius = radius,
                localFormat = config.getString("local.format", "<player>: <message>").orEmpty(),
                globalPrefix = prefix,
                globalFormat = config.getString("global.format", "<player>: <message>").orEmpty(),
                nobodyHeardMessage = config.getString("messages.nobody-heard").orEmpty(),
                emptyGlobalMessage = config.getString("messages.empty-global").orEmpty(),
                reloadedMessage = config.getString("messages.reloaded").orEmpty(),
                usageMessage = config.getString("messages.usage").orEmpty(),
                channelCurrentMessage = config.getString("messages.channel-current").orEmpty(),
                channelSwitchedMessage = config.getString("messages.channel-switched").orEmpty(),
                channelUsageMessage = config.getString("messages.channel-usage").orEmpty(),
                playerOnlyMessage = config.getString("messages.player-only").orEmpty(),
                channelNames = ChatChannel.entries.associateWith { channel ->
                    config.getString("messages.channel-names.${channel.id}") ?: channel.id
                },
            )
        }
    }
}
