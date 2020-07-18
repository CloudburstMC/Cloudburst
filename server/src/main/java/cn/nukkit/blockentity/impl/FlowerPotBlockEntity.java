package cn.nukkit.blockentity.impl;

import cn.nukkit.block.Block;
import cn.nukkit.block.BlockIds;
import cn.nukkit.blockentity.BlockEntityType;
import cn.nukkit.blockentity.FlowerPot;
import cn.nukkit.level.chunk.Chunk;
import cn.nukkit.registry.BlockRegistry;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;

import static cn.nukkit.block.BlockIds.AIR;

/**
 * Created by Snake1999 on 2016/2/4.
 * Package cn.nukkit.blockentity in project Nukkit.
 */
public class FlowerPotBlockEntity extends BaseBlockEntity implements FlowerPot {

    private Block plant = Block.get(AIR);

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
                .putString("name", plant.getId().toString())
                .putShort("val", (short) plant.getMeta())
                .build());
    }

    @Override
    public boolean isValid() {
        return this.getBlock().getId() == BlockIds.FLOWER_POT;
    }

    public Block getPlant() {
        return plant.clone();
    }

    public void setPlant(Block block) {
        this.plant = block == null ? Block.get(AIR) : block;
    }

    @Override
    public boolean isSpawnable() {
        return true;
    }
}
