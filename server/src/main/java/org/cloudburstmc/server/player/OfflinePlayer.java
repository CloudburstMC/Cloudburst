package org.cloudburstmc.server.player;

import com.nukkitx.math.vector.Vector2f;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.nbt.NbtMap;
import org.cloudburstmc.api.Server;
import org.cloudburstmc.api.blockentity.EnderChest;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.misc.LightningBolt;
import org.cloudburstmc.api.event.entity.EntityDamageEvent;
import org.cloudburstmc.api.event.entity.EntityRegainHealthEvent;
import org.cloudburstmc.api.event.player.PlayerTeleportEvent;
import org.cloudburstmc.api.inventory.ContainerInventory;
import org.cloudburstmc.api.inventory.CraftingGrid;
import org.cloudburstmc.api.inventory.Inventory;
import org.cloudburstmc.api.inventory.PlayerInventory;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.api.player.GameMode;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.player.skin.Skin;
import org.cloudburstmc.api.potion.Effect;
import org.cloudburstmc.api.potion.EffectType;
import org.cloudburstmc.api.util.AxisAlignedBB;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.data.CardinalDirection;
import org.cloudburstmc.api.util.data.MountType;
import org.cloudburstmc.server.CloudServer;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Represents a player that is currently offline.
 * //TODO Figure out better implementation of Offline players
 */
public class OfflinePlayer implements Player {
    private final CloudServer server;
    private final NbtMap namedTag;

    /**
     * 初始化这个{@code OfflinePlayer}对象。<br>
     * Initializes the object {@code OfflinePlayer}.
     *
     * @param server 这个玩家所在服务器的{@code Server}对象。<br>
     *               The server this player is in, as a {@code Server} object.
     * @param uuid   这个玩家的UUID。<br>
     *               UUID of this player.
     * @since Nukkit 1.0 | Nukkit API 1.0.0
     */
    public OfflinePlayer(Server server, UUID uuid) {
        this(server, uuid, null);
    }

    public OfflinePlayer(Server server, String name) {
        this(server, null, name);
    }

    public OfflinePlayer(Server server, UUID uuid, String name) {
        this.server = (CloudServer) server;

        NbtMap nbt;
        if (uuid != null) {
            nbt = this.server.getOfflinePlayerData(uuid, false);
        } else if (name != null) {
            nbt = this.server.getOfflinePlayerData(name, false);
        } else {
            throw new IllegalArgumentException("Name and UUID cannot both be null");
        }
        if (nbt == null) {
            nbt = NbtMap.EMPTY;
        }

        if (uuid != null) {
            nbt = nbt.toBuilder()
                    .putLong("UUIDMost", uuid.getMostSignificantBits())
                    .putLong("UUIDLeast", uuid.getLeastSignificantBits())
                    .build();
        } else {
            nbt = nbt.toBuilder().putString("NameTag", name).build();
        }
        this.namedTag = nbt;
    }

    @Override
    public boolean isOnline() {
        return false;
    }

    @Override
    public String getName() {
        if (namedTag != null && namedTag.containsKey("NameTag")) {
            return namedTag.getString("NameTag");
        }
        return null;
    }

    @Override
    public void spawnTo(Player player) {

    }

    @Override
    public void spawnToAll() {

    }

    @Override
    public void despawnFrom(Player player) {

    }

    @Override
    public void despawnFromAll() {

    }

    @Override
    public Set<? extends Player> getViewers() {
        return null;
    }

    @Override
    public boolean attack(EntityDamageEvent source) {
        return false;
    }

    @Override
    public void heal(EntityRegainHealthEvent source) {

    }

    @Override
    public float getHealth() {
        return 0;
    }

    @Override
    public void setHealth(float health) {

    }

    @Override
    public int getMaxHealth() {
        return 0;
    }

    @Override
    public void setMaxHealth(int maxHealth) {

    }

    @Override
    public EntityDamageEvent getLastDamageCause() {
        return null;
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        return false;
    }

    @Override
    public Direction getDirection() {
        return null;
    }

    @Override
    public Vector3f getDirectionVector() {
        return null;
    }

    @Override
    public Vector2f getDirectionPlane() {
        return null;
    }

    @Override
    public Direction getHorizontalDirection() {
        return null;
    }

    @Override
    public boolean onUpdate(int currentTick) {
        return false;
    }

    @Override
    public float getAbsorption() {
        return 0;
    }

    @Override
    public void setAbsorption(float absorption) {

    }

    @Override
    public void setOnFire(int seconds) {

    }

    @Override
    public int getFireTicks() {
        return 0;
    }

    @Override
    public void extinguish() {

    }

