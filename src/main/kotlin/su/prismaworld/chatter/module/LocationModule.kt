package su.prismaworld.chatter.module

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.entity.Player
import su.prismaworld.chatter.config.ModuleSettings

/**
 * `:loc:` - координаты блока, на котором стоит игрок, в цвете его измерения.
 * По нажатию координаты копируются в буфер обмена.
 */
class LocationModule : ChatModule {

    private val miniMessage = MiniMessage.miniMessage()

    override val token = ":loc:"

    override fun isEnabled(settings: ModuleSettings) = settings.locationEnabled

    override fun render(player: Player, settings: ModuleSettings): Component {
        val location = player.location
        val coordinates = mapOf("x" to location.blockX, "y" to location.blockY, "z" to location.blockZ)
        val placeholders = TagResolver.resolver(coordinates.map { (name, value) -> Placeholder.unparsed(name, value.toString()) })

        val clipboard = coordinates.entries.fold(settings.locationClipboard) { text, (name, value) ->
            text.replace("<$name>", value.toString())
        }

        var component = miniMessage.deserialize(settings.locationFormat, placeholders)
            .color(settings.dimensionColors.getValue(player.world.environment))
            .clickEvent(ClickEvent.copyToClipboard(clipboard))
        if (settings.locationHover.isNotEmpty()) {
            component = component.hoverEvent(HoverEvent.showText(miniMessage.deserialize(settings.locationHover, placeholders)))
        }
        return component
    }
}
