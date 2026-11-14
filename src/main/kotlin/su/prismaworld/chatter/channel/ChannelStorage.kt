package su.prismaworld.chatter.channel

import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Хранит выбранный игроком канал для сообщений без префикса.
 *
 * Выбор сохраняется в данных игрока (PersistentDataContainer), поэтому переживает
 * перезаход и рестарт. Для чтения из асинхронного потока чата значения дублируются
 * в потокобезопасный кэш, который заполняется при входе и очищается при выходе.
 * Если игрок ничего не выбирал, используется канал по умолчанию из конфига.
 */
class ChannelStorage(plugin: Plugin, private val defaultChannel: () -> ChatChannel) : Listener {

    private val key = NamespacedKey(plugin, "channel")
    private val cache = ConcurrentHashMap<UUID, ChatChannel>()

    /** Канал игрока. Безопасно вызывать из любого потока. */
    fun get(player: Player): ChatChannel = cache[player.uniqueId] ?: defaultChannel()

    /** Сохраняет выбор игрока. Вызывать только из основного потока. */
    fun set(player: Player, channel: ChatChannel) {
        player.persistentDataContainer.set(key, PersistentDataType.STRING, channel.id)
        cache[player.uniqueId] = channel
    }

    /** Загружает выбор игрока в кэш. Вызывать только из основного потока. */
    fun load(player: Player) {
        val channel = player.persistentDataContainer.get(key, PersistentDataType.STRING)?.let(ChatChannel::fromId)
        if (channel != null) {
            cache[player.uniqueId] = channel
        } else {
            cache.remove(player.uniqueId)
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onJoin(event: PlayerJoinEvent) = load(event.player)

    @EventHandler(priority = EventPriority.MONITOR)
    fun onQuit(event: PlayerQuitEvent) {
        cache.remove(event.player.uniqueId)
    }
}
