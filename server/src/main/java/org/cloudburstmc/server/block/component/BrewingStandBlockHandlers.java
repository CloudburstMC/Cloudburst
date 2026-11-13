package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.component.BlockLootHandler;
import org.cloudburstmc.api.block.component.ComplexBlockHandler;
import org.cloudburstmc.api.blockentity.BlockEntityTypes;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.blockentity.BrewingStandBlockEntity;
import org.cloudburstmc.server.registry.CloudBlockEntityRegistry;

import java.util.List;

@UtilityClass
public class BrewingStandBlockHandlers {

    public static final ComplexBlockHandler ON_ADDED = block -> {
        if (block.getLevel().getBlockEntity(block.getPosition()) == null) {
            CloudBlockEntityRegistry.get().newEntity(BlockEntityTypes.BREWING_STAND, block);
        }
    };

    public static final ComplexBlockHandler ON_REMOVE = block -> {
        if (block.getLevel().getBlockEntity(block.getPosition()) instanceof BrewingStandBlockEntity stand && !stand.isValid()) {
            stand.onBreak();
        }
    };

    public static final BlockLootHandler GET_LOOT = (block, context) -> {
        List<ItemStack> drops = DefaultBlockHandlers.GET_LOOT.execute(block, context);
        if (block.getLevel().getBlockEntity(block.getPosition()) instanceof BrewingStandBlockEntity stand) {
            String name = stand.getCustomName();
            if (name != null) {
                return drops.stream().map(item -> item.toBuilder()
                        .setData(ItemDataComponents.CUSTOM_NAME, name).build()).toList();
            }
        }
        return drops;
    };
}
