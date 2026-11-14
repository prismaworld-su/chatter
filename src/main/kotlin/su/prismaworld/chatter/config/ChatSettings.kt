package su.prismaworld.chatter.config

import org.bukkit.configuration.ConfigurationSection
import java.util.logging.Logger

/**
 * Неизменяемый снимок настроек плагина.
 *
 * Объект пересоздаётся целиком при каждой загрузке конфигурации, поэтому его
 * можно безопасно читать из асинхронного потока чата.
 */
data class ChatSettings(
    val localRadius: Double,
    val localFormat: String,
    val globalPrefix: String,
    val globalFormat: String,
    val nobodyHeardMessage: String,
    val emptyGlobalMessage: String,
    val reloadedMessage: String,
    val usageMessage: String,
) {
    /** Квадрат радиуса, чтобы сравнивать расстояния без извлечения корня. */
    val localRadiusSquared: Double = localRadius * localRadius

    companion object {
        const val DEFAULT_LOCAL_RADIUS = 128.0
        const val DEFAULT_GLOBAL_PREFIX = "!"

        fun load(config: ConfigurationSection, logger: Logger): ChatSettings {
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
                localRadius = radius,
                localFormat = config.getString("local.format", "<player>: <message>").orEmpty(),
                globalPrefix = prefix,
                globalFormat = config.getString("global.format", "<player>: <message>").orEmpty(),
                nobodyHeardMessage = config.getString("messages.nobody-heard").orEmpty(),
                emptyGlobalMessage = config.getString("messages.empty-global").orEmpty(),
                reloadedMessage = config.getString("messages.reloaded").orEmpty(),
                usageMessage = config.getString("messages.usage").orEmpty(),
            )
        }
    }
}
