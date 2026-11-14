package su.prismaworld.chatter.menu

import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import su.prismaworld.chatter.channel.ChatChannel
import su.prismaworld.chatter.config.ChatSettings
import su.prismaworld.chatter.config.MenuItemText
import su.prismaworld.chatter.preference.ChatToggle
import su.prismaworld.chatter.preference.PreferenceManager

/**
 * Меню личных настроек чата игрока. Каждый игрок получает собственный экземпляр.
 * Клики обрабатывает [ChatMenuListener].
 */
class ChatMenu(
    private val player: Player,
    private val preferences: PreferenceManager,
    private val settings: () -> ChatSettings,
) : InventoryHolder {

    private val inventory: Inventory = Bukkit.createInventory(this, ROWS * 9, render(settings().menu.title))

    override fun getInventory(): Inventory = inventory

    fun open() {
        redraw()
        player.openInventory(inventory)
    }

    /** Обрабатывает клик по слоту меню. Слоты без кнопок игнорируются. */
    fun click(slot: Int) {
        val toggle = SLOT_TOGGLES[slot]
        when {
            slot == CHANNEL_SLOT -> switchChannel()
            toggle != null -> preferences.toggle(player, toggle)
            else -> return
        }
        player.playSound(CLICK_SOUND, Sound.Emitter.self())
        redraw()
    }

    private fun switchChannel() {
        val settings = settings()
        val channel = preferences.channel(player).opposite
        if (!preferences.selectChannel(player, channel)) {
            player.sendMessage(render(settings.globalDisabledMessage, prefixPlaceholder(settings)))
        }
    }

    private fun redraw() {
        val settings = settings()
        val menu = settings.menu

        val channel = preferences.channel(player)
        inventory.setItem(
            CHANNEL_SLOT,
            createItem(
                material = if (channel == ChatChannel.LOCAL) Material.CAMPFIRE else Material.BEACON,
                text = menu.channelItem,
                glint = false,
                Placeholder.unparsed("channel", settings.channelNames.getValue(channel)),
                prefixPlaceholder(settings),
            ),
        )

        TOGGLE_SLOTS.forEach { (toggle, slot) ->
            val enabled = preferences.isEnabled(player, toggle)
            inventory.setItem(
                slot,
                createItem(
                    material = TOGGLE_ICONS.getValue(toggle),
                    text = menu.toggleItems.getValue(toggle),
                    glint = enabled,
                    Placeholder.parsed("state", if (enabled) menu.stateEnabled else menu.stateDisabled),
                    prefixPlaceholder(settings),
                ),
            )
        }
    }

    private fun createItem(material: Material, text: MenuItemText, glint: Boolean, vararg resolvers: TagResolver) =
        ItemStack(material).apply {
            editMeta { meta ->
                meta.displayName(render(text.name, *resolvers))
                meta.lore(text.lore.map { render(it, *resolvers) })
                meta.setEnchantmentGlintOverride(glint)
            }
        }

    private fun prefixPlaceholder(settings: ChatSettings) = Placeholder.unparsed("prefix", settings.globalPrefix)

    /** Текст предметов по умолчанию курсивный, поэтому курсив отключается, если он не задан явно. */
    private fun render(text: String, vararg resolvers: TagResolver): Component =
        MINI_MESSAGE.deserialize(text, *resolvers).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)

    private companion object {
        val MINI_MESSAGE = MiniMessage.miniMessage()

        const val ROWS = 3
        const val CHANNEL_SLOT = 10

        val TOGGLE_SLOTS = mapOf(
            ChatToggle.GLOBAL_CHAT to 11,
            ChatToggle.JOIN_QUIT to 12,
            ChatToggle.DEATH_MESSAGES to 14,
            ChatToggle.ADVANCEMENTS to 15,
            ChatToggle.MENTION_SOUND to 16,
        )
        val SLOT_TOGGLES = TOGGLE_SLOTS.entries.associate { (toggle, slot) -> slot to toggle }

        val TOGGLE_ICONS = mapOf(
            ChatToggle.GLOBAL_CHAT to Material.FILLED_MAP,
            ChatToggle.JOIN_QUIT to Material.OAK_DOOR,
            ChatToggle.DEATH_MESSAGES to Material.SKELETON_SKULL,
            ChatToggle.ADVANCEMENTS to Material.KNOWLEDGE_BOOK,
            ChatToggle.MENTION_SOUND to Material.BELL,
        )

        val CLICK_SOUND: Sound = Sound.sound(
            org.bukkit.Sound.UI_BUTTON_CLICK,
            Sound.Source.MASTER,
            0.5f,
            1.0f,
        )
    }
}
