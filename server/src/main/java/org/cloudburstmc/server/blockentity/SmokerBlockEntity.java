package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.Smoker;
import org.cloudburstmc.api.inventory.view.BlockFurnaceView;
import org.cloudburstmc.api.inventory.view.SlotGroupType;
import org.cloudburstmc.api.inventory.view.SlotGroupTypes;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.server.registry.VanillaCookingData;

import static org.cloudburstmc.api.block.BlockTypes.LIT_SMOKER;
import static org.cloudburstmc.api.block.BlockTypes.SMOKER;

/**
 * Block entity implementation for a smoker. Smelts food items at twice the speed of a regular furnace
 * and only accepts {@code SMOKER} / {@code LIT_SMOKER} block types.
 */
public class SmokerBlockEntity extends FurnaceBlockEntity implements Smoker {

    public SmokerBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position);
    }

    @Override
    public SlotGroupType<BlockFurnaceView> getSlotGroupType() {
        return SlotGroupTypes.SMOKER;
    }

    @Override
    protected int standardCookTime() {
        return VanillaCookingData.FAST_COOK_TICKS;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvent.SMOKER_USE;
    }

    @Override
    protected BlockType getLitType() {
        return LIT_SMOKER;
    }

    @Override
    protected BlockType getUnlitType() {
        return SMOKER;
    }
}
