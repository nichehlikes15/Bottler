package utils

import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import kotlin.math.atan2
import kotlin.math.sqrt

object RotationUtil {

    private val mc = Minecraft.getInstance()

    fun snapToBlock(pos: BlockPos) {
        val player = mc.player ?: return

        val eyeX = player.x
        val eyeY = player.y + player.eyeHeight.toDouble()
        val eyeZ = player.z

        val targetX = pos.x + 0.5
        val targetY = pos.y + 0.5
        val targetZ = pos.z + 0.5

        val diffX = targetX - eyeX
        val diffY = targetY - eyeY
        val diffZ = targetZ - eyeZ

        val horizontal = sqrt(diffX * diffX + diffZ * diffZ)

        val yaw = Math.toDegrees(atan2(diffZ, diffX)).toFloat() - 90f
        val pitch = (-Math.toDegrees(atan2(diffY, horizontal))).toFloat()

        player.yRot = yaw
        player.xRot = pitch
    }
}