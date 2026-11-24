package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.Breedable;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.event.entity.EntitySpawnEvent;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.server.entity.data.CloudAgeableMobData;
import org.cloudburstmc.server.event.entity.CloudEntityDamageEvent;

import static org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag.IN_LOVE;

public abstract class EntityBreedableAnimal extends EntityAgeableAnimal implements Breedable {

    private final AnimalBreeding breeding = new AnimalBreeding(this, value -> this.data.setFlag(IN_LOVE, value), this::spawn);

    protected EntityBreedableAnimal(EntityType<?> type, Location location) {
        super(type, location, new CloudAgeableMobData(), false);
    }

    @Override
    protected void loadCustom(NbtMap tag) {
        this.breeding.resetLove();
        super.loadCustom(tag);
    }

    @Override
    public void setAge(int age) {
        super.setAge(age);
        if (age != 0) {
            this.breeding.resetLove();
        }
    }

    @Override
    public boolean canBreed() {
        return this.isAlive() && this.getAge() == 0;
    }

    @Override
    public boolean isInLove() {
        return this.breeding.isInLove();
    }

    @Override
    public boolean isBreedingItem(ItemStack item) {
        return item.getType() == ItemTypes.WHEAT;
    }

    @Override
    protected boolean interactDefault(Player player, ItemStack item, Vector3f clickedPos) {
        return this.breeding.feed(player, item) || super.interactDefault(player, item, clickedPos);
    }

    @Override
    public boolean entityBaseTick(int elapsedTicks) {
        boolean updated = super.entityBaseTick(elapsedTicks);
        this.breeding.tick(elapsedTicks);
        return this.isInLove() || updated;
    }

    @Override
    protected void onDamageAccepted(CloudEntityDamageEvent event) {
        this.breeding.resetLove();
        super.onDamageAccepted(event);
    }

    @Override
    public boolean spawn(EntitySpawnEvent event) {
        return !this.breeding.isPendingOffspring() && super.spawn(event);
    }

    @Override
    public void restoreFromStorage() {
        if (this.breeding.isPendingOffspring()) {
            throw new IllegalStateException("Breeding offspring cannot be restored before spawning");
        }

        super.restoreFromStorage();
    }

    public AnimalBreeding breeding() {
        return this.breeding;
    }
}
