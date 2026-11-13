package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.component.BlockLootHandler;
import org.cloudburstmc.api.block.component.ComplexBlockHandler;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.blockentity.FurnaceBlockEntity;

import java.util.List;

@UtilityClass
public class FurnaceBlockHandlers {

    public static final BlockLootHandler GET_LOOT = (block, context) -> {
        List<ItemStack> drops = DefaultBlockHandlers.GET_LOOT.execute(block, context);
        if (block.getLevel().getBlockEntity(block.getPosition()) instanceof FurnaceBlockEntity furnace) {
            String name = furnace.getCustomName();
            if (name != null) {
                return drops.stream().map(item -> item.toBuilder().setData(ItemDataComponents.CUSTOM_NAME, name).build()).toList();
            }
        }
        return drops;
    };

    public static final ComplexBlockHandler ON_REMOVE = block -> {
        if (block.getLevel().getBlockEntity(block.getPosition()) instanceof FurnaceBlockEntity furnace && !furnace.isValid()) {
            furnace.onBreak();
        }
    };
}
