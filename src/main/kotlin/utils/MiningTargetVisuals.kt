package utils

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.RenderType
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

object MiningTargetVisuals {

    private val mc = Minecraft.getInstance()
    private var registered = false

    private var miningPos: BlockPos? = null
    private var aimPoint: Vec3? = null

    fun registerIfNeeded() {
        if (registered) return
        registered = true

        WorldRenderEvents.END_MAIN.register { _ ->
            val player = mc.player ?: return@register
            val pos = miningPos ?: return@register
            val point = aimPoint

            val matrices: PoseStack = PoseStack()
            matrices.pushPose()

            val cameraPos = mc.gameRenderer.mainCamera.position
            matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z)

            // Use vanilla's line RenderType; submit to an immediate buffer source.
            val bufferSource = mc.renderBuffers().bufferSource()
            val lineBuffer = bufferSource.getBuffer(RenderType.lines())

            // Blue outline around the block currently being mined.
            val blockBox = AABB(pos).inflate(0.002)
            renderLineBox(matrices, lineBuffer, blockBox, 0.15f, 0.45f, 1.0f, 1.0f)

            // Green point (small cube) used as the rotation target.
            if (point != null) {
                val s = 0.035
                val pointBox = AABB(
                    point.x - s, point.y - s, point.z - s,
                    point.x + s, point.y + s, point.z + s
                )
                renderLineBox(matrices, lineBuffer, pointBox, 0.2f, 1.0f, 0.2f, 1.0f)
            }

            matrices.popPose()
            bufferSource.endBatch(RenderType.lines())
        }
    }

    fun setTarget(pos: BlockPos, point: Vec3) {
        miningPos = pos
        aimPoint = point
    }

    fun clear() {
        miningPos = null
        aimPoint = null
    }

    private fun renderLineBox(
        matrices: PoseStack,
        consumer: VertexConsumer,
        box: AABB,
        r: Float,
        g: Float,
        b: Float,
        a: Float
    ) {
        val pose = matrices.last()

        fun v(x: Double, y: Double, z: Double) {
            consumer
                .addVertex(pose, x.toFloat(), y.toFloat(), z.toFloat())
                .setColor(r, g, b, a)
                .setNormal(pose, 0f, 1f, 0f)
        }

        val x1 = box.minX
        val y1 = box.minY
        val z1 = box.minZ
        val x2 = box.maxX
        val y2 = box.maxY
        val z2 = box.maxZ

        // Bottom rectangle
        v(x1, y1, z1); v(x2, y1, z1)
        v(x2, y1, z1); v(x2, y1, z2)
        v(x2, y1, z2); v(x1, y1, z2)
        v(x1, y1, z2); v(x1, y1, z1)

        // Top rectangle
        v(x1, y2, z1); v(x2, y2, z1)
        v(x2, y2, z1); v(x2, y2, z2)
        v(x2, y2, z2); v(x1, y2, z2)
        v(x1, y2, z2); v(x1, y2, z1)

        // Vertical edges
        v(x1, y1, z1); v(x1, y2, z1)
        v(x2, y1, z1); v(x2, y2, z1)
        v(x2, y1, z2); v(x2, y2, z2)
        v(x1, y1, z2); v(x1, y2, z2)
    }
}