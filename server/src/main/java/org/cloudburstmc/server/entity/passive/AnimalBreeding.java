package org.cloudburstmc.server.entity.passive;

import lombok.Getter;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.event.entity.*;
import org.cloudburstmc.api.inventory.view.PlayerInventoryView;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.level.gamerule.GameRules;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudEntityRegistry;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiPredicate;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Transient feeding and courtship state, shared by breedable animals.
 */
public class AnimalBreeding {

    private static final int BABY_AGE = -24_000;
    private static final int LOVE_TICKS = 600;
    private static final int COOLDOWN_TICKS = 6_000;
    private static final int COURTSHIP_TICKS = 60;
    private static final int SEARCH_INTERVAL = 10;
    private static final float MATE_RANGE = 3;

    private final EntityBreedableAnimal animal;
    private final Consumer<Boolean> setLoveFlag;
    private final BiPredicate<EntitySpawnEvent, BooleanSupplier> guardedSpawn;

    private @Nullable Feeding feeding;
    private @Nullable AnimalBreeding mate;
    private int loveTicks;
    private int courtshipTicks;
    private int searchDelay;
    private boolean busy;
    @Getter
    private boolean pendingOffspring;

    public AnimalBreeding(EntityBreedableAnimal animal, Consumer<Boolean> setLoveFlag, BiPredicate<EntitySpawnEvent, BooleanSupplier> guardedSpawn) {
        this.animal = animal;
        this.setLoveFlag = setLoveFlag;
        this.guardedSpawn = guardedSpawn;
    }

    public boolean feed(Player player, ItemStack item) {
        if (this.busy || this.loveTicks > 0 || this.animal.getAge() > 0
                || !this.animal.isValid() || this.animal.isClosed()
                || !player.isAlive() || player.isClosed() || player.isSpectator()
                || player.getLevel() != this.animal.getLevel()
                || player instanceof CloudPlayer cloudPlayer && cloudPlayer.isEntityOutOfReach(this.animal)
                || item.isEmpty() || !this.animal.isBreedingItem(item)) {
            return false;
        }

        PlayerInventoryView inventory = player.getInventory();
        int slot = inventory.getSelectedSlot();
        if (!item.equals(inventory.getItem(slot))) {
            return false;
        }

        this.busy = true;
        try {
            int age = this.animal.getAge();
            if (age < 0) {
                this.animal.setAge(ageAfterFeeding(age));
            } else {
                this.feeding = new Feeding(player.getServerId(), item);
                this.loveTicks = LOVE_TICKS;
                this.setLoveFlag.accept(true);
            }

            if (!player.isCreative()) {
                inventory.setItem(slot, item.decreaseCount());
            }

            return true;
        } finally {
            this.busy = false;
        }
    }

    public static int ageAfterFeeding(int age) {
        return age < 0 ? age + (int) Math.max(1, -(long) age / 10) : age;
    }

    public void resetLove() {
        if (this.loveTicks > 0) {
            this.setLoveFlag.accept(false);
        }

        this.loveTicks = 0;
        this.feeding = null;
        this.mate = null;
        this.courtshipTicks = 0;
        this.searchDelay = 0;
    }

    public boolean isInLove() {
        return this.loveTicks > 0;
    }

    public void tick(int elapsedTicks) {
        if (this.busy || this.loveTicks == 0 || elapsedTicks <= 0) {
            return;
        }

        if (!this.isEligible() || elapsedTicks >= this.loveTicks) {
            this.resetLove();
            return;
        }

        this.loveTicks -= elapsedTicks;

        CloudLevel level = this.animal.getLevel();
        AnimalBreeding partner = this.mate;
        if (partner != null && !partner.busy && this.canMate(partner, level)) {
            this.courtshipTicks += elapsedTicks;
            if (this.courtshipTicks >= COURTSHIP_TICKS) {
                this.breed(partner, level);
            }
            return;
        }

        this.mate = null;
        this.courtshipTicks = 0;
        this.searchDelay = Math.max(0, this.searchDelay - elapsedTicks);
        if (this.searchDelay == 0) {
            this.searchDelay = SEARCH_INTERVAL;
            this.mate = this.findMate(level);
        }
    }

    private boolean isEligible() {
        return this.animal.isValid() && !this.animal.isClosed() && this.animal.getAge() == 0;
    }

