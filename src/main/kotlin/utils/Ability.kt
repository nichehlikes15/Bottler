package utils

import net.minecraft.network.chat.Component
import utils.rightClick
import java.util.regex.Pattern
import handlers.Typo.modMessage

object Ability {

    private var enabled = false

    private var waitingForCooldown = false
    private var nextAvailableTime = 0L

    private val cooldownPattern =
        Pattern.compile("Your Pickaxe ability is on cooldown for (\\d+)s")

    fun start() {
        enabled = true
        attemptUse()
    }

    fun stop() {
        enabled = false
        waitingForCooldown = false
        nextAvailableTime = 0L
    }

    fun onChat(component: Component) {
        if (!enabled) return

        val message = component.string

        if (message.contains("Mining Speed Boost is now available!")) {
            attemptUse()
            return
        }

        if (message.contains("You used your Mining Speed Boost Pickaxe Ability!")) {
            waitingForCooldown = true
            nextAvailableTime = System.currentTimeMillis() + 120_000
            return
        }

        val matcher = cooldownPattern.matcher(message)
        if (matcher.find()) {
            val seconds = matcher.group(1).toLong()
            waitingForCooldown = true
            nextAvailableTime = System.currentTimeMillis() + seconds * 1000
        }
    }

    fun tick() {
        if (!enabled) return

        if (waitingForCooldown && System.currentTimeMillis() >= nextAvailableTime) {
            waitingForCooldown = false
        }
    }

    private fun attemptUse() {
        modMessage("Attempting to use Mining Speed Boost")
        if (waitingForCooldown) return
        rightClick()
    }
}