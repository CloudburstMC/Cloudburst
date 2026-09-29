package org.cloudburstmc.server.entity.ai.executor;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityIntelligent;
import org.cloudburstmc.api.entity.ai.behavior.BehaviorExecutor;
import org.cloudburstmc.api.entity.ai.memory.MemoryTypes;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.server.entity.passive.Animal;
import org.cloudburstmc.server.level.CloudLevel;

import static org.cloudburstmc.server.entity.ai.executor.EntityControlHelper.*;

/**
 * Generic breeding executor. Finds the nearest in-love entity of the same type,
 * moves both toward each other, and spawns a baby after the breeding duration.
 *
 * @author daoge_cmd
 */
public class EntityBreedingExecutor implements BehaviorExecutor {
    protected static final int FINDING_RANGE_SQUARED = 256; // 16 blocks

    protected final int duration;
    protected final float speed;

    protected int tickCounter;
    protected Entity spouse;
    protected boolean isInitiator;
    protected Vector3d lastTargetPos;

    public EntityBreedingExecutor(int duration, float speed) {
        this.duration = duration;
        this.speed = speed;
    }

    @Override
    public void onStart(EntityIntelligent entity) {
        tickCounter = 0;
        spouse = null;
        isInitiator = false;
        lastTargetPos = null;
        entity.setMovementSpeed(speed);
    }

    @Override
    public boolean execute(EntityIntelligent entity) {
        tickCounter++;

        if (spouse == null || !spouse.isAlive()) {
            spouse = null;
            // Check if another entity's executor already paired with us
            var spouseId = entity.getMemoryStorage().get(MemoryTypes.ENTITY_SPOUSE);
            if (spouseId != null) {
                var resolved = ((CloudLevel) entity.getLevel()).getEntityByRuntimeId(spouseId);
                if (resolved != null && resolved.isAlive()) {
                    spouse = resolved;
                }
            }

            if (spouse == null) {
                spouse = findSpouse(entity);
                if (spouse == null) {
                    return true; // keep searching
                }
                isInitiator = true;
                if (spouse instanceof EntityIntelligent spouseIntelligent) {
                    // Atomically claim the spouse — if another entity already claimed it, retry next tick
                    if (!spouseIntelligent
                            .getMemoryStorage()
                            .putIfAbsent(MemoryTypes.ENTITY_SPOUSE, entity.getUniqueId())) {
                        spouse = null;
                        isInitiator = false;
                        return true;
                    }
                }
                entity.getMemoryStorage().put(MemoryTypes.ENTITY_SPOUSE, spouse.getUniqueId());
            }
        }

        // Move toward spouse, re-path only when target moved >1 block
        var spousePos = spouse.getPosition();
        var targetPos = Vector3f.from(spousePos.getX(), spousePos.getY(), spousePos.getZ());
        if (lastTargetPos == null || lastTargetPos.distanceSquared(targetPos.toDouble()) > 1.0) {
            setRouteTarget(entity, targetPos);
            lastTargetPos = targetPos.toDouble();
        }

        setLookTarget(
                entity, Vector3f.from(spousePos.getX(), spousePos.getY() + spouse.getEyeHeight(), spousePos.getZ()));

        // Only the initiator checks for breeding completion and spawns the baby
        if (isInitiator) {
            double distSq = entity.getPosition().distanceSquared(spousePos);
            if (distSq < 4.0 && tickCounter >= duration) {
                spawnBaby(entity);
                entity.getMemoryStorage().put(MemoryTypes.IS_IN_LOVE, false);
                if (spouse instanceof EntityIntelligent spouseIntelligent) {
                    spouseIntelligent.getMemoryStorage().put(MemoryTypes.IS_IN_LOVE, false);
                }
                return false;
            }
        }

        return tickCounter < duration + 60; // timeout after duration + buffer
    }

    @Override
    public void onStop(EntityIntelligent entity) {
        clearEntityState(entity);
        if (spouse instanceof EntityIntelligent spouseIntelligent) {
            clearEntityState(spouseIntelligent);
        }
        spouse = null;
        isInitiator = false;
        lastTargetPos = null;
    }

    @Override
    public void onInterrupt(EntityIntelligent entity) {
        onStop(entity);
    }

    protected void clearEntityState(EntityIntelligent entity) {
        removeRouteTarget(entity);
        removeLookTarget(entity);
        entity.getMemoryStorage().clear(MemoryTypes.ENTITY_SPOUSE);
    }

    protected Entity findSpouse(EntityIntelligent entity) {
        Entity nearest = null;
        double nearestDistSq = Double.MAX_VALUE;

        for (var candidate : entity.getLevel().getEntities()) {
            if (candidate == entity) continue;
            if (candidate.getType() != entity.getType()) continue;
            if (!(candidate instanceof EntityIntelligent candidateIntelligent)) continue;
            if (!candidateIntelligent.getMemoryStorage().get(MemoryTypes.IS_IN_LOVE)) continue;
            // Skip babies
            if (candidate instanceof Animal animal && animal.isBaby()) continue;
            // Skip already paired entities
            if (candidateIntelligent.getMemoryStorage().get(MemoryTypes.ENTITY_SPOUSE) != null) continue;

            double distSq = entity.getPosition().distanceSquared(candidate.getPosition());
            if (distSq > FINDING_RANGE_SQUARED) continue;
            if (distSq < nearestDistSq) {
                nearestDistSq = distSq;
                nearest = candidate;
            }
        }

        return nearest;
    }

    protected void spawnBaby(EntityIntelligent entity) {
        var loc = entity.getLocation();
        var babyPos = Location.from(loc.getX(), loc.getY(), loc.getZ(), loc.getLevel());
        var baby = entity.getServer().getEntityRegistry().create(entity.getType(), babyPos);
        if (baby instanceof Animal animalBaby) {
            animalBaby.setBaby(true);
        }
        // Prevent baby from breeding immediately
        if (baby instanceof EntityIntelligent babyIntelligent) {
            babyIntelligent.getMemoryStorage().put(MemoryTypes.LAST_IN_LOVE_TIME, entity.getTick());
        }
        baby.spawn();
    }
}
