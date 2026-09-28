package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.EntityAgeable;
import org.cloudburstmc.api.entity.EntityIntelligent;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.ai.behaviorgroup.BehaviorGroup;
import org.cloudburstmc.api.entity.ai.memory.MemoryTypes;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.server.container.view.CloudPlayerInventory;
import org.cloudburstmc.server.entity.CloudEntityIntelligent;
import org.cloudburstmc.server.entity.EntityCreature;
import org.cloudburstmc.server.entity.ai.behavior.BehaviorImpl;
import org.cloudburstmc.server.entity.ai.behaviorgroup.BehaviorGroupImpl;
import org.cloudburstmc.server.entity.ai.controller.FluctuateController;
import org.cloudburstmc.server.entity.ai.controller.LookController;
import org.cloudburstmc.server.entity.ai.controller.WalkController;
import org.cloudburstmc.server.entity.ai.evaluator.LogicHelper;
import org.cloudburstmc.server.entity.ai.evaluator.MemoryCheckNotEmptyEvaluator;
import org.cloudburstmc.server.entity.ai.evaluator.PassByTimeEvaluator;
import org.cloudburstmc.server.entity.ai.evaluator.ProbabilityEvaluator;
import org.cloudburstmc.server.entity.ai.executor.FlatRandomRoamExecutor;
import org.cloudburstmc.server.entity.ai.executor.LookAtEntityExecutor;
import org.cloudburstmc.server.entity.ai.route.finder.FlatAStarRouteFinder;
import org.cloudburstmc.server.entity.ai.route.posevaluator.WalkingPosEvaluator;
import org.cloudburstmc.server.entity.ai.sensor.NearestFeedingPlayerSensor;
import org.cloudburstmc.server.entity.ai.sensor.NearestPlayerSensor;

import java.util.Set;

import static org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag.BABY;

/**
 * Abstract base class for passive animals. Extends {@link EntityCreature} and implements {@link EntityAgeable}
 * for baby/adult state.
 */
public abstract class Animal extends CloudEntityIntelligent implements EntityAgeable {
    public Animal(EntityType<?> type, Location location) {
        super(type, location);
    }

    @Override
    public boolean isBaby() {
        return this.data.getFlag(BABY);
    }

    @Override
    public void setBaby(boolean baby) {
        this.data.setFlag(BABY, baby);
        this.recalculateBoundingBox();
    }

    public boolean isBreedingItem(ItemStack item) {
        return item.getType() == ItemTypes.WHEAT; //default
    }

    @Override
    public boolean onInteract(Player player, ItemStack item, Vector3f clickedPos) {
        if (super.onInteract(player, item, clickedPos)) {
            return true;
        }

        if (item.getType() == ItemTypes.NAME_TAG) {
            if (item.get(ItemDataComponents.CUSTOM_NAME) != null) {
                this.setNameTag(item.get(ItemDataComponents.CUSTOM_NAME));
                this.setNameTagVisible(true);
                ((CloudPlayerInventory) player.getInventory()).getContainer().removeItem(item);
                return true;
            }
        }
        return false;
    }

    @Override
    public BehaviorGroup createBehaviorGroup() {
        return BehaviorGroupImpl
                .builder()
                .sensor(new NearestFeedingPlayerSensor(8))
                .sensor(new NearestPlayerSensor(8, 0, 20))
                .behavior(BehaviorImpl.builder()
                        .executor(new LookAtEntityExecutor(MemoryTypes.NEAREST_PLAYER, 100))
                        .evaluator(LogicHelper.all(
                                new MemoryCheckNotEmptyEvaluator(MemoryTypes.NEAREST_PLAYER),
                                new ProbabilityEvaluator(2, 5)))
                        .priority(2)
                        .period(100)
                        .build())
                .behavior(BehaviorImpl.builder()
                        .executor(new FlatRandomRoamExecutor(this.getMovementSpeed(), 12, 40, true, 100, true, 10))
                        .evaluator(new PassByTimeEvaluator(EntityIntelligent::getLastDamageTime, 0, 100))
                        .priority(6)
                        .build())
                .behavior(BehaviorImpl.builder()
                        .executor(new FlatRandomRoamExecutor(this.getMovementSpeed(), 12, 100, false, -1, true, 10))
                        .evaluator(entity -> true)
                        .priority(1)
                        .build())
                .routeFinder(new FlatAStarRouteFinder(new WalkingPosEvaluator()))
                .controllers(Set.of(new LookController(true, true), new WalkController(), new FluctuateController()))
                .build();
    }
}
