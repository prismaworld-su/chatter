package su.prismaworld.chatter.placeholder

import me.clip.placeholderapi.PlaceholderAPIPlugin
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.OfflinePlayer
import org.bukkit.plugin.Plugin
import su.prismaworld.chatter.config.ChatSettings
import su.prismaworld.chatter.preference.ChatToggle
import su.prismaworld.chatter.preference.PreferenceManager
import su.prismaworld.chatter.privatemessage.PrivateMessenger
import java.util.Locale

/**
 * Плейсхолдеры Chatter для других плагинов (скорборды, TAB и т.д.):
 *
 * - `%chatter_channel%` - канал для сообщений без префикса: `local` или `global`;
 * - `%chatter_channel_name%` - название этого канала из `messages.channel-names`;
 * - `%chatter_<настройка>%` - включена ли настройка из меню `/chat`, например `%chatter_global_chat%`;
 * - `%chatter_socialspy%` - видит ли игрок чужие личные сообщения: слежка включена и есть право `chatter.spy`.
 *
 * Логические значения выводятся так, как задано в конфиге PlaceholderAPI (`boolean.true`, `boolean.false`).
 * Для игрока не в сети значение пустое. Безопасно вызывать из любого потока.
 */
internal class ChatterExpansion(
    private val plugin: Plugin,
    private val settings: () -> ChatSettings,
    private val preferences: PreferenceManager,
) : PlaceholderExpansion() {

    /** Настройки по названию плейсхолдера: `global-chat` - `global_chat`. */
    private val toggles = ChatToggle.entries.associateBy { it.id.replace('-', '_') }
    private val knownParams = listOf(CHANNEL, CHANNEL_NAME) + toggles.keys + SOCIAL_SPY

    override fun getIdentifier(): String = IDENTIFIER

    override fun getAuthor(): String = plugin.pluginMeta.authors.joinToString()

    override fun getVersion(): String = plugin.pluginMeta.version

    /** Плейсхолдеры зарегистрированы плагином, а не загружены из файла, поэтому /papi reload не должен их удалять. */
    override fun persist(): Boolean = true

    override fun getPlaceholders(): List<String> = knownParams.map { "%${IDENTIFIER}_$it%" }

    override fun onRequest(player: OfflinePlayer?, params: String): String? {
        val param = params.lowercase(Locale.ROOT)
        if (param !in knownParams) return null
        val online = player?.player ?: return ""

        return when (param) {
            CHANNEL -> preferences.channel(online).id
            CHANNEL_NAME -> settings().channelNames.getValue(preferences.channel(online))
            SOCIAL_SPY -> format(online.hasPermission(PrivateMessenger.SPY_PERMISSION) && preferences.isSocialSpy(online))
            else -> format(preferences.isEnabled(online, toggles.getValue(param)))
        }
    }

    private fun format(value: Boolean): String =
        if (value) PlaceholderAPIPlugin.booleanTrue() else PlaceholderAPIPlugin.booleanFalse()

    private companion object {
        const val IDENTIFIER = "chatter"
        const val CHANNEL = "channel"
        const val CHANNEL_NAME = "channel_name"
        const val SOCIAL_SPY = "socialspy"
    }
}
