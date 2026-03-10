package org.cloudburstmc.api.block.component;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.item.ItemStack;

/**
 * Produces the {@link ItemStack} placed into the player's hand when they
 * middle-click (pick-block) this block.
 *
 * <p>The default implementation returns {@link ItemStack#from(org.cloudburstmc.api.block.BlockState)},
 * which is correct for blocks whose item identity is fully encoded in their
 * block state. Blocks whose item variant is stored in a block entity (e.g.
 * beds, banners) should register a custom handler that reads the entity data
 * and encodes it into the appropriate item key.</p>
 */
@FunctionalInterface
public interface PickBlockHandler {

    ItemStack execute(Block block);
}
