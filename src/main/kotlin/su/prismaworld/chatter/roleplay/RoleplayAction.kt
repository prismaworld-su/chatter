package su.prismaworld.chatter.roleplay

import su.prismaworld.chatter.channel.ChatChannel

/** Вид отыгрыша. [id] используется в командах и конфиге, [description] - в описании команд. */
enum class RoleplayAction(val id: String, val description: String) {
    /** Действие персонажа: `* Ник открыл дверь`. */
    ME("me", "Отыгрыш действия"),

    /** Попытка действия со случайным исходом: `* Ник открыл дверь [Удачно]`. */
    TRY("try", "Отыгрыш попытки со случайным исходом"),

    /** Описание окружения или результата действия: `* Дверь открыта (Ник)`. */
    DO("do", "Описание окружения"),
    ;

    /** Команда отыгрыша в канал: `/me` - в локальный, `/gme` - в глобальный. */
    fun command(channel: ChatChannel): String = when (channel) {
        ChatChannel.LOCAL -> id
        ChatChannel.GLOBAL -> "g$id"
    }
}
