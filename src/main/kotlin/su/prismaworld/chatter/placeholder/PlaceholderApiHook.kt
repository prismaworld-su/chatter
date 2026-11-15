package su.prismaworld.chatter.placeholder

import me.clip.placeholderapi.PlaceholderAPI
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import su.prismaworld.chatter.config.ChatSettings
import su.prismaworld.chatter.preference.PreferenceManager
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level

/**
 * Интеграция с PlaceholderAPI: значения плейсхолдеров для форматов и регистрация плейсхолдеров `%chatter_...%`.
 *
 * Классы PlaceholderAPI загружаются только при обращении к ним, поэтому создавать этот объект можно,
 * только если PlaceholderAPI включён.
 */
class PlaceholderApiHook(
    private val plugin: Plugin,
    settings: () -> ChatSettings,
    preferences: PreferenceManager,
) : PlaceholderSource {

    private val expansion = ChatterExpansion(plugin, settings, preferences)

    /** Плейсхолдеры, ошибка которых уже записана в лог, чтобы не повторять её в каждом сообщении. */
    private val failedPlaceholders = ConcurrentHashMap.newKeySet<String>()

    /** Регистрирует плейсхолдеры `%chatter_...%`. */
    fun register(): Boolean = expansion.register()

    /** Удаляет плейсхолдеры `%chatter_...%`, чтобы PlaceholderAPI не обращался к выключенному плагину. */
    fun unregister() {
        if (expansion.isRegistered) expansion.unregister()
    }

    /**
     * Ошибка в расширении PlaceholderAPI не должна мешать отправке сообщения,
     * поэтому плейсхолдер с ошибкой остаётся в тексте как есть.
     */
    override fun value(player: Player, placeholder: String): String? {
        val text = "%$placeholder%"
        val result = try {
            PlaceholderAPI.setPlaceholders(player, text)
        } catch (exception: Exception) {
            return failed(text, exception)
        } catch (error: LinkageError) {
            // Например, расширение собрано под другую версию сервера или плагина.
            return failed(text, error)
        }
        // Нераспознанный плейсхолдер PlaceholderAPI возвращает без изменений.
        return result.takeUnless { it == text }
    }

    private fun failed(placeholder: String, throwable: Throwable): String? {
        if (failedPlaceholders.add(placeholder)) {
            plugin.logger.log(Level.WARNING, "Не удалось получить значение плейсхолдера $placeholder", throwable)
        }
        return null
    }
}
