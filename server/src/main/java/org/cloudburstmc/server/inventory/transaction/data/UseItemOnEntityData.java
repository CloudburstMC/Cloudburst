package org.cloudburstmc.server.inventory.transaction.data;

import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.math.vector.Vector3f;

/**
 * @author CreeperFace
 */
public class UseItemOnEntityData implements TransactionData {

    public long entityRuntimeId;
    public int actionType;
    public int hotbarSlot;
    public ItemStack itemInHand;
    public Vector3f playerPos;
    public Vector3f clickPos;

}
