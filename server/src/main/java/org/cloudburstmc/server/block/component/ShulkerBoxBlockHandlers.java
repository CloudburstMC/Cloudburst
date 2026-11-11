package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.component.BlockLootHandler;
import org.cloudburstmc.server.blockentity.ShulkerBoxBlockEntity;

import java.util.List;

@UtilityClass
public class ShulkerBoxBlockHandlers {

    public static final BlockLootHandler GET_LOOT = (block, context) -> {
        if (block.getLevel().getBlockEntity(block.getPosition()) instanceof ShulkerBoxBlockEntity box) {
            return List.of(box.asItem());
        }
        return DefaultBlockHandlers.GET_LOOT.execute(block, context);
    };

    public static final BlockLootHandler GET_CREATIVE_LOOT = (block, _) -> {
        if (block.getLevel().getBlockEntity(block.getPosition()) instanceof ShulkerBoxBlockEntity box && !box.getContainer().isEmpty()) {
            return List.of(box.asItem());
        }
        return List.of();
    };
}
