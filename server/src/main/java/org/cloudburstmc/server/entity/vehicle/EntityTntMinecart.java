package org.cloudburstmc.server.entity.vehicle;

import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.damage.DamageTypeTags;
import org.cloudburstmc.api.entity.projectile.Projectile;
import org.cloudburstmc.api.entity.vehicle.TntMinecart;
import org.cloudburstmc.api.event.entity.EntityRemoveCause;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.ExplosionBlockInteraction;
import org.cloudburstmc.api.level.ExplosionSettings;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.level.gamerule.GameRules;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.data.MinecartType;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.protocol.bedrock.data.LevelEvent;
import org.cloudburstmc.server.entity.data.CloudTntMinecartData;
import org.cloudburstmc.server.event.entity.CloudEntityDamageEvent;
import org.cloudburstmc.server.item.component.DefaultItemHandlers;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.level.Explosion;

import static org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes.FUSE_TIME;

public class EntityTntMinecart extends EntityAbstractMinecart implements TntMinecart {

    private static final int DEFAULT_FUSE = 80;
    private static final float DEFAULT_YIELD = 4;

    private final CloudTntMinecartData persistenceState;
    private boolean detonating;

    public EntityTntMinecart(EntityType<TntMinecart> type, Location location) {
        CloudTntMinecartData state = new CloudTntMinecartData();
        super(type, location, state);
        this.persistenceState = state;
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setDisplayBlock(BlockStates.TNT);
        this.setDisplay(true);
        this.data.set(FUSE_TIME, this.data().getFuseTicks());
    }

    @Override
    protected CloudTntMinecartData data() {
        return this.persistenceState;
    }

    @Override
    protected void loadCustom(NbtMap tag) {
        if (tag.containsKey("Fuse")) {
            this.setFuseTicks(this.data().getFuseTicks());
        }
        super.loadCustom(tag);
    }

    @Override
    public MinecartType getMinecartType() {
        return MinecartType.valueOf(3);
    }

    @Override
    public boolean isRideable() {
        return false;
    }

    @Override
    public int getFuseTicks() {
        return this.data().getFuseTicks();
    }

    @Override
    public void setFuseTicks(int fuseTicks) {
        if (fuseTicks < -1) {
            throw new IllegalArgumentException("Fuse must be -1 or non-negative");
        }

        this.data().setFuseTicks(fuseTicks);
        this.data.set(FUSE_TIME, fuseTicks);
    }

    @Override
    protected boolean tickEntity(int currentTick, int elapsedTicks) {
        double horizontalSpeedSquared = this.motion.getX() * this.motion.getX() + this.motion.getZ() * this.motion.getZ();
        boolean updated = super.tickEntity(currentTick, elapsedTicks);
        if (this.isClosed() || !this.isAlive()) {
            return updated;
        }

        int fuseTicks = this.getFuseTicks();
        if (fuseTicks >= 0 && elapsedTicks > 0) {
            // The countdown reaches zero before the tick that detonates it.
            this.setFuseTicks(Math.max(0, fuseTicks - elapsedTicks));
            if (elapsedTicks > fuseTicks) {
                this.explode();
                return updated && !this.isClosed();
            }
        }

        if (this.hasHorizontalCollision() && horizontalSpeedSquared >= 0.01) {
            this.detonate(horizontalSpeedSquared);
            return updated && !this.isClosed();
        }

        return updated || this.getFuseTicks() >= 0;
    }

    @Override
    public void activate(int x, int y, int z, boolean powered) {
        if (powered) {
            this.primeFuse();
        }
    }

    private boolean primeFuse() {
        if (this.getFuseTicks() >= 0 || !this.getLevel().getGameRules().get(GameRules.TNT_EXPLODES)) {
            return false;
        }

        this.setFuseTicks(DEFAULT_FUSE);
        this.getLevel().addLevelEvent(this.getPosition(), LevelEvent.SOUND_FUSE, 0);
        return true;
    }

