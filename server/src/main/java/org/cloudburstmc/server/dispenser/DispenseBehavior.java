package org.cloudburstmc.server.dispenser;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.item.ItemStack;

/**
 * @author CreeperFace
 */
public interface DispenseBehavior {

    void dispense(Block block, ItemStack item);

}
