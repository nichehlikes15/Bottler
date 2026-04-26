@file:Suppress("ConstPropertyName")

package xyz.aerii.template

import net.fabricmc.api.ClientModInitializer
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.Minecraft
import kotlin.random.Random
import utils.RotationUtil
import utils.BlockScanner
import utils.BlockWatchUtil
import utils.holdLeftClick
import utils.releaseLeftClick

object Template : ClientModInitializer {
    const val modVersion: String = /*$ mod_version*/ "0.0.1"
    const val modId: String = /*$ mod_id*/ "template"
    const val modName: String = /*$ mod_name*/ "Template"

    @JvmField
    val LOGGER: Logger = LogManager.getLogger(Template::class.java)
    private var snapLoopActive = false
    private var nextSnapDelayTicks = 0

    override fun onInitializeClient() {
        LOGGER.info("Template mod initialised.")
        LOGGER.info("Mixins are automatically loaded, you don't need to add them to the mixins.json file!")

        ClientTickEvents.END_CLIENT_TICK.register {
            BlockWatchUtil.tick()
            RotationUtil.tick()
            if (snapLoopActive && nextSnapDelayTicks > 0) {
                nextSnapDelayTicks--
                if (nextSnapDelayTicks == 0) {
                    snapCommand()
                }
            }
        }

        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->

            dispatcher.register(
                literal("snap").executes { ctx ->
                    if (snapLoopActive) {
                        stopSnapLoop()
                    } else {
                        LOGGER.info("Snap loop started")
                        snapLoopActive = true
                        nextSnapDelayTicks = 0
                        snapCommand()
                    }

                    1
                }
            )
        }
    }

    fun snapCommand() {
        if (!snapLoopActive) return

        val pos = BlockScanner.findGrayWool(4)

        if (pos != null) {

            RotationUtil.smoothRotateToBlock(pos)
            RotationUtil.setIdleAroundBlock(pos)

            holdLeftClick()

            BlockWatchUtil.watch(pos) {
                if (!snapLoopActive) return@watch
                LOGGER.info("Block turned into bedrock!")
                releaseLeftClick()
                RotationUtil.clearIdleAroundBlock()
                nextSnapDelayTicks = Random.nextInt(2, 6)
            }

        } else {
            LOGGER.info("No gray wool found")
            stopSnapLoop()
        }
    }

    private fun stopSnapLoop() {
        if (!snapLoopActive) return
        snapLoopActive = false
        releaseLeftClick()
        BlockWatchUtil.clear()
        RotationUtil.clearIdleAroundBlock()
        nextSnapDelayTicks = 0
        LOGGER.info("Snap loop stopped")
    }
}