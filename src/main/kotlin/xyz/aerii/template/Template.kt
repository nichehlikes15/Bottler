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
import utils.MiningTargetVisuals
import net.minecraft.core.BlockPos

object Template : ClientModInitializer {
    const val modVersion: String = /*$ mod_version*/ "0.0.1"
    const val modId: String = /*$ mod_id*/ "template"
    const val modName: String = /*$ mod_name*/ "Template"

    @JvmField
    val LOGGER: Logger = LogManager.getLogger(Template::class.java)
    private const val NO_TARGET_RETRY_TICKS = 10
    private var snapLoopActive = false
    private var nextSnapDelayTicks = 0
    private var lastMinedPos: BlockPos? = null

    override fun onInitializeClient() {
        LOGGER.info("Template mod initialised.")
        LOGGER.info("Mixins are automatically loaded, you don't need to add them to the mixins.json file!")
        MiningTargetVisuals.registerIfNeeded()

        ClientTickEvents.START_CLIENT_TICK.register {
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

            dispatcher.register(
                literal("lookblock").executes { ctx ->
                    runBlock()

                    1
                }
            )
        }
    }

    private fun runBlock() {
        val mc = Minecraft.getInstance()
        val player = mc.player ?: return
        val level = mc.level ?: return

        val eyePos = player.eyePosition
        val lookVec = player.lookAngle
        val reach = player.blockInteractionRange()

        val endPos = eyePos.add(
            lookVec.x * reach,
            lookVec.y * reach,
            lookVec.z * reach
        )

        val hit = level.clip(
            net.minecraft.world.level.ClipContext(
                eyePos,
                endPos,
                net.minecraft.world.level.ClipContext.Block.OUTLINE,
                net.minecraft.world.level.ClipContext.Fluid.NONE,
                player
            )
        )

        if (hit.type == net.minecraft.world.phys.HitResult.Type.BLOCK) {
            val blockHit = hit as net.minecraft.world.phys.BlockHitResult
            val pos = blockHit.blockPos
            val state = level.getBlockState(pos)
            val block = state.block

            LOGGER.info("Looking at block: ${block.name.string}")
            LOGGER.info("Position: ${pos.x}, ${pos.y}, ${pos.z}")
            LOGGER.info("Block ID: ${net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block)}")
        } else {
            LOGGER.info("You are not looking at a block")
        }
    }

    fun snapCommand() {
        if (!snapLoopActive) return

        val target = BlockScanner.findWhitelistedTarget(4, anchor = lastMinedPos)

        if (target != null) {
            val pos = target.pos
            val aim = target.aimPoint

            MiningTargetVisuals.setTarget(pos, aim)
            RotationUtil.smoothRotateToPoint(aim)
            RotationUtil.setIdleAroundBlock(pos)

            holdLeftClick()

            BlockWatchUtil.watch(pos) {
                if (!snapLoopActive) return@watch
                LOGGER.info("Block turned into bedrock!")
                releaseLeftClick()
                RotationUtil.clearIdleAroundBlock()
                lastMinedPos = pos.immutable()
                nextSnapDelayTicks = Random.nextInt(2, 6)
            }

        } else {
            LOGGER.info("No whitelisted blocks found")
            releaseLeftClick()
            BlockWatchUtil.clear()
            RotationUtil.clearIdleAroundBlock()
            MiningTargetVisuals.clear()
            nextSnapDelayTicks = NO_TARGET_RETRY_TICKS
        }
    }

    private fun stopSnapLoop() {
        if (!snapLoopActive) return
        snapLoopActive = false
        releaseLeftClick()
        BlockWatchUtil.clear()
        RotationUtil.clearIdleAroundBlock()
        MiningTargetVisuals.clear()
        lastMinedPos = null
        nextSnapDelayTicks = 0
        LOGGER.info("Snap loop stopped")
    }
}