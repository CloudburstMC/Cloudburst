package org.cloudburstmc.server.blockentity.impl;

import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockStates;
import org.cloudburstmc.server.block.util.BlockStateMetaMappings;
import org.cloudburstmc.server.blockentity.BlockEntity;
import org.cloudburstmc.server.blockentity.BlockEntityType;
import org.cloudburstmc.server.blockentity.MovingBlock;
import org.cloudburstmc.server.level.chunk.Chunk;
import org.cloudburstmc.server.registry.BlockEntityRegistry;
import org.cloudburstmc.server.registry.BlockRegistry;

/**
 * Created by CreeperFace on 11.4.2017.
 */
public class MovingBlockEntity extends BaseBlockEntity implements MovingBlock {

    private BlockState blockState = BlockStates.AIR;
    private BlockState extraBlockState = BlockStates.AIR;
    private BlockEntity blockEntity;
    private Vector3i piston;

    public MovingBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position);
    }

    @Override
    public void loadAdditionalData(NbtMap tag) {
        super.loadAdditionalData(tag);

        BlockRegistry registry = BlockRegistry.get();
        if (tag.containsKey("movingBlockId") && tag.containsKey("movingBlockData")) {
            int id = tag.getByte("movingBlockId") & 0xff;
            int meta = tag.getByte("movingBlockData");

            this.blockState = registry.getBlock(id, meta);
        } else {
            NbtMap blockTag = tag.getCompound("movingBlock");
            int legacyId = registry.getLegacyId(blockTag.getString("name"));
            short meta = blockTag.getShort("val");

            this.blockState = registry.getBlock(legacyId, meta);

            NbtMap extraBlockTag = tag.getCompound("movingBlockExtra");
            int extraId = registry.getLegacyId(extraBlockTag.getString("name", "minecraft:air"));
            short extraData = extraBlockTag.getShort("val");
            this.extraBlockState = registry.getBlock(extraId, extraData);
        }

        tag.listenForCompound("movingEntity", entityTag -> {
            BlockEntityType<?> type = BlockEntityRegistry.get().getBlockEntityType(entityTag.getString("id"));
            this.blockEntity = BlockEntityRegistry.get().newEntity(type, this.getChunk(), this.getPosition());
            this.blockEntity.loadAdditionalData(entityTag);
        });

        this.piston = Vector3i.from(tag.getInt("pistonPosX"), tag.getInt("pistonPosY"), tag.getInt("pistonPosZ"));
    }

    @Override
    public void saveAdditionalData(NbtMapBuilder tag) {
        super.saveAdditionalData(tag);

        tag.putCompound("movingBlock", NbtMap.builder()
                .putString("name", this.blockState.getType().toString())
                .putShort("val", (short) BlockStateMetaMappings.getMetaFromState(this.blockState)) //TODO: check
                .build());

        tag.putCompound("movingBlockExtra", NbtMap.builder()
                .putString("name", this.extraBlockState.getType().toString())
                .putShort("val", (short) BlockStateMetaMappings.getMetaFromState(this.extraBlockState)) //TODO: check
                .build());

        tag.putInt("pistonPosX", this.piston.getX());
        tag.putInt("pistonPosY", this.piston.getY());
        tag.putInt("pistonPosZ", this.piston.getZ());

        if (this.blockEntity != null) {
            tag.putCompound("movingEntity", this.blockEntity.getServerTag());
        }
    }

    public BlockState getMovingBlock() {
        return this.blockState;
    }

    public void setBlock(BlockState blockState) {
        this.blockState = blockState == null ? BlockStates.AIR : blockState;
    }

    public BlockState getExtraBlock() {
        return extraBlockState;
    }

    public void setExtraBlock(BlockState extraBlockState) {
        this.extraBlockState = extraBlockState == null ? BlockStates.AIR : extraBlockState;
    }

    public BlockEntity getBlockEntity() {
        return blockEntity;
    }

    public void setBlockEntity(BlockEntity blockEntity) {
        this.blockEntity = blockEntity;
    }

    public Vector3i getPiston() {
        return piston;
    }

    public void setPiston(Vector3i piston) {
        this.piston = piston;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public boolean isSpawnable() {
        return true;
    }
}
