package handlers

import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextColor

object Typo {
    enum class PrefixType(val parts: List<Pair<String, String>>) {
        DEFAULT(
            listOf(
                "[" to "#BFD7FF",
                "B" to "#AFCBFF",
                "o" to "#9FC0FF",
                "t" to "#8FB4FF",
                "t" to "#7FA9FF",
                "l" to "#6F9DFF",
                "e" to "#5F92FF",
                "r" to "#4F86FF",
                "]" to "#3F7BFF"
            )
        )
    }

    fun modMessage(message: String, prefixType: PrefixType = PrefixType.DEFAULT) {
        val client = Minecraft.getInstance() ?: return
        val chat = client.gui.chat ?: return

        val full: MutableComponent = Component.empty()

        for ((text, hex) in prefixType.parts) {
            val color = TextColor.parseColor(hex).result().orElse(null)

            val part = Component.literal(text).withStyle(
                Style.EMPTY.withColor(color)
            )

            full.append(part)
        }

        full.append(Component.literal(" "))
        full.append(Component.literal(message))

        chat.addMessage(full)
    }
}