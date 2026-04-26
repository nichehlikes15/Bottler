package utils

import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks

object BlockWatchUtil {

    private val mc = Minecraft.getInstance()

    private var watchedPos: BlockPos? = null
    private var lastWasAir: Boolean = false
    private var callback: (() -> Unit)? = null

    fun watch(pos: BlockPos, onBreak: () -> Unit) {
        watchedPos = pos
        callback = onBreak
        lastWasAir = isAir(pos)
    }

    fun clear() {
        watchedPos = null
        callback = null
        lastWasAir = false
    }

    fun tick() {
        val pos = watchedPos ?: return

        val nowAir = isAir(pos)

        // detect: not air -> air (block broke)
        if (!lastWasAir && nowAir) {
            callback?.invoke()
        }

        lastWasAir = nowAir
    }

    private fun isAir(pos: BlockPos): Boolean {
        val level = mc.level ?: return false
        return level.getBlockState(pos).block == Blocks.AIR
    }
}