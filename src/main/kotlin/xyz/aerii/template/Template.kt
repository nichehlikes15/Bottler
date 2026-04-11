@file:Suppress("ConstPropertyName")

package xyz.aerii.template

import net.fabricmc.api.ClientModInitializer
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

object Template : ClientModInitializer {
    const val modVersion: String = /*$ mod_version*/ "0.0.1"
    const val modId: String = /*$ mod_id*/ "template"
    const val modName: String = /*$ mod_name*/ "Template"

    @JvmField
    val LOGGER: Logger = LogManager.getLogger(Template::class.java)

    override fun onInitializeClient() {
        LOGGER.info("Template mod initialised.")
        LOGGER.info("Mixins are automatically loaded, you don't need to add them to the mixins.json file!")
    }
}