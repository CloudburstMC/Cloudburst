package org.cloudburstmc.server.item;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.item.ItemComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.Tool;
import org.cloudburstmc.server.registry.CloudItemRegistry;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ToolUtils {

    public static @Nullable Tool getTool(ItemStack item) {
        if (item.isEmpty()) {
            return null;
        }

        return CloudItemRegistry.get().requireComponent(item.getType(), ItemComponents.GET_TOOL).execute(item);
    }

    public static float getMiningSpeed(ItemStack item, BlockState block) {
        Tool tool = getTool(item);
        if (tool == null) {
            return 1;
        }

        return tool.getMiningSpeed(block.getType());
    }

    public static boolean isCorrectForDrops(ItemStack item, BlockState block) {
        if (!block.requiresCorrectToolForDrops()) {
            return true;
        }

        Tool tool = getTool(item);
        if (tool == null) {
            return false;
        }

        return tool.isCorrectForDrops(block.getType());
    }

    public static boolean canDestroyInCreative(ItemStack item) {
        Tool tool = getTool(item);
        return tool == null || tool.canDestroyBlocksInCreative();
    }
}