    private boolean canMate(AnimalBreeding partner, CloudLevel level) {
        return this != partner && this.animal.getType() == partner.animal.getType()
                && this.animal.getLevel() == level && partner.animal.getLevel() == level
                && this.loveTicks > 0 && partner.loveTicks > 0
                && this.isEligible() && partner.isEligible()
                && this.animal.getPosition().distanceSquared(partner.animal.getPosition()) < MATE_RANGE * MATE_RANGE;
    }

    private @Nullable AnimalBreeding findMate(CloudLevel level) {
        AnimalBreeding nearest = null;
        float bestDistance = MATE_RANGE * MATE_RANGE;
        for (Entity entity : level.getNearbyEntities(this.animal.getBoundingBox().inflate(MATE_RANGE, MATE_RANGE, MATE_RANGE))) {
            AnimalBreeding candidate = of(entity);
            if (candidate == null || candidate.busy || !this.canMate(candidate, level)) {
                continue;
            }

            float distance = this.animal.getPosition().distanceSquared(entity.getPosition());
            if (distance < bestDistance) {
                nearest = candidate;
                bestDistance = distance;
            }
        }

        return nearest;
    }

    private void breed(AnimalBreeding partner, CloudLevel level) {
        this.busy = partner.busy = true;
        AnimalBreeding child = null;
        boolean spawned = false;
        try {
            child = this.createOffspring(partner, level);
            if (child == null) {
                return;
            }

            child.pendingOffspring = true;
            Feeding firstFeeding = this.feeding;
            if (firstFeeding == null || !this.canFinish(partner, child, level)) {
                return;
            }

            Player breeder = this.findBreeder(level);
            if (breeder == null) {
                breeder = partner.findBreeder(level);
            }

            EntityBreedEvent event = new EntityBreedEvent(child.animal, this.animal, partner.animal,
                    breeder, firstFeeding.item(), ThreadLocalRandom.current().nextInt(1, 8));
            level.getServer().getEventManager().fire(event);
            if (event.isCancelled() || !this.canFinish(partner, child, level)) {
                return;
            }

            AnimalBreeding offspring = child;
            spawned = child.guardedSpawn.test(new CreatureSpawnEvent(child.animal, CreatureSpawnReason.BREEDING), () -> this.canFinish(partner, offspring, level));
            if (!spawned) {
                return;
            }

            this.animal.setAge(COOLDOWN_TICKS);
            partner.animal.setAge(COOLDOWN_TICKS);
            if (event.getExperience() > 0 && level.getGameRules().get(GameRules.DO_MOB_LOOT)) {
                level.dropExpOrb(child.animal.getPosition(), event.getExperience());
            }
        } finally {
            this.resetLove();
            partner.resetLove();
            this.busy = partner.busy = false;
            if (child != null) {
                child.pendingOffspring = false;
                if (!spawned) {
                    child.animal.remove(EntityRemoveCause.DISCARD);
                }
            }
        }
    }

    private boolean canFinish(AnimalBreeding partner, AnimalBreeding child, CloudLevel level) {
        return this.canMate(partner, level) && child.animal.getLevel() == level
                && !child.animal.isClosed() && child.animal.isAlive() && !child.animal.isValid()
                && child.animal.getAge() < 0 && child.animal.isBaby();
    }

    private @Nullable AnimalBreeding createOffspring(AnimalBreeding partner, CloudLevel level) {
        Location location = Location.from(this.animal.getPosition(), level);
        Entity entity = CloudEntityRegistry.get().create(this.animal.getType(), location);

        AnimalBreeding offspring = of(entity);
        if (offspring != null && offspring.animal.getType() == this.animal.getType()) {
            offspring.animal.setAge(BABY_AGE);
            if (offspring.animal instanceof EntitySheep lamb && this.animal instanceof EntitySheep first && partner.animal instanceof EntitySheep second) {
                lamb.setColor((ThreadLocalRandom.current().nextBoolean() ? first : second).getColor());
            }

            return offspring;
        }

        if (entity instanceof org.cloudburstmc.server.entity.CloudEntity cloudEntity && !entity.isValid()) {
            cloudEntity.remove(EntityRemoveCause.DISCARD);
        }

        return null;
    }

    private @Nullable Player findBreeder(CloudLevel level) {
        return this.feeding == null ? null : level.getServer().getPlayer(this.feeding.playerId())
                .filter(player -> player.isOnline() && !player.isClosed() && player.getLevel() == level)
                .orElse(null);
    }

    private static @Nullable AnimalBreeding of(Entity entity) {
        return entity instanceof EntityBreedableAnimal animal ? animal.breeding() : null;
    }

    private record Feeding(UUID playerId, ItemStack item) {
    }
}
