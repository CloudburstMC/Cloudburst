package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.FlowerPot;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.block.BlockPalette;
import org.cloudburstmc.server.registry.CloudBlockRegistry;

import static org.cloudburstmc.api.block.BlockTypes.AIR;

/**
 * Created by Snake1999 on 2016/2/4.
 * Package cn.nukkit.blockentity in project Nukkit.
 */
public class FlowerPotBlockEntity extends BaseBlockEntity implements FlowerPot {

    private BlockState plant = BlockStates.AIR;

    public FlowerPotBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position);
    }

    @Override
    public void loadAdditionalData(NbtMap tag) {
        super.loadAdditionalData(tag);

        CloudBlockRegistry registry = CloudBlockRegistry.REGISTRY;

        if (tag.containsKey("item") && tag.containsKey("mData")) {
            short id = tag.getShort("item");
            int meta = tag.getInt("mData");

            this.plant = registry.getBlock(id, meta);
        } else if (tag.containsKey("PlantBlock", NbtType.COMPOUND)) {
            NbtMap plantTag = tag.getCompound("PlantBlock");

            this.plant = CloudBlockRegistry.REGISTRY.getBlock(plantTag);
        }
    }

    @Override
    public void saveAdditionalData(NbtMapBuilder tag) {
        super.saveAdditionalData(tag);

        if (this.plant != BlockStates.AIR) {
            tag.putCompound("PlantBlock", BlockPalette.INSTANCE.getSerialized(this.plant));
        }
    }

    @Override
    public boolean isValid() {
        return this.getBlockState().getType() == BlockTypes.FLOWER_POT;
    }

    public BlockState getPlant() {
        return plant;
    }

    public void setPlant(BlockState blockState) {
        this.plant = blockState == null ? CloudBlockRegistry.REGISTRY.getBlock(AIR) : blockState;
    }

    @Override
    public boolean isSpawnable() {
        return true;
    }
}
