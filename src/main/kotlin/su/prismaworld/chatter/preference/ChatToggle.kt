package su.prismaworld.chatter.preference

/** Настройка чата, которую игрок может включить или выключить. [id] используется в конфиге и данных игрока. */
enum class ChatToggle(val id: String) {
    /** Получение и отправка сообщений глобального чата. */
    GLOBAL_CHAT("global-chat"),

    /** Сообщения о входе и выходе игроков. */
    JOIN_QUIT("join-quit"),

    /** Сообщения о смерти игроков. */
    DEATH_MESSAGES("death-messages"),

    /** Сообщения о получении достижений. */
    ADVANCEMENTS("advancements"),

    /** Звук при упоминании ника игрока в чате. */
    MENTION_SOUND("mention-sound"),

    /** Получение и отправка личных сообщений. */
    PRIVATE_MESSAGES("private-messages"),

    /** Звук при получении личного сообщения. */
    PRIVATE_MESSAGE_SOUND("private-message-sound"),
}
