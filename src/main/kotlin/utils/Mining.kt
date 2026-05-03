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
private var forcingAttack = false

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
    options.keyAttack.setDown(false)
}

private fun registerMiningTickIfNeeded() {
    if (miningRegistered) return

    // END tick works better than START tick: Minecraft polls real input during the tick
    // and can overwrite synthetic key states set too early.
    ClientTickEvents.END_CLIENT_TICK.register { client ->
        val options = client.options ?: return@register

        // Important: don't override the user's real input unless we're actively automining.
        if (!miningActive || client.screen != null) {
            if (forcingAttack) {
                options.keyAttack.setDown(false)
                forcingAttack = false
            }
            return@register
        }

        options.keyAttack.setDown(true)
        forcingAttack = true
    }

    miningRegistered = true
}