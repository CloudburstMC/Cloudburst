package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.Cauldron;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.api.util.data.BlockColor;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;

/**
 * author: CreeperFace
 * Nukkit Project
 */
public class CauldronBlockEntity extends BaseBlockEntity implements Cauldron {

    private short potionType;
    private short potionId;
    private boolean splash;
    private BlockColor customColor = BlockColor.WHITE_BLOCK_COLOR;

    public CauldronBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position);
    }

    @Override
    public void loadAdditionalData(NbtMap tag) {
        super.loadAdditionalData(tag);

        // TODO: "Items" tag 0-10 slots
        tag.listenForShort("PotionType", this::setPotionType);
        tag.listenForShort("PotionId", this::setPotionId);
        tag.listenForBoolean("IsSplash", this::setSplash);
        tag.listenForInt("CustomColor", this::setCustomColor);
    }

    @Override
    public void saveAdditionalData(NbtMapBuilder tag) {
        super.saveAdditionalData(tag);

        tag.putShort("PotionType", this.getPotionType());
        tag.putShort("PotionId", this.getPotionId());
        tag.putBoolean("IsSplash", this.isSplash());
        if (this.customColor != null) {
            tag.putInt("CustomColor", this.customColor.getRGB());
        }
    }

    @Override
    public short getPotionId() {
        return potionId;
    }

    @Override
    public void setPotionId(int potionId) {
        this.potionId = (short) potionId;
    }

    @Override
    public short getPotionType() {
        return potionType;
    }

    @Override
    public void setPotionType(int potionType) {
        this.potionType = (short) potionType;
    }

    @Override
    public boolean isSplash() {
        return splash;
    }

    @Override
    public void setSplash(boolean splash) {
        this.splash = splash;
    }

    @Override
    public BlockColor getCustomColor() {
        return this.customColor;
    }

    @Override
    public void setCustomColor(BlockColor customColor) {
        this.customColor = customColor;
    }

    private void setCustomColor(int customColor) {
       // this.customColor = new BlockColor(customColor);
        //TODO - need to convert BlockColer back to class instead of enum? or just store/return an RGB int?

    }

    @Override
    public boolean hasCustomColor() {
        return this.customColor != null;
    }

    @Override
    public boolean isValid() {
        return getBlockState().getType() == BlockTypes.CAULDRON;
    }

    @Override
    public boolean isSpawnable() {
        return true;
    }
}
