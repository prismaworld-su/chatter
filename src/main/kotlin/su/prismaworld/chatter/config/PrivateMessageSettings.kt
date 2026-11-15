package su.prismaworld.chatter.config

import net.kyori.adventure.sound.Sound
import org.bukkit.configuration.ConfigurationSection
import java.util.logging.Logger

/** Настройки личных сообщений (`/w`, `/r`, `/socialspy`). */
data class PrivateMessageSettings(
    val formatSent: String,
    val formatReceived: String,
    val formatSpy: String,
    val replyHover: String,
    val sound: Sound,
    val usageMessage: String,
    val replyUsageMessage: String,
    val playerNotFoundMessage: String,
    val selfMessage: String,
    val noReplyTargetMessage: String,
    val disabledMessage: String,
    val recipientDisabledMessage: String,
    val spyEnabledMessage: String,
    val spyDisabledMessage: String,
) {
    companion object {
        const val DEFAULT_SOUND = "minecraft:block.note_block.pling"

        fun load(config: ConfigurationSection, logger: Logger) = PrivateMessageSettings(
            formatSent = config.getString("private-messages.format-sent", "-> <recipient>: <message>").orEmpty(),
            formatReceived = config.getString("private-messages.format-received", "<sender> ->: <message>").orEmpty(),
            formatSpy = config.getString("private-messages.format-spy", "<sender> -> <recipient>: <message>").orEmpty(),
            replyHover = config.getString("private-messages.reply-hover").orEmpty(),
            sound = loadSound(config, "private-messages.sound", DEFAULT_SOUND, logger),
            usageMessage = message(config, "usage"),
            replyUsageMessage = message(config, "reply-usage"),
            playerNotFoundMessage = message(config, "player-not-found"),
            selfMessage = message(config, "self"),
            noReplyTargetMessage = message(config, "no-reply-target"),
            disabledMessage = message(config, "disabled"),
            recipientDisabledMessage = message(config, "recipient-disabled"),
            spyEnabledMessage = message(config, "spy-enabled"),
            spyDisabledMessage = message(config, "spy-disabled"),
        )

        private fun message(config: ConfigurationSection, id: String) =
            config.getString("private-messages.messages.$id").orEmpty()
    }
}
