package org.cloudburstmc.server.entity.misc;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.misc.OminousItemSpawner;
import org.cloudburstmc.api.event.entity.EntityRemoveCause;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.level.sound.SoundPlayback;
import org.cloudburstmc.api.level.sound.SoundTypes;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.protocol.bedrock.data.LevelEvent;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.event.entity.CloudEntityDamageEvent;
import org.cloudburstmc.server.item.ItemStackNbtCodec;
import org.cloudburstmc.server.item.ItemStackPacketCodec;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import static org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes.DISPLAY_FIREWORK;

public class EntityOminousItemSpawner extends CloudEntity implements OminousItemSpawner {

    private static final String ITEM_KEY = "Item";
    private static final String DELAY_KEY = "TicksUntilSpawn";

    private static final int MIN_DELAY = 60;
    private static final int MAX_DELAY = 120;
    private static final int WARNING_TICKS = 36;
    private static final SoundPlayback WARNING_SOUND = new SoundPlayback(SoundTypes.OMINOUS_ITEM_SPAWNER_ABOUT_TO_SPAWN_ITEM, 1, 1);

    private ItemStack item = ItemStack.EMPTY;
    private int ticksUntilSpawn = randomDelay();

    public EntityOminousItemSpawner(EntityType<OminousItemSpawner> type, Location location) {
        super(type, location);
        this.noPhysics = true;
    }

    @Override
    protected void loadCustom(NbtMap tag) {
        super.loadCustom(tag);
        ItemStack item = tag.get(ITEM_KEY) instanceof NbtMap itemTag ? ItemStackNbtCodec.deserializeItem(itemTag) : ItemStack.EMPTY;
        this.updateItem(item);
        this.ticksUntilSpawn = tag.containsKey(DELAY_KEY) ? Math.max(0, tag.getInt(DELAY_KEY)) : randomDelay();
    }

    @Override
    protected void saveCustom(NbtMapBuilder tag) {
        super.saveCustom(tag);
        if (!this.item.isEmpty()) {
            tag.putCompound(ITEM_KEY, ItemStackNbtCodec.serializeItem(this.item));
            tag.putInt(DELAY_KEY, this.ticksUntilSpawn);
        }
    }

    @Override
    protected Set<String> customKeys() {
        Set<String> keys = new HashSet<>(super.customKeys());
        keys.add(ITEM_KEY);
        keys.add(DELAY_KEY);
        return keys;
    }

    @Override
    public ItemStack getItem() {
        return this.item;
    }

    @Override
    public void setItem(ItemStack item) {
        this.updateItem(Objects.requireNonNull(item, "item"));
        this.markChunkDirty();
    }

    @Override
    public int getTicksUntilSpawn() {
        return this.ticksUntilSpawn;
    }

    @Override
    public void setTicksUntilSpawn(int ticks) {
        if (ticks < 0) {
            throw new IllegalArgumentException("Spawn delay must be non-negative");
        }

        this.ticksUntilSpawn = ticks;
        this.markChunkDirty();
    }

    @Override
    protected boolean tickEntity(int currentTick, int elapsedTicks) {
        boolean needsNextTick = super.tickEntity(currentTick, elapsedTicks);
        if (!this.isAlive()) {
            return needsNextTick;
        }

        if (this.item.isEmpty()) {
            this.remove(EntityRemoveCause.DISCARD);
            return false;
        }

        int previousDelay = this.ticksUntilSpawn;
        this.setTicksUntilSpawn(Math.max(0, previousDelay - elapsedTicks));

        if (this.ticksUntilSpawn == 0) {
            this.releaseItem();
            return false;
        }

        if (previousDelay > WARNING_TICKS && this.ticksUntilSpawn <= WARNING_TICKS) {
            this.level.playSound(this.getPosition(), WARNING_SOUND);
        }

        return true;
    }

    @Override
    protected boolean applyDamage(CloudEntityDamageEvent event) {
        return false;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return false;
    }

    @Override
    public boolean canCollide() {
        return false;
    }

    private void updateItem(ItemStack item) {
        ItemData display = ItemStackPacketCodec.toNetwork(item);
        NbtMapBuilder tag = NbtMap.builder()
                .putString("Name", display.getDefinition().getIdentifier())
                .putByte("Count", (byte) display.getCount())
                .putShort("Damage", (short) display.getDamage());
        if (display.getTag() != null) {
            tag.putCompound("tag", display.getTag());
        }
        this.item = item;
        this.data.set(DISPLAY_FIREWORK, tag.build());
    }

    private void releaseItem() {
        ItemStack released = this.item;
        Vector3f position = this.getPosition();
        this.remove(EntityRemoveCause.DROP);
        if (this.level.dropItem(position, released, Vector3f.ZERO).isValid()) {
            this.level.addLevelEvent(position, LevelEvent.PARTICLE_TRIAL_SPAWNER_EJECTING, 1);
        }
    }

    private static int randomDelay() {
        return ThreadLocalRandom.current().nextInt(MIN_DELAY, MAX_DELAY + 1);
    }
}
