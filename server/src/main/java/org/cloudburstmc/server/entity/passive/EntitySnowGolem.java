package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.passive.SnowGolem;
import org.cloudburstmc.api.event.entity.EntityDamageEvent;
import org.cloudburstmc.api.event.player.PlayerShearEntityEvent;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.server.entity.EntityCreature;
import org.cloudburstmc.server.item.component.DefaultItemHandlers;
import org.cloudburstmc.server.level.Sound;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag.SHEARED;

public class EntitySnowGolem extends EntityCreature implements SnowGolem {

    public EntitySnowGolem(EntityType<?> type, Location location) {
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
    public void initEntity() {
        super.initEntity();
        this.setMaxHealth(4);
        this.setHealth(4);
    }

    @Override
    public void loadAdditionalData(NbtMap tag) {
        super.loadAdditionalData(tag);
        tag.listenForBoolean("Pumpkin", pumpkin -> this.setDerp(!pumpkin));
    }

    @Override
    public void saveAdditionalData(NbtMapBuilder tag) {
        super.saveAdditionalData(tag);
        tag.putBoolean("Pumpkin", !this.isDerp());
    }

    @Override
    public boolean onInteract(Player player, ItemStack item) {
        if (item.getType() != ItemTypes.SHEARS || this.isDerp()) {
            return false;
        }

        ItemStack pumpkin = ItemStack.builder(BlockStates.CARVED_PUMPKIN).build();
        PlayerShearEntityEvent event = new PlayerShearEntityEvent(player, this, item, List.of(pumpkin));
        this.server.getEventManager().fire(event);
        if (event.isCancelled()) {
            return true;
        }

        this.setDerp(true);
        this.level.addLevelSoundEvent(this.getPosition(), SoundEvent.SHEAR);
        for (ItemStack drop : event.getDrops()) {
            this.level.dropItem(this.getPosition().add(0, this.getEyeHeight(), 0), drop);
        }

        if (!player.isCreative()) {
            player.getInventory().setSelectedItem(DefaultItemHandlers.ON_DAMAGE.execute(item, 1, player));
        }

        return true;
    }

    @Override
    public boolean attack(EntityDamageEvent source) {
        if (!super.attack(source)) {
            return false;
        }

        this.level.addSound(this.getPosition(), this.isAlive() ? Sound.MOB_SNOWGOLEM_HURT : Sound.MOB_SNOWGOLEM_DEATH);
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
    public boolean isDerp() {
        return this.data.getFlag(SHEARED);
    }

    @Override
    public void setDerp(boolean derp) {
        this.data.setFlag(SHEARED, derp);
    }

}
