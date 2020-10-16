package org.cloudburstmc.server.entity.impl.vehicle;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.SoundEvent;
import com.nukkitx.protocol.bedrock.data.entity.EntityLinkData;
import org.cloudburstmc.server.block.BlockStates;
import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.entity.EntityExplosive;
import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.entity.vehicle.TntMinecart;
import org.cloudburstmc.server.event.entity.EntityExplosionPrimeEvent;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.ItemTypes;
import org.cloudburstmc.server.level.Explosion;
import org.cloudburstmc.server.level.Location;
import org.cloudburstmc.server.level.gamerule.GameRules;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.data.MinecartType;

import java.util.concurrent.ThreadLocalRandom;

import static com.nukkitx.protocol.bedrock.data.entity.EntityData.FUSE_LENGTH;
import static com.nukkitx.protocol.bedrock.data.entity.EntityFlag.CHARGED;

/**
 * Author: Adam Matthew [larryTheCoder]
 * <p>
 * Nukkit Project.
 */
public class EntityTntMinecart extends EntityAbstractMinecart implements TntMinecart, EntityExplosive {

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
        this.data.setInt(FUSE_LENGTH, 80);
        this.data.setFlag(CHARGED, false);
    }

    @Override
    public boolean onUpdate(int currentTick) {
        this.timing.startTiming();

        int fuse = this.data.getInt(FUSE_LENGTH);

        if (fuse < 80) {
            int tickDiff = currentTick - lastUpdate;

            lastUpdate = currentTick;

            if (fuse <= 5 || fuse % 5 == 0) {
                this.data.setInt(FUSE_LENGTH, fuse);
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
        this.data.setInt(FUSE_LENGTH, 79);
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
        this.getLevel().dropItem(this.getPosition(), ItemStack.get(ItemTypes.TNT_MINECART));
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
            this.data.setInt(FUSE_LENGTH, 79);
            return true;
        }

        return interact;
    }

    @Override
    public boolean mount(Entity entity, EntityLinkData.Type mode) {
        return false;
    }
}
