package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.passive.SulfurCube;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.server.entity.EntityAbstractCubeMob;

import static org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag.BABY;

public class EntitySulfurCube extends EntityAbstractCubeMob implements SulfurCube {

    public EntitySulfurCube(EntityType<SulfurCube> type, Location location) {
        super(type, location, 2);
    }

    @Override
    public ItemStack getBaseBucketItem() {
        return ItemStack.from(ItemTypes.SULFUR_CUBE_BUCKET);
    }

    @Override
    protected int getMaximumSize() {
        return 2;
    }

    @Override
    protected int getBaseHealthForSize(int size) {
        return size == 1 ? 4 : 9;
    }

    @Override
    protected float getUnitDimension() {
        return 0.49f;
    }

    @Override
    protected void onSizeChanged() {
        this.data.setFlag(BABY, this.getSize() == 1);
    }

    @Override
    protected float getNetworkScale() {
        return this.getScale();
    }

    @Override
    protected String getDefaultName() {
        return "Sulfur Cube";
    }
}
