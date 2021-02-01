package org.cloudburstmc.server.dispenser;

import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.block.Block;

/**
 * @author CreeperFace
 */
public interface DispenseBehavior {

    void dispense(Block block, ItemStack item);

}
