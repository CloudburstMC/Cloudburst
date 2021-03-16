package org.cloudburstmc.server.dispenser;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.Direction;

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
