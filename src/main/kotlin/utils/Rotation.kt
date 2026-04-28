package utils

import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

object RotationUtil {

    private val mc = Minecraft.getInstance()
    private var rotateActive = false
    private var targetYaw = 0f
    private var targetPitch = 0f
    private var yawMaxSpeed = 0f
    private var pitchMaxSpeed = 0f
    private var yawVelocity = 0f
    private var pitchVelocity = 0f
    private var yawAccel = 0f
    private var pitchAccel = 0f
    private var jitterStrength = 0f
    private var jitterTicks = 0
    private var initialYawDelta = 1f
    private var initialPitchDelta = 1f
    private var pathWiggleYawAmp = 0f
    private var pathWigglePitchAmp = 0f
    private var pathWiggleFreq = 0f
    private var pathWigglePhase = 0f
    private var idleTargetPos: BlockPos? = null
    private var idleTick = 0
    private var idlePatternDuration = 0
    private var idleYawAmp = 0f
    private var idlePitchAmp = 0f
    private var idleYawFreq = 0f
    private var idlePitchFreq = 0f
    private var idleDriftYaw = 0f
    private var idleDriftPitch = 0f

    fun snapToBlock(pos: BlockPos) {
        val player = mc.player ?: return

        val (yaw, pitch) = calculateAngles(
            player.x,
            player.y + player.eyeHeight.toDouble(),
            player.z,
            pos.x + 0.5,
            pos.y + 0.5,
            pos.z + 0.5
        )

        player.yRot = yaw
        player.xRot = pitch
    }

    fun smoothRotateToBlock(pos: BlockPos) {
        val player = mc.player ?: return
        val (yaw, pitch) = calculateAngles(
            player.x,
            player.y + player.eyeHeight.toDouble(),
            player.z,
            pos.x + 0.5,
            pos.y + 0.5,
            pos.z + 0.5
        )
        targetYaw = yaw
        targetPitch = pitch
        yawMaxSpeed = Random.nextFloat() * 3.5f + 8.5f      // 8.5..12.0
        pitchMaxSpeed = Random.nextFloat() * 3.0f + 7.0f    // 7.0..10.0
        yawAccel = Random.nextFloat() * 1.4f + 2.6f         // 2.6..4.0
        pitchAccel = Random.nextFloat() * 1.2f + 2.2f       // 2.2..3.4
        yawVelocity = 0f
        pitchVelocity = 0f
        jitterStrength = Random.nextFloat() * 0.55f + 0.15f
        jitterTicks = Random.nextInt(4, 10)
        initialYawDelta = kotlin.math.abs(wrapDegrees(targetYaw - player.yRot)).coerceAtLeast(1f)
        initialPitchDelta = kotlin.math.abs(targetPitch - player.xRot).coerceAtLeast(1f)
        pathWiggleYawAmp = Random.nextFloat() * 2.2f + 0.8f
        pathWigglePitchAmp = Random.nextFloat() * 1.3f + 0.35f
        pathWiggleFreq = Random.nextFloat() * 1.2f + 1.6f
        pathWigglePhase = Random.nextFloat() * (Math.PI.toFloat() * 2f)
        rotateActive = true
    }

    fun snapToPoint(point: Vec3) {
        val player = mc.player ?: return
        val (yaw, pitch) = calculateAngles(
            player.x,
            player.y + player.eyeHeight.toDouble(),
            player.z,
            point.x,
            point.y,
            point.z
        )
        player.yRot = yaw
        player.xRot = pitch
    }

