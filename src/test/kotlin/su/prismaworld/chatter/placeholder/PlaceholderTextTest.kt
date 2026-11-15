package su.prismaworld.chatter.placeholder

import kotlin.test.Test
import kotlin.test.assertEquals

class PlaceholderTextTest {

    private val values = mapOf(
        "player_name" to "Steve",
        "luckperms_prefix" to "[Admin] ",
        "math_1 + 2" to "3",
        "server_time_HH:mm" to "12:30",
    )

    /** Заменяет плейсхолдеры и запоминает, какие из них запрашивались. */
    private fun replace(text: String): Pair<String, List<String>> {
        val requested = mutableListOf<String>()
        val result = PlaceholderText.replace(text) {
            requested += it
            values[it]
        }
        return result to requested
    }

    @Test
    fun `replaces known placeholders`() {
        assertEquals("[Admin] Steve: hi" to listOf("luckperms_prefix", "player_name"), replace("%luckperms_prefix%%player_name%: hi"))
    }

    @Test
    fun `keeps unknown placeholders`() {
        assertEquals("%unknown_value% Steve" to listOf("unknown_value", "player_name"), replace("%unknown_value% %player_name%"))
    }

    @Test
    fun `returns text without placeholders unchanged`() {
        assertEquals("plain text" to emptyList(), replace("plain text"))
        assertEquals("100%" to emptyList(), replace("100%"))
        assertEquals("%player_name" to emptyList(), replace("%player_name"))
    }

    @Test
    fun `treats percent before a space as text`() {
        assertEquals("50% и 30%" to emptyList(), replace("50% и 30%"))
        assertEquals("Скидка 50% для Steve" to listOf("player_name"), replace("Скидка 50% для %player_name%"))
    }

    @Test
    fun `ignores text between percent signs without underscore`() {
        assertEquals("%foo% Steve" to listOf("player_name"), replace("%foo% %player_name%"))
    }

    @Test
    fun `allows spaces and colons after underscore`() {
        assertEquals("1 + 2 = 3" to listOf("math_1 + 2"), replace("1 + 2 = %math_1 + 2%"))
        assertEquals("12:30" to listOf("server_time_HH:mm"), replace("%server_time_HH:mm%"))
    }

    @Test
    fun `replaces placeholders inside MiniMessage tags`() {
        assertEquals(
            "<click:suggest_command:'/w Steve '>Steve</click>" to listOf("player_name", "player_name"),
            replace("<click:suggest_command:'/w %player_name% '>%player_name%</click>"),
        )
    }
}
