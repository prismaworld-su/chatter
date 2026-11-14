package su.prismaworld.chatter.preference

import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin
import su.prismaworld.chatter.channel.ChatChannel
import su.prismaworld.chatter.config.ChatSettings
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Хранит и изменяет личные настройки чата игроков.
 *
 * Явно выбранные значения сохраняются в данных игрока (PersistentDataContainer), поэтому
 * переживают перезаход и рестарт. Для чтения из асинхронного потока чата они дублируются
 * в потокобезопасный кэш, который заполняется при входе и очищается при выходе.
 * Если игрок ничего не выбирал, используются значения по умолчанию из конфига.
 */
class PreferenceManager(plugin: Plugin, private val settings: () -> ChatSettings) : Listener {

    /** Явно выбранные игроком значения. Отсутствующие значения берутся из конфига. */
    private data class Preferences(
        val channel: ChatChannel? = null,
        val toggles: Map<ChatToggle, Boolean> = emptyMap(),
    )

    // Ключ канала совпадает с ранее использовавшимся, чтобы сохранить уже сделанный игроками выбор.
    private val channelKey = NamespacedKey(plugin, "channel")
    private val toggleKeys = ChatToggle.entries.associateWith { NamespacedKey(plugin, it.id) }
    private val cache = ConcurrentHashMap<UUID, Preferences>()

    /** Канал для сообщений без префикса. Безопасно вызывать из любого потока. */
    fun channel(player: Player): ChatChannel =
        cache[player.uniqueId]?.channel ?: settings().defaultChannel

    /** Включена ли настройка у игрока. Безопасно вызывать из любого потока. */
    fun isEnabled(player: Player, toggle: ChatToggle): Boolean =
        cache[player.uniqueId]?.toggles?.get(toggle) ?: settings().defaultToggles.getValue(toggle)

    /**
     * Выбирает канал для сообщений без префикса. Вызывать только из основного потока.
     *
     * @return `false`, если выбран глобальный канал, но глобальный чат у игрока отключён.
     */
    fun selectChannel(player: Player, channel: ChatChannel): Boolean {
        if (channel == ChatChannel.GLOBAL && !isEnabled(player, ChatToggle.GLOBAL_CHAT)) return false

        player.persistentDataContainer.set(channelKey, PersistentDataType.STRING, channel.id)
        update(player) { it.copy(channel = channel) }
        return true
    }

    /**
     * Переключает настройку и возвращает её новое состояние. Вызывать только из основного потока.
     *
     * При отключении глобального чата канал по умолчанию переключается на локальный,
     * чтобы сообщения без префикса не уходили в отключённый канал.
     */
    fun toggle(player: Player, toggle: ChatToggle): Boolean {
        val enabled = !isEnabled(player, toggle)

        player.persistentDataContainer.set(toggleKeys.getValue(toggle), PersistentDataType.BOOLEAN, enabled)
        update(player) { it.copy(toggles = it.toggles + (toggle to enabled)) }

        if (toggle == ChatToggle.GLOBAL_CHAT && !enabled && channel(player) == ChatChannel.GLOBAL) {
            selectChannel(player, ChatChannel.LOCAL)
        }
        return enabled
    }

    /** Загружает настройки игрока в кэш. Вызывать только из основного потока. */
    fun load(player: Player) {
        val container = player.persistentDataContainer
        val channel = container.get(channelKey, PersistentDataType.STRING)?.let(ChatChannel::fromId)
        val toggles = buildMap {
            toggleKeys.forEach { (toggle, key) ->
                container.get(key, PersistentDataType.BOOLEAN)?.let { put(toggle, it) }
            }
        }
        cache[player.uniqueId] = Preferences(channel, toggles)
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onJoin(event: PlayerJoinEvent) = load(event.player)

    @EventHandler(priority = EventPriority.MONITOR)
    fun onQuit(event: PlayerQuitEvent) {
        cache.remove(event.player.uniqueId)
    }

    private fun update(player: Player, transform: (Preferences) -> Preferences) {
        cache.compute(player.uniqueId) { _, current -> transform(current ?: Preferences()) }
    }
}
