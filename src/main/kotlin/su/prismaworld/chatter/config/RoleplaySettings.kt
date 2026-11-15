package su.prismaworld.chatter.config

import org.bukkit.configuration.ConfigurationSection
import su.prismaworld.chatter.channel.ChatChannel
import su.prismaworld.chatter.roleplay.RoleplayAction

/** Настройки отыгрыша (`/me`, `/try`, `/do` и их глобальных вариантов). */
data class RoleplaySettings(
    val formats: Map<ChatChannel, Map<RoleplayAction, String>>,
    val trySuccess: String,
    val tryFailure: String,
    val usageMessage: String,
) {
    fun format(channel: ChatChannel, action: RoleplayAction): String = formats.getValue(channel).getValue(action)

    companion object {
        // Значения без явного default: если ключа нет в конфиге сервера, берётся значение из config.yml плагина.
        fun load(config: ConfigurationSection) = RoleplaySettings(
            formats = ChatChannel.entries.associateWith { channel ->
                RoleplayAction.entries.associateWith { action ->
                    config.getString("roleplay.formats.${channel.id}.${action.id}").orEmpty()
                }
            },
            trySuccess = config.getString("roleplay.try-success").orEmpty(),
            tryFailure = config.getString("roleplay.try-failure").orEmpty(),
            usageMessage = config.getString("roleplay.messages.usage").orEmpty(),
        )
    }
}
