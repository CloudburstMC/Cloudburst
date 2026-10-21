package org.cloudburstmc.server.item;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.enchantment.Enchantment;
import org.cloudburstmc.api.enchantment.EnchantmentTypes;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class IceMiningTest {

    private static final List<BlockState> ICE_VARIANTS = List.of(
            BlockTestRegistry.get().getBlock(BlockTypes.ICE),
            BlockTestRegistry.get().getBlock(BlockTypes.PACKED_ICE),
            BlockTestRegistry.get().getBlock(BlockTypes.BLUE_ICE)
    );

    @Test
    void netheritePickaxeUsesItsMaterialSpeedForMineableIceWithOrWithoutSilkTouch() {
        Player player = InterfaceProxy.create(Player.class, Map.of("isOnGround", true));
        ItemStack ordinary = ItemStack.from(ItemTypes.NETHERITE_PICKAXE);
        ItemStack silk = ItemStack.builder().itemType(ItemTypes.NETHERITE_PICKAXE)
                .setData(ItemDataComponents.ENCHANTMENTS, Map.of(EnchantmentTypes.SILK_TOUCH, new Enchantment(EnchantmentTypes.SILK_TOUCH, 1)))
                .build();

        for (BlockState ice : ICE_VARIANTS) {
            assertEquals(9f, ToolUtils.getMiningSpeed(ordinary, ice));
            assertEquals(ToolUtils.getDestroyProgress(player, ordinary, ice), ToolUtils.getDestroyProgress(player, silk, ice));
            assertEquals(ice.getType() == BlockTypes.BLUE_ICE ? 10 : 2, ToolUtils.getBreakTicks(player, silk, ice));
            assertTrue(ToolUtils.getBreakTicks(player, ItemStack.EMPTY, ice) > ToolUtils.getBreakTicks(player, silk, ice));
        }

        assertEquals(1f, ToolUtils.getMiningSpeed(silk, BlockTypes.FROSTED_ICE.getDefaultState()));
    }
}
