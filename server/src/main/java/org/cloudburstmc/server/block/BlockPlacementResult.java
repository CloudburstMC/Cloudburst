package org.cloudburstmc.server.block;

import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.math.vector.Vector3i;

public record BlockPlacementResult(Vector3i position, ItemStack remainingItem) {
}
