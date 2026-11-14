package su.prismaworld.chatter.module

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TranslatableComponent
import net.kyori.adventure.text.TranslationArgument
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.entity.Player
import su.prismaworld.chatter.config.ModuleSettings

/**
 * `:item:` - название предмета в основной руке в цвете его редкости.
 * При наведении показываются характеристики предмета. Если рука пуста, токен удаляется.
 */
class ItemModule : ChatModule {

    private val miniMessage = MiniMessage.miniMessage()

    override val token = ":item:"

    override fun isEnabled(settings: ModuleSettings) = settings.itemEnabled

    override fun render(player: Player, settings: ModuleSettings): Component {
        // Копия, т.к. метод вызывается вне основного потока, а предмет в руке может измениться.
        val item = player.inventory.itemInMainHand.clone()
        if (item.isEmpty) return Component.empty()

        // Корневой цвет имени - цвет редкости с учётом зачарований, как его вычисляет сервер.
        val name = item.effectiveName()
        val rarityColor = name.color() ?: NamedTextColor.WHITE

        return miniMessage.deserialize(settings.itemFormat, Placeholder.component("item", name.withoutStyle()))
            .color(rarityColor)
            .hoverEvent(item.asHoverEvent())
    }

    /**
     * Убирает оформление (цвета, курсив и т.д.) из всего дерева компонента, чтобы текст модуля
     * целиком окрасился в цвет редкости. Переводимые названия остаются переводимыми.
     */
    private fun Component.withoutStyle(): Component {
        val stripped = style(Style.empty()).children(children().map { it.withoutStyle() })
        if (stripped !is TranslatableComponent) return stripped

        return stripped.arguments(
            stripped.arguments().map { argument ->
                val value = argument.value()
                if (value is Component) TranslationArgument.component(value.withoutStyle()) else argument
            }
        )
    }
}
