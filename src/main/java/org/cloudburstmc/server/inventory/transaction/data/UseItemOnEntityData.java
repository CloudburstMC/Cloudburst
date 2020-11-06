package org.cloudburstmc.server.inventory.transaction.data;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.item.ItemStack;

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
