package cn.nukkit.blockentity.impl;

import cn.nukkit.block.BlockRedstoneComparator;
import cn.nukkit.blockentity.BlockEntityType;
import cn.nukkit.blockentity.Comparator;
import cn.nukkit.level.chunk.Chunk;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;

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
        return this.getBlock() instanceof BlockRedstoneComparator;
    }

    public int getOutputSignal() {
        return outputSignal;
    }

    public void setOutputSignal(int outputSignal) {
        this.outputSignal = outputSignal;
    }
}
