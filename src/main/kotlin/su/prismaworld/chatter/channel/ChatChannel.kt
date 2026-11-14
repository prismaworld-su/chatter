package su.prismaworld.chatter.channel

/** Канал чата. [id] используется в командах, конфиге и данных игрока. */
enum class ChatChannel(val id: String) {
    LOCAL("local"),
    GLOBAL("global");

    /** Противоположный канал: в него уходит сообщение с префиксом. */
    val opposite: ChatChannel
        get() = when (this) {
            LOCAL -> GLOBAL
            GLOBAL -> LOCAL
        }

    companion object {
        fun fromId(id: String): ChatChannel? = entries.firstOrNull { it.id.equals(id, ignoreCase = true) }
    }
}
