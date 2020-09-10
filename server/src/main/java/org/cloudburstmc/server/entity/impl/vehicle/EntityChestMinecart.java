package org.cloudburstmc.server.entity.impl.vehicle;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.BlockIds;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.entity.vehicle.ChestMinecart;
import org.cloudburstmc.server.item.ItemIds;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.level.Location;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.data.MinecartType;

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

        this.setDisplayBlock(BlockState.get(BlockIds.CHEST));
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
        this.getLevel().dropItem(this.getPosition(), ItemStack.get(ItemIds.CHEST_MINECART));
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
