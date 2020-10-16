package org.cloudburstmc.server.entity.impl.vehicle;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.entity.vehicle.HopperMinecart;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.ItemTypes;
import org.cloudburstmc.server.level.Location;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.data.MinecartType;

public class EntityHopperMinecart extends EntityAbstractMinecart implements HopperMinecart {

    public static final int NETWORK_ID = 96;

    public EntityHopperMinecart(EntityType<HopperMinecart> type, Location location) {
        super(type, location);
    }

    @Override
    public void initEntity() {
        super.initEntity();

        this.setDisplayBlock(BlockState.get(BlockTypes.HOPPER));
        this.setDisplay(true);
    }

    // TODO: 2016/12/18 inventory

    @Override
    public MinecartType getMinecartType() {
        return MinecartType.valueOf(5);
    }

    @Override
    public boolean isRideable() {
        return false;
    }

    @Override
    public void dropItem() {
        this.getLevel().dropItem(this.getPosition(), ItemStack.get(ItemTypes.HOPPER_MINECART));
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
