package org.cloudburstmc.server.inventory.transaction.data;

import com.nukkitx.math.vector.Vector3f;
import lombok.ToString;
import org.cloudburstmc.server.item.ItemStack;

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
