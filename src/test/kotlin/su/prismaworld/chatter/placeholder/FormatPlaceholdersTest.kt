package su.prismaworld.chatter.placeholder

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.entity.Player
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertEquals

class FormatPlaceholdersTest {

    private val miniMessage = MiniMessage.miniMessage()
    private val sender = player()
    private val recipient = player()

    private val values = mapOf(
        sender to mapOf(
            "luckperms_prefix" to "&c[Admin] ",
            "luckperms_suffix" to "<gradient:red:blue>VIP</gradient>",
            "vault_eco_balance" to "100",
            "player_name" to "Steve",
        ),
        recipient to mapOf(
            "luckperms_prefix" to "&a[Player] ",
            "player_name" to "Alex",
            "server_time_HH:mm" to "12:30",
        ),
    )

    private val placeholders = FormatPlaceholders { player, placeholder -> values.getValue(player)[placeholder] }

    /** Оформляет [format] так же, как форматы личных сообщений: `%...%` - отправитель, `<recipient_papi:...>` - получатель. */
    private fun render(format: String, vararg resolvers: TagResolver): Component =
        miniMessage.deserialize(
            placeholders.apply(format, sender),
            Placeholder.unparsed("player", "Steve"),
            placeholders.tag("recipient_papi", recipient),
            *resolvers,
        )

    @Test
    fun `legacy colors of a value do not leak into the format`() {
        assertEquals(
            listOf(Segment("[Admin] ", NamedTextColor.RED), Segment("Steve")),
            segments(render("%luckperms_prefix%<player>")),
        )
    }

    @Test
    fun `MiniMessage tags in a value are parsed`() {
        val segments = segments(render("%luckperms_suffix% <player>"))
        assertEquals("VIP Steve", segments.joinToString("") { it.text })
        // Градиент окрашивает каждый символ VIP, остальной текст без цвета.
        assertEquals(3, segments.count { it.color != null })
        assertEquals(Segment(" Steve"), segments.last())
    }

    @Test
    fun `format styles apply to a value`() {
        assertEquals(
            listOf(Segment("100", NamedTextColor.GOLD, bold = true)),
            segments(render("<gold><bold>%vault_eco_balance%</bold></gold>")),
        )
    }

    @Test
    fun `unknown placeholders stay as is`() {
        assertEquals("%unknown_value% %unknown_value%", plain(render("%unknown_value% <recipient_papi:unknown_value>")))
    }

    @Test
    fun `placeholders work in hover and click arguments`() {
        val component = render("<hover:show_text:'Баланс: %vault_eco_balance%'><click:suggest_command:'/w %player_name% '><player></click></hover>")
        val target = component.children().ifEmpty { listOf(component) }.first()

        assertEquals("Баланс: 100", plain(target.hoverEvent()!!.value() as Component))
        assertEquals(ClickEvent.suggestCommand("/w Steve "), target.clickEvent())
    }

    @Test
    fun `player text is not parsed`() {
        val component = render("<message>", Placeholder.component("message", Component.text("%luckperms_prefix% <red>hi")))
        assertEquals("%luckperms_prefix% <red>hi", plain(component))
    }

    @Test
    fun `recipient tag uses recipient values`() {
        assertEquals(
            listOf(Segment("[Admin] ", NamedTextColor.RED), Segment("→ "), Segment("[Player] ", NamedTextColor.GREEN), Segment("Alex")),
            segments(render("%luckperms_prefix%→ <recipient_papi:luckperms_prefix><recipient_papi:player_name>")),
        )
    }

    @Test
    fun `recipient tag accepts colons and quotes`() {
        assertEquals("12:30 12:30", plain(render("<recipient_papi:server_time_HH:mm> <recipient_papi:'server_time_HH:mm'>")))
    }

    @Test
    fun `recipient tag works in hover`() {
        val component = render("<hover:show_text:'<recipient_papi:player_name>'>name</hover>")
        val hover = component.hoverEvent() ?: component.children().first().hoverEvent()

        assertEquals(HoverEvent.Action.SHOW_TEXT, hover!!.action())
        assertEquals("Alex", plain(hover.value() as Component))
    }

    @Test
    fun `recipient tag without placeholder stays as text`() {
        assertEquals("<recipient_papi>", plain(render("<recipient_papi>")))
    }

    @Test
    fun `nothing is replaced without a source`() {
        val format = "%luckperms_prefix%<player>"
        assertEquals(format, FormatPlaceholders(PlaceholderSource.NONE).apply(format, sender))
    }

    /** Отрезок текста с итоговым цветом и жирностью. */
    private data class Segment(val text: String, val color: TextColor? = null, val bold: Boolean = false)

    /** Текст компонента, разбитый на отрезки с одинаковым оформлением, независимо от структуры дерева компонентов. */
    private fun segments(component: Component): List<Segment> {
        val result = mutableListOf<Segment>()
        fun walk(component: Component, parent: Style) {
            val style = parent.merge(component.style())
            if (component is TextComponent && component.content().isNotEmpty()) {
                val segment = Segment(component.content(), style.color(), style.decoration(TextDecoration.BOLD) == TextDecoration.State.TRUE)
                val last = result.lastOrNull()
                if (last != null && last.color == segment.color && last.bold == segment.bold) {
                    result[result.lastIndex] = last.copy(text = last.text + segment.text)
                } else {
                    result += segment
                }
            }
            component.children().forEach { walk(it, style) }
        }
        walk(component, Style.empty())
        return result
    }

    private fun plain(component: Component) = PlainTextComponentSerializer.plainText().serialize(component)

    /** Игрок-заглушка: тестам нужен только уникальный объект Player. */
    private fun player(): Player = Proxy.newProxyInstance(Player::class.java.classLoader, arrayOf(Player::class.java)) { proxy, method, args ->
        when (method.name) {
            "equals" -> proxy === args?.firstOrNull()
            "hashCode" -> System.identityHashCode(proxy)
            "toString" -> "Player@${System.identityHashCode(proxy)}"
            else -> throw UnsupportedOperationException(method.name)
        }
    } as Player
}