    @Override
    public int getNoDamageTicks() {
        return 0;
    }

    @Override
    public void setNoDamageTicks(int noDamageTicks) {

    }

    @Override
    public float getHighestPosition() {
        return 0;
    }

    @Override
    public void setHighestPosition(float highestPosition) {

    }

    @Override
    public void resetFallDistance() {

    }

    @Override
    public AxisAlignedBB getBoundingBox() {
        return null;
    }

    @Override
    public void fall(float fallDistance) {

    }

    @Override
    public void onStruckByLightning(LightningBolt lightningBolt) {

    }

    @Override
    public boolean onInteract(Player player, ItemStack item, Vector3f clickedPos) {
        return false;
    }

    @Override
    public float getX() {
        return 0;
    }

    @Override
    public float getY() {
        return 0;
    }

    @Override
    public float getZ() {
        return 0;
    }

    @Override
    public Vector3f getPosition() {
        return null;
    }

    @Override
    public boolean setPosition(Vector3f pos) {
        return false;
    }

    @Override
    public Location getLocation() {
        return null;
    }

    @Override
    public Vector3f getMotion() {
        return null;
    }

    @Override
    public boolean setMotion(Vector3f motion) {
        return false;
    }

    @Override
    public void setRotation(float yaw, float pitch) {

    }

    @Override
    public boolean setPositionAndRotation(Vector3f pos, float yaw, float pitch) {
        return false;
    }

    @Override
    public float getPitch() {
        return 0;
    }

    @Override
    public float getYaw() {
        return 0;
    }

    @Override
    public boolean canBeMovedByCurrents() {
        return false;
    }

    @Override
    public boolean canTriggerPressurePlate() {
        return false;
    }

    @Override
    public boolean canPassThrough() {
        return false;
    }

    @Override
    public UUID getServerId() {
        if (namedTag != null) {
            long least = namedTag.getLong("UUIDLeast");
            long most = namedTag.getLong("UUIDMost");

            if (least != 0 && most != 0) {
                return new UUID(most, least);
            }
        }
        return null;
    }

    @Override
    public EntityType<?> getType() {
        return null;
    }

    @Override
    public Chunk getChunk() {
        return null;
    }

    public CloudServer getServer() {
        return server;
    }

    @Override
    public long getUniqueId() {
        return 0;
    }

    @Override
    public long getRuntimeId() {
        return 0;
    }

    @Override
    public float getHeight() {
        return 0;
    }

    @Override
    public float getEyeHeight() {
        return 0;
    }

    @Override
    public float getWidth() {
        return 0;
    }

    @Override
    public float getLength() {
        return 0;
    }

    @Override
    public boolean canCollide() {
        return false;
    }

    @Override
    public void onEntityCollision(Entity entity) {

    }

    @Override
    public float getGravity() {
        return 0;
    }

    @Override
    public float getDrag() {
        return 0;
    }

    @Override
    public boolean hasNameTag() {
        return false;
    }

    @Override
    public String getNameTag() {
        return null;
    }

    @Override
    public void setNameTag(String name) {

    }

    @Override
    public boolean isNameTagVisible() {
        return false;
    }

    @Override
    public void setNameTagVisible(boolean visible) {

    }

    @Override
    public float getScale() {
        return 0;
    }

    @Override
    public void setScale(float scale) {

    }

    @Override
    public List<? extends Entity> getPassengers() {
        return null;
    }

    @Override
    public boolean isPassenger(Entity entity) {
        return false;
    }

    @Override
    public boolean isControlling(Entity entity) {
        return false;
    }

    @Override
    public boolean hasControllingPassenger() {
        return false;
    }

    @Override
    public Vector3f getSeatPosition() {
        return null;
    }

    @Override
    public void setSeatPosition(Vector3f pos) {

    }

    @Override
    public Entity getVehicle() {
        return null;
    }

    @Override
    public boolean mount(Entity vehicle, MountType mode) {
        return false;
    }

    @Override
    public boolean dismount(Entity vehicle) {
        return false;
    }

    @Override
    public void onMount(Entity passenger) {

    }

    @Override
    public void onDismount(Entity passenger) {

    }

    @Override
    public Map<EffectType, Effect> getEffects() {
        return null;
    }

    @Override
    public void removeAllEffects() {

    }

    @Override
    public void addEffect(Effect effect) {

    }

    @Override
    public Effect getEffect(int effectId) {
        return null;
    }

    @Override
    public Effect getEffect(EffectType type) {
        return null;
    }

    @Override
    public void removeEffect(int effectId) {

    }

    @Override
    public void removeEffect(EffectType type) {

    }

    @Override
    public boolean hasEffect(int effectId) {
        return false;
    }