    fun smoothRotateToPoint(point: Vec3) {
        val player = mc.player ?: return
        val (yaw, pitch) = calculateAngles(
            player.x,
            player.y + player.eyeHeight.toDouble(),
            player.z,
            point.x,
            point.y,
            point.z
        )
        targetYaw = yaw
        targetPitch = pitch
        yawMaxSpeed = Random.nextFloat() * 3.5f + 8.5f      // 8.5..12.0
        pitchMaxSpeed = Random.nextFloat() * 3.0f + 7.0f    // 7.0..10.0
        yawAccel = Random.nextFloat() * 1.4f + 2.6f         // 2.6..4.0
        pitchAccel = Random.nextFloat() * 1.2f + 2.2f       // 2.2..3.4
        yawVelocity = 0f
        pitchVelocity = 0f
        jitterStrength = Random.nextFloat() * 0.55f + 0.15f
        jitterTicks = Random.nextInt(4, 10)
        initialYawDelta = kotlin.math.abs(wrapDegrees(targetYaw - player.yRot)).coerceAtLeast(1f)
        initialPitchDelta = kotlin.math.abs(targetPitch - player.xRot).coerceAtLeast(1f)
        pathWiggleYawAmp = Random.nextFloat() * 2.2f + 0.8f
        pathWigglePitchAmp = Random.nextFloat() * 1.3f + 0.35f
        pathWiggleFreq = Random.nextFloat() * 1.2f + 1.6f
        pathWigglePhase = Random.nextFloat() * (Math.PI.toFloat() * 2f)
        rotateActive = true
    }

    fun tick() {
        val player = mc.player ?: return

        if (!rotateActive) {
            applyIdleMovement(player)
            return
        }

        val baseYawDelta = wrapDegrees(targetYaw - player.yRot)
        val basePitchDelta = targetPitch - player.xRot

        val yawProgress = (1f - (kotlin.math.abs(baseYawDelta) / initialYawDelta)).coerceIn(0f, 1f)
        val pitchProgress = (1f - (kotlin.math.abs(basePitchDelta) / initialPitchDelta)).coerceIn(0f, 1f)
        val wiggleEnvelope = sin(yawProgress * Math.PI).toFloat()
        val pitchWiggleEnvelope = sin(pitchProgress * Math.PI).toFloat()
        val wiggleAngle = (yawProgress * Math.PI.toFloat() * pathWiggleFreq) + pathWigglePhase
        val wiggleYaw = sin(wiggleAngle) * pathWiggleYawAmp * wiggleEnvelope
        val wigglePitch = cos(wiggleAngle * 0.9f) * pathWigglePitchAmp * pitchWiggleEnvelope

        val yawDelta = wrapDegrees((targetYaw + wiggleYaw) - player.yRot)
        val pitchDelta = (targetPitch + wigglePitch) - player.xRot
        val yawAbs = kotlin.math.abs(yawDelta)
        val pitchAbs = kotlin.math.abs(pitchDelta)

        val largeTurnBoost = ((yawAbs - 100f) / 80f).coerceIn(0f, 1f)
        val boostedYawMaxSpeed = yawMaxSpeed + (yawMaxSpeed * 0.55f * largeTurnBoost)
        val boostedYawAccel = yawAccel + (yawAccel * 0.45f * largeTurnBoost)

        val desiredYawVel = yawDelta.coerceIn(-boostedYawMaxSpeed, boostedYawMaxSpeed)
        val desiredPitchVel = pitchDelta.coerceIn(-pitchMaxSpeed, pitchMaxSpeed)
        yawVelocity += (desiredYawVel - yawVelocity).coerceIn(-boostedYawAccel, boostedYawAccel)
        pitchVelocity += (desiredPitchVel - pitchVelocity).coerceIn(-pitchAccel, pitchAccel)

        val minYawStep = max(0.35f, yawMaxSpeed * 0.045f)
        val minPitchStep = max(0.3f, pitchMaxSpeed * 0.045f)
        val appliedYaw = yawVelocity.coerceIn(-yawAbs, yawAbs).let { step ->
            if (yawAbs > 2.0f) step else step.coerceIn(-minYawStep, minYawStep)
        }
        val appliedPitch = pitchVelocity.coerceIn(-pitchAbs, pitchAbs).let { step ->
            if (pitchAbs > 2.0f) step else step.coerceIn(-minPitchStep, minPitchStep)
        }

        var nextYaw = player.yRot + appliedYaw
        var nextPitch = player.xRot + appliedPitch

        // Small, decaying noise to avoid perfectly robotic movement.
        if (jitterTicks > 0) {
            val decay = (jitterTicks / 10f).coerceIn(0.15f, 1f)
            val yawNoise = (Random.nextFloat() * 2f - 1f) * jitterStrength * decay
            val pitchNoise = (Random.nextFloat() * 2f - 1f) * (jitterStrength * 0.65f) * decay
            nextYaw += yawNoise
            nextPitch += pitchNoise
            jitterTicks--
        }

        player.yRot = nextYaw
        player.xRot = nextPitch.coerceIn(-90f, 90f)

        if (kotlin.math.abs(baseYawDelta) < 0.7f && kotlin.math.abs(basePitchDelta) < 0.7f) {
            player.yRot = targetYaw
            player.xRot = targetPitch
            yawVelocity = 0f
            pitchVelocity = 0f
            rotateActive = false
        }
    }

