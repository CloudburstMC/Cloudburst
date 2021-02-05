package org.cloudburstmc.server.inventory.transaction.data;

import lombok.ToString;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.math.Direction;

/**
 * @author CreeperFace
 */
@ToString
public class UseItemData implements TransactionData {

    public int actionType;
    public Vector3i blockPos;
    public Direction face;
    public int hotbarSlot;
    public ItemStack itemInHand;
    public Vector3f playerPos;
    public Vector3f clickPos;
    public int blockRuntimeId;
}
