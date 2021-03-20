package org.cloudburstmc.server.entity.vehicle;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.vehicle.HopperMinecart;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.data.MinecartType;
import org.cloudburstmc.server.registry.BlockRegistry;
import org.cloudburstmc.server.registry.CloudItemRegistry;

public class EntityHopperMinecart extends EntityAbstractMinecart implements HopperMinecart {

    public static final int NETWORK_ID = 96;

    public EntityHopperMinecart(EntityType<HopperMinecart> type, Location location) {
        super(type, location);
    }

    @Override
    public void initEntity() {
        super.initEntity();

        this.setDisplayBlock(BlockRegistry.get().getBlock(BlockTypes.HOPPER));
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
        this.getLevel().dropItem(this.getPosition(), CloudItemRegistry.get().getItem(ItemTypes.HOPPER_MINECART));
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
