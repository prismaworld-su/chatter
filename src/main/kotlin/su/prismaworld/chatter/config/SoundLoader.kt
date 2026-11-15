package su.prismaworld.chatter.config

import net.kyori.adventure.key.InvalidKeyException
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import org.bukkit.configuration.ConfigurationSection
import java.util.logging.Logger

/**
 * Загружает звук из секции [path] с ключами `sound`, `volume` и `pitch`.
 * Некорректные значения заменяются значениями по умолчанию с предупреждением в лог.
 */
internal fun loadSound(config: ConfigurationSection, path: String, defaultSound: String, logger: Logger): Sound {
    val soundId = config.getString("$path.sound", defaultSound).orEmpty()
    val key = try {
        Key.key(soundId)
    } catch (_: InvalidKeyException) {
        logger.warning("$path.sound \"$soundId\" не является корректным ключом, используется \"$defaultSound\"")
        Key.key(defaultSound)
    }

    var volume = config.getDouble("$path.volume", 1.0)
    if (!volume.isFinite() || volume < 0.0) {
        logger.warning("$path.volume должен быть неотрицательным числом, используется 1.0")
        volume = 1.0
    }

    var pitch = config.getDouble("$path.pitch", 1.0)
    if (!pitch.isFinite() || pitch !in 0.0..2.0) {
        logger.warning("$path.pitch должен быть в диапазоне от 0 до 2, используется 1.0")
        pitch = 1.0
    }

    return Sound.sound(key, Sound.Source.PLAYER, volume.toFloat(), pitch.toFloat())
}
