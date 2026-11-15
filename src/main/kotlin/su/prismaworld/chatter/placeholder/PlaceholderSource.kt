package su.prismaworld.chatter.placeholder

import org.bukkit.entity.Player

/** Источник значений плейсхолдеров вида `%identifier_params%`, например PlaceholderAPI. */
fun interface PlaceholderSource {
    /**
     * Значение плейсхолдера [placeholder] для [player] или `null`, если плейсхолдер не распознан.
     *
     * @param placeholder плейсхолдер без знаков `%`, например `luckperms_prefix`
     */
    fun value(player: Player, placeholder: String): String?

    companion object {
        /** Источник, не распознающий ни одного плейсхолдера. Используется, когда PlaceholderAPI не установлен. */
        val NONE = PlaceholderSource { _, _ -> null }
    }
}
