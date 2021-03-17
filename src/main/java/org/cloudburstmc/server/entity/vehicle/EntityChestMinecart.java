package org.cloudburstmc.server.entity.vehicle;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.vehicle.ChestMinecart;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.data.MinecartType;
import org.cloudburstmc.server.item.ItemTypes;
import org.cloudburstmc.server.registry.BlockRegistry;
import org.cloudburstmc.server.registry.CloudItemRegistry;

/**
 * Created by Snake1999 on 2016/1/30.
 * Package cn.nukkit.entity.item in project Nukkit.
 */
public class EntityChestMinecart extends EntityAbstractMinecart implements ChestMinecart {

    public EntityChestMinecart(EntityType<ChestMinecart> type, Location location) {
        super(type, location);
    }

    @Override
    public void initEntity() {
        super.initEntity();

        this.setDisplayBlock(BlockRegistry.get().getBlock(BlockTypes.CHEST));
        this.setDisplay(true);
    }

    // TODO: 2016/1/30 inventory

    @Override
    public MinecartType getMinecartType() {
        return MinecartType.valueOf(1);
    }

    @Override
    public boolean isRideable() {
        return false;
    }

    @Override
    public void dropItem() {
        this.getLevel().dropItem(this.getPosition(), CloudItemRegistry.get().getItem(ItemTypes.CHEST_MINECART));
    }

    @Override
    protected void activate(int x, int y, int z, boolean flag) {

    }

    @Override
    public boolean mount(Entity entity) {
        return false;
    }

    @Override
    public boolean onInteract(Player p, ItemStack item, Vector3f clickedPos) {
        return false;
    }
}
