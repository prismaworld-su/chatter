package su.prismaworld.chatter.placeholder

import kotlin.test.Test
import kotlin.test.assertFailsWith

/** PlaceholderAPI - необязательная зависимость: без него классы плагина должны загружаться и проходить проверку JVM. */
class OptionalPlaceholderApiTest {

    @Test
    fun `plugin classes load without PlaceholderAPI`() {
        // PlaceholderAPI подключён как compileOnly и в тестах отсутствует, как на сервере без него.
        assertFailsWith<ClassNotFoundException> { Class.forName("me.clip.placeholderapi.PlaceholderAPI") }

        listOf(
            "su.prismaworld.chatter.Chatter",
            "su.prismaworld.chatter.listener.ChatListener",
            "su.prismaworld.chatter.privatemessage.PrivateMessenger",
            "su.prismaworld.chatter.roleplay.RoleplayBroadcaster",
            "su.prismaworld.chatter.roleplay.RoleplayRenderer",
        ).forEach { Class.forName(it, true, javaClass.classLoader) }
    }
}
