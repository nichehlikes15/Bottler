@file:Suppress("ConstPropertyName")

package xyz.aerii.template

import net.fabricmc.api.ClientModInitializer
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.Minecraft
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import kotlin.random.Random
import utils.RotationUtil
import utils.BlockScanner
import utils.BlockWatchUtil
import utils.holdLeftClick
import utils.releaseLeftClick
import utils.MiningTargetVisuals
import net.minecraft.core.BlockPos
import net.minecraft.world.item.Item
import handlers.Typo.modMessage
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import utils.Ability

object Template : ClientModInitializer {
    const val modVersion: String = /*$ mod_version*/ "0.0.1"
    const val modId: String = /*$ mod_id*/ "template"
    const val modName: String = /*$ mod_name*/ "Template"

    @JvmField
    val LOGGER: Logger = LogManager.getLogger(Template::class.java)
    private const val NO_TARGET_RETRY_TICKS = 10
    private const val BLOCK_MINE_TIMEOUT_MS = 5_000L
    private const val MAX_CONSECUTIVE_MINE_FAILURES = 3
    private var snapLoopActive = false
    private var nextSnapDelayTicks = 0
    private var lastMinedPos: BlockPos? = null
    private var currentMiningPos: BlockPos? = null
    private var miningStartedMs: Long = 0L
    private var consecutiveMineFailures: Int = 0
    private var startingToolItem: Item? = null

    override fun onInitializeClient() {
        LOGGER.info("Template mod initialised.")
        LOGGER.info("Mixins are automatically loaded, you don't need to add them to the mixins.json file!")
        MiningTargetVisuals.registerIfNeeded()

        ClientTickEvents.START_CLIENT_TICK.register {
            BlockWatchUtil.tick()
            RotationUtil.tick()
            Ability.tick()

            if (snapLoopActive) {
                safetyTick()
            }

            if (snapLoopActive && nextSnapDelayTicks > 0) {
                nextSnapDelayTicks--
                if (nextSnapDelayTicks == 0) {
                    snapCommand()
                }
            }
        }

        ClientReceiveMessageEvents.GAME.register { message, _ ->
            Ability.onChat(message)
        }

        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->

            dispatcher.register(
                literal("snap").executes { ctx ->
                    if (snapLoopActive) {
                        modMessage("Stop Snapping")
                        Ability.stop()
                        stopSnapLoop()
                    } else {
                        modMessage("Snapping...")
                        LOGGER.info("Snap loop started")
                        snapLoopActive = true
                        nextSnapDelayTicks = 0
                        consecutiveMineFailures = 0
                        currentMiningPos = null
                        miningStartedMs = 0L

                        Ability.start()
                        val mc = Minecraft.getInstance()
                        val player = mc.player
                        startingToolItem = player?.mainHandItem?.item
                        if (startingToolItem == null) {
                            stopSnapLoop("No player/tool found; stopping.")
                            return@executes 1
                        }

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

            currentMiningPos = pos.immutable()
            miningStartedMs = System.currentTimeMillis()
            holdLeftClick()

            BlockWatchUtil.watch(pos) {
                if (!snapLoopActive) return@watch
                //LOGGER.info("Block turned into bedrock!")
                consecutiveMineFailures = 0
                releaseLeftClick()
                RotationUtil.clearIdleAroundBlock()
                lastMinedPos = pos.immutable()
                currentMiningPos = null
                miningStartedMs = 0L
                nextSnapDelayTicks = Random.nextInt(2, 6)
            }

        } else {
            //LOGGER.info("No whitelisted blocks found")
            releaseLeftClick()
            BlockWatchUtil.clear()
            RotationUtil.clearIdleAroundBlock()
            MiningTargetVisuals.clear()
            currentMiningPos = null
            miningStartedMs = 0L
            nextSnapDelayTicks = NO_TARGET_RETRY_TICKS
        }
    }

    private fun stopSnapLoop(reason: String? = null) {
        if (!snapLoopActive) return
        snapLoopActive = false
        releaseLeftClick()
        BlockWatchUtil.clear()
        RotationUtil.clearIdleAroundBlock()
        MiningTargetVisuals.clear()
        lastMinedPos = null
        nextSnapDelayTicks = 0
        currentMiningPos = null
        miningStartedMs = 0L
        consecutiveMineFailures = 0
        startingToolItem = null
        if (reason != null) {
            modMessage(reason)
        }
        LOGGER.info("Snap loop stopped")
    }

    private fun safetyTick() {
        val mc = Minecraft.getInstance()
        val player = mc.player ?: run {
            stopSnapLoop("Snap stopped: player missing.")
            return
        }

        // Tool safety: if the item in main hand changes mid-run, stop immediately.
        val expected = startingToolItem
        val current = player.mainHandItem.item
        if (expected != null && current != expected) {
            val expectedId = BuiltInRegistries.ITEM.getKey(expected).toString()
            val currentId = BuiltInRegistries.ITEM.getKey(current).toString()
            stopSnapLoop("Snap stopped: tool changed ($expectedId -> $currentId).")
            return
        }

        // Mining timeout safety: don't hold a block longer than 5 seconds.
        val pos = currentMiningPos ?: return
        val started = miningStartedMs
        if (started <= 0L) return
        val elapsed = System.currentTimeMillis() - started
        if (elapsed < BLOCK_MINE_TIMEOUT_MS) return

        consecutiveMineFailures++
        LOGGER.info("Mining timed out at $pos after ${elapsed}ms (failures=$consecutiveMineFailures)")

        if (consecutiveMineFailures >= MAX_CONSECUTIVE_MINE_FAILURES) {
            stopSnapLoop("Snap stopped: couldn't mine a block ${MAX_CONSECUTIVE_MINE_FAILURES} times in a row.")
            return
        }

        // Treat as "broken": release, clear state, move anchor forward, and retry after a short delay.
        releaseLeftClick()
        BlockWatchUtil.clear()
        RotationUtil.clearIdleAroundBlock()
        MiningTargetVisuals.clear()

        lastMinedPos = pos.immutable()
        currentMiningPos = null
        miningStartedMs = 0L
        nextSnapDelayTicks = Random.nextInt(2, 6)
    }
}