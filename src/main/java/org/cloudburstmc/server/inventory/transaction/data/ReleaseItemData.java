package org.cloudburstmc.server.inventory.transaction.data;

import lombok.ToString;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.math.vector.Vector3f;

/**
 * @author CreeperFace
 */
@ToString
public class ReleaseItemData implements TransactionData {

    public int actionType;
    public int hotbarSlot;
    public ItemStack itemInHand;
    public Vector3f headRot;
}
