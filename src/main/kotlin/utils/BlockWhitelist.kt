package utils

import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks

object BlockWhitelist {

    enum class Group {
        GROUP_1,
        GROUP_2
    }

    data class Entry(val block: Block, val group: Group)

    private fun g1(block: Block) = Entry(block, Group.GROUP_1)
    private fun g2(block: Block) = Entry(block, Group.GROUP_2)

    val prioritizedBlocks: List<Entry> = listOf(

        // =========================
        // GROUP 1 (HIGH PRIORITY)
        // =========================
        g1(Blocks.COAL_BLOCK),
        g1(Blocks.DIAMOND_BLOCK),
        g1(Blocks.EMERALD_BLOCK),
        g1(Blocks.REDSTONE_BLOCK),
        g1(Blocks.IRON_BLOCK),
        g1(Blocks.GOLD_BLOCK),
        g1(Blocks.LAPIS_BLOCK),

        g1(Blocks.GRAY_WOOL),
        g1(Blocks.CYAN_TERRACOTTA),
        g1(Blocks.POLISHED_DIORITE), //Titanium
        g1(Blocks.PRISMARINE_BRICKS),
        g2(Blocks.PRISMARINE),
        g2(Blocks.DARK_PRISMARINE),

        // =========================
        // GROUP 2 (FALLBACK)
        // =========================
        g2(Blocks.LIGHT_BLUE_WOOL)
    )
}