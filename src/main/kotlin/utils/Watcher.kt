package utils

import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks

object BlockWatchUtil {

    private val mc = Minecraft.getInstance()

    private var watchedPos: BlockPos? = null
    private var lastWasBedrock: Boolean = false
    private var callback: (() -> Unit)? = null

    fun watch(pos: BlockPos, onBreak: () -> Unit) {
        watchedPos = pos
        callback = onBreak
        lastWasBedrock = isBedrock(pos)
    }

    fun clear() {
        watchedPos = null
        callback = null
        lastWasBedrock = false
    }

    fun tick() {
        val pos = watchedPos ?: return

        val nowBedrock = isBedrock(pos)

        // detect: not bedrock -> bedrock (block replaced after mining)
        if (!lastWasBedrock && nowBedrock) {
            callback?.invoke()
        }

        lastWasBedrock = nowBedrock
    }

    private fun isBedrock(pos: BlockPos): Boolean {
        val level = mc.level ?: return false
        return level.getBlockState(pos).block == Blocks.BEDROCK
    }
}