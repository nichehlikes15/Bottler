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
    private const val BREAK_TIMEOUT_MS = 5_000L
    private const val AIMPOINT_TRIES = 24

    private var currentTarget: BlockPos? = null
    private var targetSinceMs: Long = 0L

    data class ScanTarget(val pos: BlockPos, val aimPoint: Vec3)

    fun findWhitelistedTarget(radius: Int = 4, anchor: BlockPos? = null): ScanTarget? {
        val pos = findWhitelistedBlock(radius, anchor) ?: return null
        val aim = findAimPointForBlock(pos)
            ?: Vec3(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5)

        return ScanTarget(pos, aim)
    }

    fun findWhitelistedBlock(radius: Int = 4, anchor: BlockPos? = null): BlockPos? {
        val player = mc.player ?: return null
        val level = mc.level ?: return null

        val now = System.currentTimeMillis()
        val playerPos = player.eyePosition
        val reach = player.blockInteractionRange()
        val maxDistanceSq = min(radius.toDouble(), reach).let { it * it }

        val entries = BlockWhitelist.prioritizedBlocks

        val group1 = entries
            .filter { it.group == BlockWhitelist.Group.GROUP_1 }
            .map { it.block }

        val group2 = entries
            .filter { it.group == BlockWhitelist.Group.GROUP_2 }
            .map { it.block }

        fun distanceSqToEye(pos: BlockPos): Double {
            return playerPos.distanceToSqr(
                pos.x + 0.5,
                pos.y + 0.5,
                pos.z + 0.5
            )
        }

        fun distanceSqToAnchor(pos: BlockPos): Double {
            val a = anchor ?: return distanceSqToEye(pos)
            val dx = (pos.x - a.x).toDouble()
            val dy = (pos.y - a.y).toDouble()
            val dz = (pos.z - a.z).toDouble()
            return dx * dx + dy * dy + dz * dz
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

        currentTarget?.let { target ->
            val state = level.getBlockState(target)

            if (!entries.any { it.block == state.block }) {
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

        fun scan(blocks: List<Block>): BlockPos? {
            var nearest: BlockPos? = null
            var nearestDist = Double.MAX_VALUE

            for (block in blocks) {
                for (dx in -radius..radius) {
                    for (dy in -radius..radius) {
                        for (dz in -radius..radius) {

                            val pos = base.offset(dx, dy, dz)
                            val state = level.getBlockState(pos)

                            if (state.block != block) continue
                            if (distanceSqToEye(pos) > maxDistanceSq) continue
                            if (!canDirectlyTarget(pos)) continue

                            val dist = distanceSqToAnchor(pos)
                            if (dist < nearestDist) {
                                nearestDist = dist
                                nearest = pos.immutable()
                            }
                        }
                    }
                }
            }

            return nearest
        }

        val g1Result = scan(group1)
        if (g1Result != null) {
            currentTarget = g1Result
            targetSinceMs = now
            return g1Result
        }

        val g2Result = scan(group2)
        if (g2Result != null) {
            currentTarget = g2Result
            targetSinceMs = now
            return g2Result
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

        val center = Vec3(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5)
        tryPoint(center)?.let { return it }

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