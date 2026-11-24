package org.cloudburstmc.server.entity.vehicle;

import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.vehicle.ChestMinecart;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.util.data.MinecartType;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType;

public class EntityChestMinecart extends EntityContainerMinecart implements ChestMinecart {

    public EntityChestMinecart(EntityType<ChestMinecart> type, Location location) {
        super(type, location, 27, ContainerType.MINECART_CHEST, ContainerType.CONTAINER);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setDisplayBlock(BlockTypes.CHEST.getDefaultState());
        this.setDisplay(true);
    }

    @Override
    public MinecartType getMinecartType() {
        return MinecartType.MINECART_CHEST;
    }

    @Override
    protected ItemType getDropItemType() {
        return ItemTypes.CHEST_MINECART;
    }
}
