package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockTags;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.block.component.BlockLootHandler;
import org.cloudburstmc.api.block.component.ComplexBlockHandler;
import org.cloudburstmc.api.blockentity.BlockEntityTypes;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.blockentity.EnchantingTableBlockEntity;
import org.cloudburstmc.server.enchantment.TableEnchantments;
import org.cloudburstmc.server.registry.CloudBlockEntityRegistry;

import java.util.List;

@UtilityClass
public class EnchantingTableBlockHandlers {

    public static final ComplexBlockHandler ON_ADDED = block -> {
        if (block.getLevel().getBlockEntity(block.getPosition()) == null) {
            CloudBlockEntityRegistry.get().newEntity(BlockEntityTypes.ENCHANTING_TABLE, block);
        }
    };

    public static final BlockLootHandler GET_LOOT = (block, context) -> {
        List<ItemStack> drops = DefaultBlockHandlers.GET_LOOT.execute(block, context);
        if (block.getLevel().getBlockEntity(block.getPosition()) instanceof EnchantingTableBlockEntity table) {
            String name = table.getCustomName();
            if (name != null) {
                return drops.stream().map(item -> item.toBuilder()
                        .setData(ItemDataComponents.CUSTOM_NAME, name).build()).toList();
            }
        }
        return drops;
    };

    public static int countBookshelves(Level level, Vector3i table) {
        int power = 0;
        for (int y = 0; y <= 1; y++) {
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    if ((Math.abs(x) == 2 || Math.abs(z) == 2)
                            && hasBookshelf(level, table, x, y, z) && ++power == TableEnchantments.MAX_BOOKSHELVES) {
                        return power;
                    }
                }
            }
        }
        return power;
    }

    private static boolean hasBookshelf(Level level, Vector3i table, int x, int y, int z) {
        Block shelf = level.getLoadedBlock(table.add(x, y, z));
        if (shelf == null || shelf.getState().getType() != BlockTypes.BOOKSHELF) {
            return false;
        }
        Block gap = level.getLoadedBlock(table.add(x / 2, y, z / 2));
        return gap != null && gap.getState().is(BlockTags.REPLACEABLE) && gap.getLiquid().isEmpty();
    }
}
