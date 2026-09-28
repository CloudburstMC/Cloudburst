package org.cloudburstmc.server.entity.vehicle;

import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.Explosive;
import org.cloudburstmc.api.entity.Projectile;
import org.cloudburstmc.api.entity.damage.DamageTypeTags;
import org.cloudburstmc.api.entity.vehicle.TntMinecart;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.ExplosionBlockInteraction;
import org.cloudburstmc.api.level.ExplosionSettings;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.level.gamerule.GameRules;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.data.MinecartType;
import org.cloudburstmc.api.util.data.MountType;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.protocol.bedrock.data.LevelEvent;
import org.cloudburstmc.server.event.entity.CloudEntityDamageEvent;
import org.cloudburstmc.server.item.component.DefaultItemHandlers;

import static org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes.FUSE_TIME;

public class EntityTntMinecart extends EntityAbstractMinecart implements TntMinecart, Explosive {

    private static final int DEFAULT_FUSE = 80;
    private int fuse = -1;

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
        this.data.set(FUSE_TIME, this.fuse);
    }

    @Override
    public void loadAdditionalData(NbtMap tag) {
        super.loadAdditionalData(tag);
        tag.listenForInt("Fuse", this::setFuse);
    }

    @Override
    public void saveAdditionalData(NbtMapBuilder tag) {
        super.saveAdditionalData(tag);
        tag.putInt("Fuse", this.fuse);
    }

    @Override
    public boolean onUpdate(int currentTick) {
        int elapsed = currentTick - this.lastUpdate;
        double horizontalSpeedSquared = this.motion.getX() * this.motion.getX() + this.motion.getZ() * this.motion.getZ();
        boolean updated = super.onUpdate(currentTick);
        if (this.closed || !this.isAlive()) {
            return updated;
        }

        if (this.fuse >= 0 && elapsed > 0) {
            this.setFuse(Math.max(0, this.fuse - elapsed));
            if (this.fuse == 0) {
                this.explode();
                return false;
            }
        }

        if (this.isCollidedHorizontally && horizontalSpeedSquared >= 0.01) {
            this.explode(horizontalSpeedSquared);
            return false;
        }

        return updated || this.fuse >= 0;
    }

    @Override
    public void activate(int x, int y, int z, boolean powered) {
        if (powered) {
            this.primeFuse();
        }
    }

    @Override
    protected boolean applyDamage(CloudEntityDamageEvent event) {
        Entity direct = event.getDamageSource().getDirectEntity();
        boolean burningProjectile = direct instanceof Projectile && direct.isOnFire();
        if (this.getLevel().getGameRules().get(GameRules.TNT_EXPLODES) && (burningProjectile
                || event.getDamageType().is(DamageTypeTags.IS_FIRE)
                || event.getDamageType().is(DamageTypeTags.IS_EXPLOSION))) {
            this.getServer().getEventManager().fire(event);
            if (event.isCancelled() || event.getDamage() <= 0) {
                return false;
            }

            this.setLastDamageCause(event);
            if (burningProjectile) {
                this.explode();
            } else if (this.primeFuse()) {
                this.setFuse(this.getLevel().getRandom().nextInt(20) + this.getLevel().getRandom().nextInt(20));
            }

            return true;
        }

        return super.applyDamage(event);
    }

    @Override
    public void explode() {
        this.explode(this.motion.getX() * this.motion.getX() + this.motion.getZ() * this.motion.getZ());
    }

    private void explode(double horizontalSpeedSquared) {
        if (this.closed) {
            return;
        }

        if (this.getLevel().getGameRules().get(GameRules.TNT_EXPLODES)) {
            float speed = (float) Math.min(Math.sqrt(horizontalSpeedSquared), 5);
            float radius = 4 + this.getLevel().getRandom().nextFloat() * 1.5f * speed;
            ExplosionBlockInteraction interaction = this.getLevel().getGameRules().get(GameRules.TNT_EXPLOSION_DROP_DECAY)
                    ? ExplosionBlockInteraction.DESTROY_WITH_DECAY : ExplosionBlockInteraction.DESTROY;
            this.getLevel().explode(this.getPosition(), new ExplosionSettings(radius, interaction, false, this, null));
        }

        this.close();
    }

    @Override
    public int getFuse() {
        return this.fuse;
    }

    @Override
    public void setFuse(int fuse) {
        if (fuse < -1) {
            throw new IllegalArgumentException("Fuse must be -1 or non-negative");
        }

        this.fuse = fuse;
        this.data.set(FUSE_TIME, fuse);
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
        if (item.getType() == ItemTypes.FLINT_AND_STEEL || item.getType() == ItemTypes.FIRE_CHARGE) {
            boolean primed = this.primeFuse();
            if (primed && !player.isCreative()) {
                player.getInventory().setSelectedItem(item.getType() == ItemTypes.FLINT_AND_STEEL
                        ? DefaultItemHandlers.ON_DAMAGE.execute(item, 1, player) : item.decreaseCount());
            }

            return primed;
        }

        return super.onInteract(player, item, clickedPos);
    }

    @Override
    public boolean mount(Entity entity, MountType mode) {
        return false;
    }

    private boolean primeFuse() {
        if (this.fuse >= 0 || !this.getLevel().getGameRules().get(GameRules.TNT_EXPLODES)) {
            return false;
        }

        this.setFuse(DEFAULT_FUSE);
        this.getLevel().addLevelEvent(this.getPosition(), LevelEvent.SOUND_FUSE, 0);
        return true;
    }
}
