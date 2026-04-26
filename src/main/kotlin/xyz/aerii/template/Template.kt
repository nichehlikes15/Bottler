@file:Suppress("ConstPropertyName")

package xyz.aerii.template

import com.jcraft.jorbis.Block
import net.fabricmc.api.ClientModInitializer
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import utils.RotationUtil
import utils.BlockScanner

object Template : ClientModInitializer {
    const val modVersion: String = /*$ mod_version*/ "0.0.1"
    const val modId: String = /*$ mod_id*/ "template"
    const val modName: String = /*$ mod_name*/ "Template"

    @JvmField
    val LOGGER: Logger = LogManager.getLogger(Template::class.java)

    override fun onInitializeClient() {
        LOGGER.info("Template mod initialised.")
        LOGGER.info("Mixins are automatically loaded, you don't need to add them to the mixins.json file!")

        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->

            dispatcher.register(
                literal("snap").executes { ctx ->

                    LOGGER.info("Snap to block run")

                    val pos = BlockScanner.findGrayWool(4)

                    if (pos != null) {
                        RotationUtil.snapToBlock(pos)
                        utils.leftClick()

                    } else {
                        LOGGER.info("No gray wool found")
                    }
    
                    1
                }
            )
        }
    }
}