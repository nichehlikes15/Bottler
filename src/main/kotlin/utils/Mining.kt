package utils

import net.minecraft.client.Minecraft
import net.minecraft.client.KeyMapping
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import xyz.aerii.template.Template
import xyz.aerii.template.mixin.accessors.KeyMappingAccessor
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents

val LOGGER: Logger = LogManager.getLogger(Template::class.java)
private var miningRegistered = false
private var miningActive = false

fun rightClick() {
    val options = Minecraft.getInstance().options ?: return
    val key = (options.keyUse as KeyMappingAccessor).boundKey
    KeyMapping.set(key, true)
    KeyMapping.click(key)
    KeyMapping.set(key, false)
}

fun holdLeftClick() {
    registerMiningTickIfNeeded()
    miningActive = true
}

fun leftClick() {
    val options = Minecraft.getInstance().options ?: return
    val key = (options.keyAttack as KeyMappingAccessor).boundKey

    KeyMapping.click(key)
}

fun releaseLeftClick() {
    miningActive = false

    val options = Minecraft.getInstance().options ?: return
    val key = (options.keyAttack as KeyMappingAccessor).boundKey
    KeyMapping.set(key, false)
}

private fun registerMiningTickIfNeeded() {
    if (miningRegistered) return

    ClientTickEvents.START_CLIENT_TICK.register { client ->
        val options = client.options ?: return@register
        val key = (options.keyAttack as KeyMappingAccessor).boundKey

        if (!miningActive) {
            return@register
        }

        if (client.screen != null) {
            KeyMapping.set(key, false)
            return@register
        }

        KeyMapping.set(key, true)
    }

    miningRegistered = true
}