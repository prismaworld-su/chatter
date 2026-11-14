package su.prismaworld.chatter.menu

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent

/** Запрещает перемещать предметы в меню [ChatMenu] и передаёт ему клики по кнопкам. */
class ChatMenuListener : Listener {

    @EventHandler(ignoreCancelled = true)
    fun onClick(event: InventoryClickEvent) {
        val top = event.view.topInventory
        val menu = top.getHolder(false) as? ChatMenu ?: return

        // Отменяются все клики, включая shift-клик из инвентаря игрока, чтобы предметы не попадали в меню.
        event.isCancelled = true
        if (event.clickedInventory == top) {
            menu.click(event.slot)
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onDrag(event: InventoryDragEvent) {
        if (event.view.topInventory.getHolder(false) is ChatMenu) {
            event.isCancelled = true
        }
    }
}
