package su.prismaworld.chatter.listener

import net.kyori.adventure.text.Component
import org.bukkit.Server
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerAdvancementDoneEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import su.prismaworld.chatter.preference.ChatToggle
import su.prismaworld.chatter.preference.PreferenceManager

/**
 * Рассылает системные сообщения (вход и выход, смерти, достижения) только тем игрокам,
 * у которых они включены в настройках чата.
 *
 * Стандартная рассылка отключается, а итоговое сообщение отправляется вручную в консоль
 * и подходящим игрокам. Обработчики выполняются с приоритетом HIGHEST, чтобы учесть
 * изменения текста другими плагинами. Если сообщение уже отключено (другим плагином или
 * правилом игры), оно не рассылается.
 */
class ServerMessageListener(
    private val server: Server,
    private val preferences: PreferenceManager,
) : Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onJoin(event: PlayerJoinEvent) {
        val message = event.joinMessage() ?: return
        event.joinMessage(null)
        broadcast(message, ChatToggle.JOIN_QUIT)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onQuit(event: PlayerQuitEvent) {
        val message = event.quitMessage() ?: return
        event.quitMessage(null)
        broadcast(message, ChatToggle.JOIN_QUIT, except = event.player)
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onDeath(event: PlayerDeathEvent) {
        if (!event.showDeathMessages) return
        val message = event.deathMessage() ?: return

        // Без сообщения о смерти сервер показал бы погибшему пустой экран смерти, поэтому текст сохраняется для него отдельно.
        if (event.deathScreenMessageOverride() == null) {
            event.deathScreenMessageOverride(message)
        }
        event.deathMessage(null)
        broadcast(message, ChatToggle.DEATH_MESSAGES)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onAdvancement(event: PlayerAdvancementDoneEvent) {
        val message = event.message() ?: return
        event.message(null)
        broadcast(message, ChatToggle.ADVANCEMENTS)
    }

    private fun broadcast(message: Component, toggle: ChatToggle, except: Player? = null) {
        server.consoleSender.sendMessage(message)
        server.onlinePlayers
            .filter { it != except && preferences.isEnabled(it, toggle) }
            .forEach { it.sendMessage(message) }
    }
}
