package org.cloudburstmc.server.inventory.transaction.data;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.math.vector.Vector3i;
import lombok.ToString;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.math.BlockFace;

/**
 * @author CreeperFace
 */
@ToString
public class UseItemData implements TransactionData {

    public int actionType;
    public Vector3i blockPos;
    public BlockFace face;
    public int hotbarSlot;
    public Item itemInHand;
    public Vector3f playerPos;
    public Vector3f clickPos;
    public int blockRuntimeId;
}
