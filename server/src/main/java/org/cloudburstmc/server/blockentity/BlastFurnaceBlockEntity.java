package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.blockentity.BlastFurnace;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.inventory.view.BlockFurnaceView;
import org.cloudburstmc.api.inventory.view.SlotGroupType;
import org.cloudburstmc.api.inventory.view.SlotGroupTypes;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.server.registry.VanillaCookingData;

import static org.cloudburstmc.api.block.BlockTypes.BLAST_FURNACE;
import static org.cloudburstmc.api.block.BlockTypes.LIT_BLAST_FURNACE;

/**
 * Block entity implementation for a blast furnace. Smelts items at twice the speed of a regular furnace.
 */
public class BlastFurnaceBlockEntity extends FurnaceBlockEntity implements BlastFurnace {

    public BlastFurnaceBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position);
    }

    @Override
    public SlotGroupType<BlockFurnaceView> getSlotGroupType() {
        return SlotGroupTypes.BLAST_FURNACE;
    }

    @Override
    protected int standardCookTime() {
        return VanillaCookingData.FAST_COOK_TICKS;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvent.BLAST_FURNACE_USE;
    }

    @Override
    protected BlockType getLitType() {
        return LIT_BLAST_FURNACE;
    }

    @Override
    protected BlockType getUnlitType() {
        return BLAST_FURNACE;
    }
}
