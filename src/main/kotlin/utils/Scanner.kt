package utils

import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.block.Block
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import kotlin.math.min
import kotlin.random.Random

object BlockScanner {

    private val mc = Minecraft.getInstance()
    private const val BREAK_TIMEOUT_MS = 15_000L
    private const val AIMPOINT_TRIES = 24

    private var currentTarget: BlockPos? = null
    private var targetSinceMs: Long = 0L

    fun findGrayWool(radius: Int = 4): BlockPos? = findWhitelistedBlock(radius)

    data class ScanTarget(val pos: BlockPos, val aimPoint: Vec3)

    fun findWhitelistedTarget(radius: Int = 4, anchor: BlockPos? = null): ScanTarget? {
        val pos = findWhitelistedBlock(radius, anchor) ?: return null
        val aim = findAimPointForBlock(pos) ?: Vec3(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5)
        return ScanTarget(pos, aim)
    }

    fun findWhitelistedBlock(radius: Int = 4): BlockPos? {
        return findWhitelistedBlock(radius, anchor = null)
    }

    fun findWhitelistedBlock(radius: Int = 4, anchor: BlockPos? = null): BlockPos? {
        val player = mc.player ?: return null
        val level = mc.level ?: return null

        val now = System.currentTimeMillis()
        val playerPos = player.eyePosition
        val interactionReach = player.blockInteractionRange()
        val maxDistanceSq = min(radius.toDouble(), interactionReach).let { it * it }
        val prioritizedBlocks = BlockWhitelist.prioritizedBlocks
        if (prioritizedBlocks.isEmpty()) return null

        fun distanceSqToEye(pos: BlockPos): Double {
            val centerX = pos.x + 0.5
            val centerY = pos.y + 0.5
            val centerZ = pos.z + 0.5
            return playerPos.distanceToSqr(centerX, centerY, centerZ)
        }

        fun distanceSqToAnchor(pos: BlockPos): Double {
            val a = anchor ?: return distanceSqToEye(pos)
            val dx = (pos.x - a.x).toDouble()
            val dy = (pos.y - a.y).toDouble()
            val dz = (pos.z - a.z).toDouble()
            // block-to-block closeness: use block-grid distance (fast + stable)
            return (dx * dx) + (dy * dy) + (dz * dz)
        }

        fun canDirectlyTarget(pos: BlockPos): Boolean {
            val center = Vec3(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5)
            val hit = level.clip(
                ClipContext(
                    playerPos,
                    center,
                    ClipContext.Block.OUTLINE,
                    ClipContext.Fluid.NONE,
                    player
                )
            )
            return hit.type == HitResult.Type.BLOCK &&
                hit is BlockHitResult &&
                hit.blockPos == pos
        }

        fun isWhitelisted(block: Block): Boolean {
            LOGGER.info("isWhitelisted: $block")
            return prioritizedBlocks.contains(block)
        }

        // Keep current target while within timeout, unless it has already been broken.
        currentTarget?.let { target ->
            val currentState = level.getBlockState(target)
            if (!isWhitelisted(currentState.block)) {
                currentTarget = null
                targetSinceMs = 0L
            } else if (distanceSqToEye(target) > maxDistanceSq) {
                currentTarget = null
                targetSinceMs = 0L
            } else if (!canDirectlyTarget(target)) {
                currentTarget = null
                targetSinceMs = 0L
            } else if (now - targetSinceMs < BREAK_TIMEOUT_MS) {
                return target
            }
        }

        val base = player.blockPosition()
        var nearest: BlockPos? = null
        var nearestDistanceSq = Double.MAX_VALUE

        val timedOutTarget = currentTarget
        for (targetBlock in prioritizedBlocks) {
            nearest = null
            nearestDistanceSq = Double.MAX_VALUE

            for (dx in -radius..radius) {
                for (dy in -radius..radius) {
                    for (dz in -radius..radius) {
                        val pos = base.offset(dx, dy, dz)
                        val state = level.getBlockState(pos)
                        if (state.block != targetBlock) continue

                        // If the previous target timed out, force a switch away from it when possible.
                        if (timedOutTarget != null && pos == timedOutTarget) continue

                        val eyeDistanceSq = distanceSqToEye(pos)
                        if (eyeDistanceSq > maxDistanceSq) continue
                        if (!canDirectlyTarget(pos)) continue

                        // Prefer blocks closest to the last-mined block, but still enforce reach/visibility from player.
                        val anchorDistanceSq = distanceSqToAnchor(pos)
                        if (anchorDistanceSq < nearestDistanceSq) {
                            nearestDistanceSq = anchorDistanceSq
                            nearest = pos.immutable()
                        }
                    }
                }
            }

            if (nearest != null) break
        }

        if (nearest != null) {
            currentTarget = nearest
            targetSinceMs = now
            return nearest
        }

        // If only the timed out block is available, keep trying it.
        if (
            timedOutTarget != null &&
            isWhitelisted(level.getBlockState(timedOutTarget).block) &&
            distanceSqToEye(timedOutTarget) <= maxDistanceSq &&
            canDirectlyTarget(timedOutTarget)
        ) {
            currentTarget = timedOutTarget
            targetSinceMs = now
            return timedOutTarget
        }

        currentTarget = null
        targetSinceMs = 0L
        return null
    }

    private fun findAimPointForBlock(pos: BlockPos): Vec3? {
        val player = mc.player ?: return null
        val level = mc.level ?: return null
        val eyePos = player.eyePosition
        val reach = player.blockInteractionRange()
        val maxDistSq = reach * reach

        fun tryPoint(candidate: Vec3): Vec3? {
            if (eyePos.distanceToSqr(candidate) > maxDistSq) return null
            val hit = level.clip(
                ClipContext(
                    eyePos,
                    candidate,
                    ClipContext.Block.OUTLINE,
                    ClipContext.Fluid.NONE,
                    player
                )
            )
            if (hit.type != HitResult.Type.BLOCK) return null
            if (hit !is BlockHitResult) return null
            if (hit.blockPos != pos) return null
            return hit.location
        }

        // First attempt: center ray (fast path).
        val center = Vec3(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5)
        tryPoint(center)?.let { return it }

        // Then sample points inside the block volume; clip() will snap to the face actually hit.
        repeat(AIMPOINT_TRIES) {
            val rx = Random.nextDouble(0.15, 0.85)
            val ry = Random.nextDouble(0.15, 0.85)
            val rz = Random.nextDouble(0.15, 0.85)
            val candidate = Vec3(pos.x + rx, pos.y + ry, pos.z + rz)
            tryPoint(candidate)?.let { return it }
        }

        return null
    }
}