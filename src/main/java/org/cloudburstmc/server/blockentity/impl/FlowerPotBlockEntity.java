package org.cloudburstmc.server.blockentity.impl;

import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.block.util.BlockStateMetaMappings;
import org.cloudburstmc.server.blockentity.BlockEntityType;
import org.cloudburstmc.server.blockentity.FlowerPot;
import org.cloudburstmc.server.level.chunk.Chunk;
import org.cloudburstmc.server.registry.BlockRegistry;

import static org.cloudburstmc.server.block.BlockTypes.AIR;

/**
 * Created by Snake1999 on 2016/2/4.
 * Package cn.nukkit.blockentity in project Nukkit.
 */
public class FlowerPotBlockEntity extends BaseBlockEntity implements FlowerPot {

    private BlockState plant = BlockState.get(AIR);

    public FlowerPotBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position);
    }

    @Override
    public void loadAdditionalData(NbtMap tag) {
        super.loadAdditionalData(tag);

        BlockRegistry registry = BlockRegistry.get();

        if (tag.containsKey("item") && tag.containsKey("mData")) {
            short id = tag.getShort("item");
            int meta = tag.getInt("mData");

            this.plant = registry.getBlock(id, meta);
        } else {
            NbtMap plantTag = tag.getCompound("PlantBlock");
            int legacyId = registry.getLegacyId(plantTag.getString("name"));
            short meta = plantTag.getShort("val");

            this.plant = registry.getBlock(legacyId, meta);
        }
    }

    @Override
    public void saveAdditionalData(NbtMapBuilder tag) {
        super.saveAdditionalData(tag);

        tag.putCompound("PlantBlock", NbtMap.builder()
                .putString("name", plant.getType().toString())
                .putShort("val", (short) BlockStateMetaMappings.getMetaFromState(plant)) //TODO: check
                .build());
    }

    @Override
    public boolean isValid() {
        return this.getBlockState().getType() == BlockTypes.FLOWER_POT;
    }

    public BlockState getPlant() {
        return plant;
    }

    public void setPlant(BlockState blockState) {
        this.plant = blockState == null ? BlockState.get(AIR) : blockState;
    }

    @Override
    public boolean isSpawnable() {
        return true;
    }
}
