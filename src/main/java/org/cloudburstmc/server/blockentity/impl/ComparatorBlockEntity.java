package org.cloudburstmc.server.blockentity.impl;

import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.blockentity.BlockEntityType;
import org.cloudburstmc.server.blockentity.Comparator;
import org.cloudburstmc.server.level.chunk.Chunk;

/**
 * @author CreeperFace
 */
public class ComparatorBlockEntity extends BaseBlockEntity implements Comparator {

    private int outputSignal;

    public ComparatorBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position);
    }

    @Override
    public void loadAdditionalData(NbtMap tag) {
        super.loadAdditionalData(tag);

        tag.listenForInt("OutputSignal", this::setOutputSignal);
    }

    @Override
    public void saveAdditionalData(NbtMapBuilder tag) {
        super.saveAdditionalData(tag);

        tag.putInt("OutputSignal", this.getOutputSignal());
    }

    @Override
    public boolean isValid() {
        return this.getBlockState().getType() == BlockTypes.COMPARATOR;
    }

    public int getOutputSignal() {
        return outputSignal;
    }

    public void setOutputSignal(int outputSignal) {
        this.outputSignal = outputSignal;
    }
}
