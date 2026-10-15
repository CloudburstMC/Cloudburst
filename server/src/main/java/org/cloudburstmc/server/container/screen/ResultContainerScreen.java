package org.cloudburstmc.server.container.screen;

import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;

import java.util.function.BiConsumer;

public interface ResultContainerScreen {

    boolean isResultSlot(ContainerSlotType type, int slot);

    boolean isInputSlot(ContainerSlotType type);

    default boolean isInvalidResultCount(ItemStack result, int count) {
        return count <= 0 || count != result.getCount();
    }

    ItemStack takeResult(ItemStack expected, int count);

    default boolean canMergeResult() {
        return true;
    }

    void trackResultSlots(BiConsumer<ContainerSlotType, Integer> tracker);
}
