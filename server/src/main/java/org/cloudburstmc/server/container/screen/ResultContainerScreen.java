package org.cloudburstmc.server.container.screen;

import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.jspecify.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;

public interface ResultContainerScreen {

    boolean isResultSlot(ContainerSlotType type, int slot);

    boolean isInputSlot(ContainerSlotType type);

    default boolean isInvalidResultCount(ItemStack result, int count) {
        return count <= 0 || count != result.getCount();
    }

    @Nullable ResultTake prepareTake(ItemStack expected, int count);

    default boolean canMergeResult() {
        return true;
    }

    void trackResultSlots(BiConsumer<ContainerSlotType, Integer> tracker);

    record ResultTake(ItemStack item, Runnable writeSlots, Runnable afterCommit) {
    }

    record ResultQuote(ItemStack item, IntPredicate invalidCount, IntFunction<@Nullable ResultTake> prepareTake) {
    }
}