    @Override
    public boolean hasEffect(EffectType type) {
        return false;
    }

    @Override
    public boolean isOp() {
        return this.server.isOp(this.getName().toLowerCase());
    }

    @Override
    public void setMovementSpeed(float speed) {

    }

    @Override
    public float getMovementSpeed() {
        return 0;
    }

    @Override
    public Level getLevel() {
        return null;
    }

    @Override
    public boolean isSpectator() {
        return false;
    }

    @Override
    public boolean isCreative() {
        return false;
    }

    @Override
    public boolean isSurvival() {
        return false;
    }

    @Override
    public void resetInAirTicks() {

    }

    @Override
    public boolean isSpawned() {
        return false;
    }

    @Override
    public GameMode getGamemode() {
        return null;
    }

    @Override
    public byte getWindowId(Inventory inventory) {
        return 0;
    }

    @Override
    public byte addWindow(Inventory window, Byte forceId, boolean isPermanent) {
        return 0;
    }

    @Override
    public void removeWindow(Inventory inventory) {

    }

    @Override
    public String getDisplayName() {
        return null;
    }

    @Override
    public String getXuid() {
        return null;
    }

    @Override
    public boolean isConnected() {
        return false;
    }

    @Override
    public void save(boolean async) {

    }

    @Override
    public CardinalDirection getCardinalDirection() {
        return null;
    }

    @Override
    public boolean isAdventure() {
        return false;
    }

    @Override
    public Location getSpawn() {
        return null;
    }

    @Override
    public void setSpawn(Location spawn) {

    }

    @Override
    public Skin getSkin() {
        return null;
    }

    @Override
    public void setSkin(Skin newSkin) {

    }

    @Override
    public EnderChest getViewingEnderChest() {
        return null;
    }

    @Override
    public void setViewingEnderChest(EnderChest chest) {

    }

    @Override
    public void setOp(boolean value) {
        if (value == this.isOp()) {
            return;
        }

        if (value) {
            this.server.addOp(this.getName().toLowerCase());
        } else {
            this.server.removeOp(this.getName().toLowerCase());
        }
    }

    @Override
    public boolean isBanned() {
        return this.server.getNameBans().isBanned(this.getName());
    }

    @Override
    public void setBanned(boolean value) {
        if (value) {
            this.server.getNameBans().addBan(this.getName(), null, null, null);
        } else {
            this.server.getNameBans().remove(this.getName());
        }
    }

    @Override
    public boolean isWhitelisted() {
        return this.server.isWhitelisted(this.getName().toLowerCase());
    }

    @Override
    public void setWhitelisted(boolean value) {
        if (value) {
            this.server.addWhitelist(this.getName().toLowerCase());
        } else {
            this.server.removeWhitelist(this.getName().toLowerCase());
        }
    }

    /*@Override
    public Player getPlayer() {
        return this.server.getPlayerExact(this.getName());
    }
*/
    @Override
    public OptionalLong getFirstPlayed() {
        return this.namedTag != null ? OptionalLong.of(this.namedTag.getLong("firstPlayed")) : OptionalLong.empty();
    }

    @Override
    public OptionalLong getLastPlayed() {
        return this.namedTag != null ? OptionalLong.of(this.namedTag.getLong("lastPlayed")) : OptionalLong.empty();
    }

    @Override
    public boolean hasPlayedBefore() {
        return this.namedTag != NbtMap.EMPTY;
    }

    @Override
    public boolean isInsideOfWater() {
        return false;
    }

    @Override
    public boolean isSneaking() {
        return false;
    }

    @Override
    public boolean isSleeping() {
        return false;
    }

    @Override
    public boolean sleepOn(Vector3i pos) {
        return false;
    }

    @Override
    public void stopSleep() {

    }

    @Override
    public boolean isOnGround() {
        return false;
    }

    @Override
    public PlayerInventory getInventory() {
        return null;
    }

    @Override
    public ContainerInventory getEnderChestInventory() {
        return null;
    }

    @Override
    public CraftingGrid getCraftingInventory() {
        return null;
    }

    @Override
    public void setOnGround(boolean onGround) {

    }

    @Override
    public void kill() {

    }

    @Override
    public boolean teleport(Vector3f pos, PlayerTeleportEvent.TeleportCause cause) {
        return false;
    }

    @Override
    public boolean teleport(Location location, PlayerTeleportEvent.TeleportCause cause) {
        return false;
    }

    @Nullable
    @Override
    public Entity getOwner() {
        return null;
    }

    @Override
    public void setOwner(@Nullable Entity entity) {

    }

    @Override
    public boolean isClosed() {
        return false;
    }

    @Override
    public void close() {

    }
}
