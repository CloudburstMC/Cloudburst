package org.cloudburstmc.server.entity;

import com.nukkitx.math.vector.Vector2f;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import com.nukkitx.protocol.bedrock.data.entity.EntityLinkData;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.entity.data.SyncedEntityData;
import org.cloudburstmc.server.entity.misc.LightningBolt;
import org.cloudburstmc.server.entity.passive.Bat;
import org.cloudburstmc.server.event.entity.EntityDamageEvent;
import org.cloudburstmc.server.event.entity.EntityRegainHealthEvent;
import org.cloudburstmc.server.event.player.PlayerTeleportEvent;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.level.Location;
import org.cloudburstmc.server.math.AxisAlignedBB;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.metadata.Metadatable;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.potion.Effect;

import javax.annotation.Nonnegative;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

public interface Entity extends Metadatable {

    EntityType<?> getType();

    Level getLevel();

    CloudServer getServer();

    long getUniqueId();

    long getRuntimeId();

    float getHeight();

    float getEyeHeight();

    float getWidth();

    float getLength();

    void loadAdditionalData(NbtMap tag);

    void saveAdditionalData(NbtMapBuilder tag);

    boolean canCollide();

    void onEntityCollision(Entity entity);

    float getGravity();

    float getDrag();

    boolean hasNameTag();

    String getNameTag();

    void setNameTag(String name);

    boolean isNameTagVisible();

    float getScale();

    void setScale(float scale);

    List<Entity> getPassengers();

    boolean isPassenger(Entity entity);

    boolean isControlling(Entity entity);

    boolean hasControllingPassenger();

    Vector3f getSeatPosition();

    void setSeatPosition(Vector3f pos);

    Entity getVehicle();

    default boolean mount(Entity entity) {
        return this.mount(entity, EntityLinkData.Type.RIDER);
    }

    /**
     * Enter into a vehicle
     *
     * @param vehicle vehicle to mount
     * @param mode    mode
     * @return whether or not the mount was successful
     */
    boolean mount(Entity vehicle, EntityLinkData.Type mode);

    boolean dismount(Entity vehicle);

    void onMount(Entity passenger);

    void onDismount(Entity passenger);

    Short2ObjectMap<Effect> getEffects();

    void removeAllEffects();

    Effect getEffect(int effectId);

    void removeEffect(int effectId);

    boolean hasEffect(int effectId);

    void addEffect(Effect effect);

    String getName();

    void spawnTo(Player player);

    void spawnToAll();

    void despawnFrom(Player player);

    void despawnFromAll();

    Set<Player> getViewers();

    default boolean attack(float damage) {
        return this.attack(new EntityDamageEvent(this, EntityDamageEvent.DamageCause.CUSTOM, damage));
    }

    boolean attack(EntityDamageEvent source);

    default void heal(float amount) {
        this.heal(new EntityRegainHealthEvent(this, amount, EntityRegainHealthEvent.CAUSE_REGEN));
    }

    void heal(EntityRegainHealthEvent source);

    float getHealth();

    void setHealth(float health);

    int getMaxHealth();

    void setMaxHealth(int maxHealth);

    default boolean isAlive() {
        return getHealth() > 0;
    }

    EntityDamageEvent getLastDamageCause();

    boolean canCollideWith(Entity entity);

    Direction getDirection();

    Vector3f getDirectionVector();

    Vector2f getDirectionPlane();

    Direction getHorizontalDirection();

    boolean onUpdate(int currentTick);

    float getAbsorption();

    void setAbsorption(float absorption);

    default boolean isOnFire() {
        return getFireTicks() > 0;
    }

    void setOnFire(@Nonnegative int seconds);

    @Nonnegative
    int getFireTicks();

    void extinguish();

    int getNoDamageTicks();

    void setNoDamageTicks(int noDamageTicks);

    float getHighestPosition();

    void setHighestPosition(float highestPosition);

    void resetFallDistance();

    AxisAlignedBB getBoundingBox();

    void fall(float fallDistance);

    void onStruckByLightning(LightningBolt lightningBolt);

    boolean onInteract(Player player, ItemStack item, Vector3f clickedPos);

    float getX();

    float getY();

    float getZ();

    Vector3f getPosition();

    boolean setPosition(Vector3f pos);

    Location getLocation();

    Vector3f getMotion();

    boolean setMotion(Vector3f motion);

    void setRotation(float yaw, float pitch);

    boolean setPositionAndRotation(Vector3f pos, float yaw, float pitch);

    float getPitch();

    float getYaw();

    boolean canBeMovedByCurrents();

    /**
     * Whether the entity can active pressure plates.
     * Used for {@link Bat}s only.
     *
     * @return triggers pressure plate
     */
    boolean canTriggerPressurePlate();

    boolean canPassThrough();

    boolean isOnGround();

    void setOnGround(boolean onGround);

    default boolean isUndead() {
        return false;
    }

    void kill();

    default boolean teleport(Vector3f pos) {
        return this.teleport(pos, PlayerTeleportEvent.TeleportCause.PLUGIN);
    }

    boolean teleport(Vector3f pos, PlayerTeleportEvent.TeleportCause cause);

    default boolean teleport(Location location) {
        return this.teleport(location, PlayerTeleportEvent.TeleportCause.PLUGIN);
    }

    boolean teleport(Location location, PlayerTeleportEvent.TeleportCause cause);

    @Nullable
    Entity getOwner();

    void setOwner(@Nullable Entity entity);

    SyncedEntityData getData();

    NbtMap getTag();

    boolean isClosed();

    void close();
}