    fun setIdleAroundBlock(pos: BlockPos) {
        idleTargetPos = pos
        idleTick = 0
        idlePatternDuration = Random.nextInt(18, 44)
        idleYawAmp = Random.nextFloat() * 1.2f + 0.35f
        idlePitchAmp = Random.nextFloat() * 0.75f + 0.2f
        idleYawFreq = Random.nextFloat() * 0.16f + 0.12f
        idlePitchFreq = Random.nextFloat() * 0.14f + 0.1f
        idleDriftYaw = 0f
        idleDriftPitch = 0f
    }

    fun clearIdleAroundBlock() {
        idleTargetPos = null
        idleTick = 0
    }

    private fun applyIdleMovement(player: net.minecraft.client.player.LocalPlayer) {
        val pos = idleTargetPos ?: return
        val (baseYaw, basePitch) = calculateAngles(
            player.x,
            player.y + player.eyeHeight.toDouble(),
            player.z,
            pos.x + 0.5,
            pos.y + 0.5,
            pos.z + 0.5
        )
        idleTick++

        if (idleTick % idlePatternDuration == 0) {
            idlePatternDuration = Random.nextInt(18, 44)
            idleYawAmp = Random.nextFloat() * 1.2f + 0.35f
            idlePitchAmp = Random.nextFloat() * 0.75f + 0.2f
            idleYawFreq = Random.nextFloat() * 0.16f + 0.12f
            idlePitchFreq = Random.nextFloat() * 0.14f + 0.1f
        }

        if (Random.nextFloat() < 0.18f) {
            idleDriftYaw = (idleDriftYaw + (Random.nextFloat() * 0.28f - 0.14f)).coerceIn(-0.65f, 0.65f)
            idleDriftPitch = (idleDriftPitch + (Random.nextFloat() * 0.2f - 0.1f)).coerceIn(-0.4f, 0.4f)
        }

        val t = idleTick.toFloat()
        val yawOffset = sin(t * idleYawFreq) * idleYawAmp + idleDriftYaw
        val pitchOffset = cos(t * idlePitchFreq) * idlePitchAmp + idleDriftPitch

        val desiredYaw = baseYaw + yawOffset
        val desiredPitch = (basePitch + pitchOffset).coerceIn(-90f, 90f)
        val yawStep = wrapDegrees(desiredYaw - player.yRot).coerceIn(-1.05f, 1.05f)
        val pitchStep = (desiredPitch - player.xRot).coerceIn(-0.8f, 0.8f)

        player.yRot += yawStep
        player.xRot = (player.xRot + pitchStep).coerceIn(-90f, 90f)
    }

    private fun calculateAngles(
        eyeX: Double,
        eyeY: Double,
        eyeZ: Double,
        targetX: Double,
        targetY: Double,
        targetZ: Double
    ): Pair<Float, Float> {
        val diffX = targetX - eyeX
        val diffY = targetY - eyeY
        val diffZ = targetZ - eyeZ

        val horizontal = sqrt(diffX * diffX + diffZ * diffZ)

        val yaw = Math.toDegrees(atan2(diffZ, diffX)).toFloat() - 90f
        val pitch = (-Math.toDegrees(atan2(diffY, horizontal))).toFloat()

        return yaw to pitch
    }

    private fun wrapDegrees(value: Float): Float {
        var wrapped = value % 360f
        if (wrapped >= 180f) wrapped -= 360f
        if (wrapped < -180f) wrapped += 360f
        return wrapped
    }
}