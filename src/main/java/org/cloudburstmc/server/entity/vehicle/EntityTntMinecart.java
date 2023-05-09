package org.cloudburstmc.server.entity.vehicle;

import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.Explosive;
import org.cloudburstmc.api.entity.vehicle.TntMinecart;
import org.cloudburstmc.api.event.entity.EntityExplosionPrimeEvent;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.level.gamerule.GameRules;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.data.MinecartType;
import org.cloudburstmc.api.util.data.MountType;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.server.level.Explosion;

import java.util.concurrent.ThreadLocalRandom;

import static org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes.FUSE_TIME;
import static org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag.CHARGED;

/**
 * Author: Adam Matthew [larryTheCoder]
 * <p>
 * Nukkit Project.
 */
public class EntityTntMinecart extends EntityAbstractMinecart implements TntMinecart, Explosive {

    public EntityTntMinecart(EntityType<TntMinecart> type, Location location) {
        super(type, location);
    }

    @Override
    public boolean isRideable() {
        return false;
    }

    @Override
    public void initEntity() {
        super.initEntity();

        this.setDisplayBlock(BlockStates.TNT);
        this.setDisplay(true);
        this.data.set(FUSE_TIME, 80);
        this.data.setFlag(CHARGED, false);
    }

    @Override
    public boolean onUpdate(int currentTick) {
        this.timing.startTiming();

        int fuse = this.data.get(FUSE_TIME);

        if (fuse < 80) {
            int tickDiff = currentTick - lastUpdate;

            lastUpdate = currentTick;

            if (fuse <= 5 || fuse % 5 == 0) {
                this.data.set(FUSE_TIME, fuse);
            }

            fuse -= tickDiff;

            if (isAlive() && fuse <= 0) {
                if (this.getLevel().getGameRules().get(GameRules.TNT_EXPLODES)) {
                    this.explode(ThreadLocalRandom.current().nextInt(5));
                }
                this.close();
                return false;
            }
        }

        this.timing.stopTiming();

        return super.onUpdate(currentTick) || fuse < 80;
    }

    @Override
    public void activate(int x, int y, int z, boolean flag) {
        this.getLevel().addLevelSoundEvent(this.getPosition(), SoundEvent.IGNITE);
        this.data.set(FUSE_TIME, 79);
    }

    @Override
    public void explode() {
        explode(0);
    }

    public void explode(double square) {
        double root = Math.sqrt(square);

        if (root > 5.0D) {
            root = 5.0D;
        }

        EntityExplosionPrimeEvent event = new EntityExplosionPrimeEvent(this, (4.0D + ThreadLocalRandom.current().nextDouble() * 1.5D * root));
        server.getEventManager().fire(event);
        if (event.isCancelled()) {
            return;
        }
        Explosion explosion = new Explosion(this.getLevel(), this.getPosition(), event.getForce(), this);
        if (event.isBlockBreaking()) {
            explosion.explodeA();
        }
        explosion.explodeB();
        this.close();
    }

    @Override
    public void dropItem() {
        this.getLevel().dropItem(this.getPosition(), ItemStack.builder().itemType(ItemTypes.TNT_MINECART).build());
    }

    @Override
    public MinecartType getMinecartType() {
        return MinecartType.valueOf(3);
    }

    @Override
    public boolean onInteract(Player player, ItemStack item, Vector3f clickedPos) {
        boolean interact = super.onInteract(player, item, clickedPos);
        if (item.getType() == ItemTypes.FLINT_AND_STEEL || item.getType() == ItemTypes.FIREBALL) {
            this.getLevel().addLevelSoundEvent(this.getPosition(), SoundEvent.IGNITE);
            this.data.set(FUSE_TIME, 79);
            return true;
        }

        return interact;
    }

    @Override
    public boolean mount(Entity entity, MountType mode) {
        return false;
    }
}
