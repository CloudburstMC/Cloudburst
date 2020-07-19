package org.cloudburstmc.server.metadata;

import com.nukkitx.math.vector.Vector3i;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.plugin.Plugin;

import java.util.List;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class BlockMetadataStore extends MetadataStore {
    private final Level owningLevel;

    public BlockMetadataStore(Level owningLevel) {
        this.owningLevel = owningLevel;
    }

    @Override
    protected String disambiguate(Metadatable block, String metadataKey) {
        if (!(block instanceof BlockState)) {
            throw new IllegalArgumentException("Argument must be a Block instance");
        }
        Vector3i pos = ((BlockState) block).getPosition();
        return pos.getX() + ":" + pos.getY() + ":" + pos.getZ() + ":" + metadataKey;
    }

    @Override
    public List<MetadataValue> getMetadata(Object block, String metadataKey) {
        if (!(block instanceof BlockState)) {
            throw new IllegalArgumentException("Object must be a Block");
        }
        if (((BlockState) block).getLevel() == this.owningLevel) {
            return super.getMetadata(block, metadataKey);
        } else {
            throw new IllegalStateException("Block does not belong to world " + this.owningLevel.getName());
        }
    }

    @Override
    public boolean hasMetadata(Object block, String metadataKey) {
        if (!(block instanceof BlockState)) {
            throw new IllegalArgumentException("Object must be a Block");
        }
        if (((BlockState) block).getLevel() == this.owningLevel) {
            return super.hasMetadata(block, metadataKey);
        } else {
            throw new IllegalStateException("Block does not belong to world " + this.owningLevel.getName());
        }
    }

    @Override
    public void removeMetadata(Object block, String metadataKey, Plugin owningPlugin) {
        if (!(block instanceof BlockState)) {
            throw new IllegalArgumentException("Object must be a Block");
        }
        if (((BlockState) block).getLevel() == this.owningLevel) {
            super.removeMetadata(block, metadataKey, owningPlugin);
        } else {
            throw new IllegalStateException("Block does not belong to world " + this.owningLevel.getName());
        }
    }

    @Override
    public void setMetadata(Object block, String metadataKey, MetadataValue newMetadataValue) {
        if (!(block instanceof BlockState)) {
            throw new IllegalArgumentException("Object must be a Block");
        }
        if (((BlockState) block).getLevel() == this.owningLevel) {
            super.setMetadata(block, metadataKey, newMetadataValue);
        } else {
            throw new IllegalStateException("Block does not belong to world " + this.owningLevel.getName());
        }
    }
}
