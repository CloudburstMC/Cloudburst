package org.cloudburstmc.server.dispenser;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.item.ItemStack;

/**
 * @author CreeperFace
 */
public interface DispenseBehavior {

    void dispense(Block block, ItemStack item);

}