    @Override
    protected boolean interactDefault(Player player, ItemStack item, Vector3f clickedPos) {
        if (item.getType() == ItemTypes.FLINT_AND_STEEL || item.getType() == ItemTypes.FIRE_CHARGE) {
            boolean primed = this.primeFuse();
            if (primed && !player.isCreative()) {
                player.getInventory().setSelectedItem(item.getType() == ItemTypes.FLINT_AND_STEEL
                        ? DefaultItemHandlers.ON_DAMAGE.execute(item, 1, player) : item.decreaseCount());
            }

            return primed;
        }

        return super.interactDefault(player, item, clickedPos);
    }

    @Override
    public boolean mount(Entity vehicle) {
        return false;
    }

    @Override
    protected boolean applyDamage(CloudEntityDamageEvent event) {
        if (this.isDamageImmune(event.getDamageSource())) {
            return false;
        }

        Entity direct = event.getDamageSource().getDirectEntity();
        boolean burningProjectile = direct instanceof Projectile && direct.isOnFire();
        if (this.getLevel().getGameRules().get(GameRules.TNT_EXPLODES) && (burningProjectile
                || event.getDamageType().is(DamageTypeTags.IS_FIRE)
                || event.getDamageType().is(DamageTypeTags.IS_EXPLOSION))) {
            this.getServer().getEventManager().fire(event);
            if (event.isCancelled() || event.getDamage() <= 0 || this.isClosed() || !this.isAlive()) {
                return false;
            }

            this.setLastDamageCause(event);
            if (burningProjectile) {
                this.explode();
            } else if (this.primeFuse()) {
                this.setFuseTicks(this.getLevel().getRandom().nextInt(20) + this.getLevel().getRandom().nextInt(20));
            }

            return true;
        }

        return super.applyDamage(event);
    }

    @Override
    public float getYield() {
        Float yield = this.data().getExplosionYield();
        return yield != null && Float.isFinite(yield) && yield >= 0 ? yield : DEFAULT_YIELD;
    }

    @Override
    public void setYield(float yield) {
        if (!Float.isFinite(yield) || yield < 0) {
            throw new IllegalArgumentException("Explosion radius must be finite and non-negative");
        }

        this.data().setExplosionYield(yield);
        this.markChunkDirty();
    }

    @Override
    public boolean isIncendiary() {
        Boolean incendiary = this.data().getIncendiary();
        return incendiary != null && incendiary;
    }

    @Override
    public void setIsIncendiary(boolean incendiary) {
        this.data().setIncendiary(incendiary);
        this.markChunkDirty();
    }

    @Override
    public void explode() {
        this.detonate(this.motion.getX() * this.motion.getX() + this.motion.getZ() * this.motion.getZ());
    }

    private void detonate(double horizontalSpeedSquared) {
        if (this.isClosed() || this.detonating) {
            return;
        }

        CloudLevel level = this.getLevel();
        if (!level.getGameRules().get(GameRules.TNT_EXPLODES)) {
            if (this.getFuseTicks() >= 0) {
                this.remove(EntityRemoveCause.DISCARD);
            }
            return;
        }

        float speed = (float) Math.min(Math.sqrt(horizontalSpeedSquared), 5);
        float radius = this.getYield() + level.getRandom().nextFloat() * 1.5f * speed;
        ExplosionBlockInteraction interaction = level.getGameRules().get(GameRules.TNT_EXPLOSION_DROP_DECAY)
                ? ExplosionBlockInteraction.DESTROY_WITH_DECAY : ExplosionBlockInteraction.DESTROY;

        this.detonating = true;
        try {
            ExplosionSettings primed = level.primeExplosion(
                    new ExplosionSettings(radius, interaction, this.isIncendiary(), this, null));
            if (primed == null) {
                if (!this.isClosed()) {
                    this.setFuseTicks(-1);
                    this.markChunkDirty();
                }
                return;
            }

            if (!this.isClosed() && this.getLevel() == level) {
                new Explosion(level, this.getPosition(), primed).explode();
                this.remove(EntityRemoveCause.EXPLODE);
            }
        } finally {
            this.detonating = false;
        }
    }

    @Override
    protected ItemType getDropItemType() {
        return ItemTypes.TNT_MINECART;
    }
}
