package utils

import net.minecraft.client.Minecraft
import net.minecraft.client.KeyMapping
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import xyz.aerii.template.Template
import xyz.aerii.template.mixin.accessors.KeyMappingAccessor

val LOGGER: Logger = LogManager.getLogger(Template::class.java)

fun rightClick() {
    val options = Minecraft.getInstance().options ?: return
    val key = (options.keyUse as KeyMappingAccessor).boundKey
    KeyMapping.set(key, true)
    KeyMapping.click(key)
    KeyMapping.set(key, false)
}

fun leftClick() {
    val options = Minecraft.getInstance().options ?: return
    val key = (options.keyAttack as KeyMappingAccessor).boundKey

    LOGGER.info("Clicked")

    KeyMapping.set((Minecraft.getInstance().options.keyAttack as KeyMappingAccessor).boundKey, true)

    //KeyMapping.set(key, true)
    //KeyMapping.click(key)
    //KeyMapping.set(key, false)
}