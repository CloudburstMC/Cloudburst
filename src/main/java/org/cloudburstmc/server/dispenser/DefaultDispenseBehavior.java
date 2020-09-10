package org.cloudburstmc.server.dispenser;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.math.Direction;

/**
 * @author CreeperFace
 */
public class DefaultDispenseBehavior implements DispenseBehavior {

    @Override
    public void dispense(Block block, ItemStack stack) {

    }

    private int getParticleMetadataForFace(Direction face) {
        return face.getXOffset() + 1 + (face.getZOffset() + 1) * 3;
    }
}
