package org.cloudburstmc.server.inventory.transaction.data;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.math.vector.Vector3i;
import lombok.ToString;
import org.cloudburstmc.server.item.ItemStack;
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
