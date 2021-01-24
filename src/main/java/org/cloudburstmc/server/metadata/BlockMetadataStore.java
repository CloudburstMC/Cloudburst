package org.cloudburstmc.server.metadata;

import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.world.World;
import org.cloudburstmc.server.plugin.PluginContainer;

import java.util.List;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class BlockMetadataStore extends MetadataStore { //TODO: fix/remove
    private final World owningWorld;

    public BlockMetadataStore(World owningWorld) {
        this.owningWorld = owningWorld;
    }

    @Override
    protected String disambiguate(Metadatable block, String metadataKey) {
        if (!(block instanceof BlockState)) {
            throw new IllegalArgumentException("Argument must be a Block instance");
        }
//        Vector3i pos = ((BlockState) block).getPosition();
//        return pos.getX() + ":" + pos.getY() + ":" + pos.getZ() + ":" + metadataKey;
        return null;
    }

    @Override
    public List<MetadataValue> getMetadata(Object block, String metadataKey) {
        if (!(block instanceof BlockState)) {
            throw new IllegalArgumentException("Object must be a Block");
        }
//        if (((BlockState) block).getWorld() == this.owningWorld) {
//            return super.getMetadata(block, metadataKey);
//        } else {
//            throw new IllegalStateException("Block does not belong to world " + this.owningWorld.getName());
//        }
        return null;
    }

    @Override
    public boolean hasMetadata(Object block, String metadataKey) {
        if (!(block instanceof BlockState)) {
            throw new IllegalArgumentException("Object must be a Block");
        }
//        if (((BlockState) block).getWorld() == this.owningWorld) {
//            return super.hasMetadata(block, metadataKey);
//        } else {
//            throw new IllegalStateException("Block does not belong to world " + this.owningWorld.getName());
//        }
        return false;
    }

    @Override
    public void removeMetadata(Object block, String metadataKey, PluginContainer owningPlugin) {
        if (!(block instanceof BlockState)) {
            throw new IllegalArgumentException("Object must be a Block");
        }
//        if (((BlockState) block).getWorld() == this.owningWorld) {
//            super.removeMetadata(block, metadataKey, owningPlugin);
//        } else {
//            throw new IllegalStateException("Block does not belong to world " + this.owningWorld.getName());
//        }
    }

    @Override
    public void setMetadata(Object block, String metadataKey, MetadataValue newMetadataValue) {
        if (!(block instanceof BlockState)) {
            throw new IllegalArgumentException("Object must be a Block");
        }
//        if (((BlockState) block).getWorld() == this.owningWorld) {
//            super.setMetadata(block, metadataKey, newMetadataValue);
//        } else {
//            throw new IllegalStateException("Block does not belong to world " + this.owningWorld.getName());
//        }
    }
}
