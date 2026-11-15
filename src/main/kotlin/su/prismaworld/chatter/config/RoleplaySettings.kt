package su.prismaworld.chatter.config

import org.bukkit.configuration.ConfigurationSection
import su.prismaworld.chatter.channel.ChatChannel
import su.prismaworld.chatter.roleplay.RoleplayAction
import java.util.logging.Logger

/** Настройки отыгрыша (`/me`, `/try`, `/do` и их глобальных вариантов). */
data class RoleplaySettings(
    /**
     * Префиксы отыгрыша в чате. Отключённые префиксы отсутствуют. Порядок - от длинного префикса к короткому,
     * чтобы длинный префикс не перехватывался более коротким, с которого он начинается.
     */
    val prefixes: Map<RoleplayAction, String>,
    val formats: Map<ChatChannel, Map<RoleplayAction, String>>,
    val trySuccess: String,
    val tryFailure: String,
    val usageMessage: String,
) {
    fun format(channel: ChatChannel, action: RoleplayAction): String = formats.getValue(channel).getValue(action)

    companion object {
        // Значения без явного default: если ключа нет в конфиге сервера, берётся значение из config.yml плагина.
        fun load(config: ConfigurationSection, globalPrefix: String, logger: Logger) = RoleplaySettings(
            prefixes = loadPrefixes(config, globalPrefix, logger),
            formats = ChatChannel.entries.associateWith { channel ->
                RoleplayAction.entries.associateWith { action ->
                    config.getString("roleplay.formats.${channel.id}.${action.id}").orEmpty()
                }
            },
            trySuccess = config.getString("roleplay.try-success").orEmpty(),
            tryFailure = config.getString("roleplay.try-failure").orEmpty(),
            usageMessage = config.getString("roleplay.messages.usage").orEmpty(),
        )

        /**
         * Префикс, пересекающийся с префиксом глобального чата, сделал бы разбор сообщений неоднозначным,
         * а повторяющийся никогда бы не сработал, поэтому такие префиксы отключаются.
         */
        private fun loadPrefixes(config: ConfigurationSection, globalPrefix: String, logger: Logger): Map<RoleplayAction, String> {
            val prefixes = linkedMapOf<RoleplayAction, String>()
            for (action in RoleplayAction.entries) {
                val path = "roleplay.prefixes.${action.id}"
                val prefix = config.getString(path).orEmpty()
                when {
                    prefix.isBlank() -> Unit
                    prefix.startsWith(globalPrefix) || globalPrefix.startsWith(prefix) ->
                        logger.warning("$path \"$prefix\" пересекается с global.prefix \"$globalPrefix\", префикс отключён")
                    prefix in prefixes.values ->
                        logger.warning("$path \"$prefix\" уже используется другим отыгрышем, префикс отключён")
                    else -> prefixes[action] = prefix
                }
            }
            return prefixes.entries.sortedByDescending { it.value.length }.associate { it.toPair() }
        }
    }
}
