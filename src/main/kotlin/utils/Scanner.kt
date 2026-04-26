package utils

import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks

object BlockScanner {

    private val mc = Minecraft.getInstance()

    fun findGrayWool(radius: Int = 4): BlockPos? {
        val player = mc.player ?: return null
        val level = mc.level ?: return null

        val base = player.blockPosition()

        for (dx in -radius..radius) {
            for (dy in -radius..radius) {
                for (dz in -radius..radius) {

                    val pos = base.offset(dx, dy, dz)
                    val state = level.getBlockState(pos)

                    if (state.block == Blocks.GRAY_WOOL) {
                        return pos
                    }
                }
            }
        }

        return null
    }
}