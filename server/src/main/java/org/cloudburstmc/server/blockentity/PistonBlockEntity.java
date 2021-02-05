package org.cloudburstmc.server.blockentity;

import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import com.nukkitx.nbt.NbtType;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.Piston;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.util.AxisAlignedBB;
import org.cloudburstmc.api.util.SimpleAxisAlignedBB;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.level.chunk.CloudChunk;
import org.cloudburstmc.server.math.Direction;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author CreeperFace
 */
public class PistonBlockEntity extends BaseBlockEntity implements Piston {

    private final List<Vector3i> attachedBlocks = new ArrayList<>();
    private final List<Vector3i> breakBlocks = new ArrayList<>();
    public Direction facing;
    public boolean extending = false;
    public boolean powered = false;
    private float progress = 1.0F;
    private float lastProgress = 1.0F;
    private byte state = 1;
    private byte newState = 1;
    private boolean sticky = false;

    public PistonBlockEntity(BlockEntityType<?> type, CloudChunk chunk, Vector3i position) {
        super(type, chunk, position);
    }

    @Override
    public void loadAdditionalData(NbtMap tag) {
        super.loadAdditionalData(tag);

        this.progress = tag.getFloat("Progress", 1.0f);
        this.lastProgress = tag.getFloat("LastProgress", 1.0f);
        this.state = tag.getByte("State", (byte) 1);
        this.newState = tag.getByte("NewState", (byte) 1);
        this.sticky = tag.getBoolean("Sticky");

        List<Integer> attachedBlocks = tag.getList("AttachedBlocks", NbtType.INT);

        for (int i = 0; i < attachedBlocks.size(); i += 3) {
            this.attachedBlocks.add(Vector3i.from(
                    attachedBlocks.get(i),
                    attachedBlocks.get(i + 1),
                    attachedBlocks.get(i + 2)
            ));
        }

        List<Integer> breakBlocks = tag.getList("BreakBlocks", NbtType.INT);

        for (int i = 0; i < breakBlocks.size(); i += 3) {
            this.breakBlocks.add(Vector3i.from(
                    breakBlocks.get(i),
                    breakBlocks.get(i + 1),
                    breakBlocks.get(i + 2)
            ));
        }
    }

    @Override
    public void saveAdditionalData(NbtMapBuilder tag) {
        super.saveAdditionalData(tag);
    }

    private void pushEntities() {
        float lastProgress = this.getExtendedProgress(this.lastProgress);
        float x = lastProgress * this.facing.getXOffset();
        float y = lastProgress * this.facing.getYOffset();
        float z = lastProgress * this.facing.getZOffset();
        AxisAlignedBB bb = new SimpleAxisAlignedBB(x, y, z, x + 1f, y + 1f, z + 1f);
        Set<Entity> entities = this.getLevel().getCollidingEntities(bb);
        if (!entities.isEmpty()) {

        }

    }

    private float getExtendedProgress(float progress) {
        return this.extending ? progress - 1.0F : 1.0F - progress;
    }

    public boolean isValid() {
        return true;
    }

    public float getProgress() {
        return progress;
    }

    public float getLastProgress() {
        return lastProgress;
    }

    public boolean isSticky() {
        return sticky;
    }

    public void setSticky(boolean sticky) {
        this.sticky = sticky;
    }

    @Override
    public boolean isPowered() {
        return powered;
    }

    @Override
    public void setPowered(boolean powered) {
        if (this.powered != powered) {
            this.powered = powered;
            this.setDirty();
        }
    }
}