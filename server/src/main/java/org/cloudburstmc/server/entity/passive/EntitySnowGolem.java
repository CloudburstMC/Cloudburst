package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.passive.SnowGolem;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.level.sound.SoundTypes;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.server.entity.CloudMob;
import org.cloudburstmc.server.entity.component.MobNameTagInteraction;
import org.cloudburstmc.server.event.entity.CloudEntityDamageEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import static org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag.SHEARED;

public class EntitySnowGolem extends CloudMob implements SnowGolem {

    public EntitySnowGolem(EntityType<SnowGolem> type, Location location) {
        super(type, location);
    }

    @Override
    public float getWidth() {
        return 0.7f;
    }

    @Override
    public float getHeight() {
        return 1.9f;
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setDefaultMaxHealth(4);
        this.setHealth(4);
    }

    @Override
    protected void loadCustom(NbtMap tag) {
        super.loadCustom(tag);
        this.setPumpkin(getBoolean(tag, "Pumpkin", this.hasPumpkin()));
    }

    @Override
    protected void saveCustom(NbtMapBuilder tag) {
        super.saveCustom(tag);
        tag.putBoolean("Pumpkin", this.hasPumpkin());
    }

    @Override
    protected Set<String> customKeys() {
        Set<String> keys = new HashSet<>(super.customKeys());
        keys.add("Pumpkin");
        return Set.copyOf(keys);
    }

    @Override
    protected boolean interactDefault(Player player, ItemStack item, Vector3f clickedPos) {
        return PlayerShearing.tryShear(this, player, item, this::createShearingDrops, this::applyShearing)
                || MobNameTagInteraction.tryApply(this, player, item);
    }

    @Override
    public boolean readyForShearing() {
        return this.isAlive() && this.hasPumpkin();
    }

    @Override
    public boolean shear() {
        if (!this.readyForShearing()) {
            return false;
        }

        this.applyShearing(this.createShearingDrops());
        return true;
    }

    private List<ItemStack> createShearingDrops() {
        return List.of(ItemStack.builder(BlockStates.CARVED_PUMPKIN).build());
    }

    private void applyShearing(List<ItemStack> drops) {
        this.setPumpkin(false);
        this.level.addLevelSoundEvent(this.getPosition(), SoundEvent.SHEAR);
        for (ItemStack drop : drops) {
            this.level.dropItem(this.getPosition().add(0, this.getEyeHeight(), 0), drop);
        }
    }

    @Override
    protected boolean applyDamage(CloudEntityDamageEvent event) {
        if (!super.applyDamage(event)) {
            return false;
        }

        this.level.playSound(this.getPosition(), this.isAlive() ? SoundTypes.MOB_SNOWGOLEM_HURT : SoundTypes.MOB_SNOWGOLEM_DEATH);
        return true;
    }

    @Override
    public ItemStack[] getDrops() {
        int snowballs = ThreadLocalRandom.current().nextInt(16);
        return snowballs == 0
                ? new ItemStack[0]
                : new ItemStack[]{ItemStack.from(ItemTypes.SNOWBALL).withCount(snowballs)};
    }

    @Override
    public boolean hasPumpkin() {
        return !this.data.getFlag(SHEARED);
    }

    @Override
    public void setPumpkin(boolean hasPumpkin) {
        if (this.hasPumpkin() != hasPumpkin) {
            this.data.setFlag(SHEARED, !hasPumpkin);
            if (this.isValid()) {
                this.markChunkDirty();
            }
        }
    }
}
