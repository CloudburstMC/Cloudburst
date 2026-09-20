package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.ai.behaviorgroup.BehaviorGroup;
import org.cloudburstmc.api.entity.ai.memory.MemoryTypes;
import org.cloudburstmc.api.entity.passive.Pig;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.server.entity.ai.behavior.BehaviorImpl;
import org.cloudburstmc.server.entity.ai.behaviorgroup.BehaviorGroupImpl;
import org.cloudburstmc.server.entity.ai.controller.FluctuateController;
import org.cloudburstmc.server.entity.ai.controller.LookController;
import org.cloudburstmc.server.entity.ai.controller.WalkController;
import org.cloudburstmc.server.entity.ai.evaluator.LogicHelper;
import org.cloudburstmc.server.entity.ai.evaluator.MemoryCheckNotEmptyEvaluator;
import org.cloudburstmc.server.entity.ai.evaluator.ProbabilityEvaluator;
import org.cloudburstmc.server.entity.ai.executor.FlatRandomRoamExecutor;
import org.cloudburstmc.server.entity.ai.executor.LookAtEntityExecutor;
import org.cloudburstmc.server.entity.ai.route.finder.FlatAStarRouteFinder;
import org.cloudburstmc.server.entity.ai.route.posevaluator.WalkingPosEvaluator;

import java.util.Set;

/**
 * Author: BeYkeRYkt Nukkit Project
 */
public class EntityPig extends Animal implements Pig {

    public static final int NETWORK_ID = 12;

    public EntityPig(EntityType<Pig> type, Location location) {
        super(type, location);
    }

    @Override
    public float getWidth() {
        if (this.isBaby()) {
            return 0.45f;
        }
        return 0.9f;
    }

    @Override
    public float getHeight() {
        if (this.isBaby()) {
            return 0.45f;
        }
        return 0.9f;
    }

    @Override
    public void initEntity() {
        super.initEntity();
        this.setMaxHealth(10);
    }

    @Override
    public String getName() {
        return "Pig";
    }

    @Override
    public ItemStack[] getDrops() {
        return new ItemStack[] {ItemStack.builder().itemType(ItemTypes.PORKCHOP).build()};
    }

    @Override
    public boolean isBreedingItem(ItemStack item) {
        var id = item.getType();

        return id == ItemTypes.CARROT || id == ItemTypes.POTATO || id == ItemTypes.BEETROOT;
    }

    @Override
    public BehaviorGroup createBehaviorGroup() {
        return BehaviorGroupImpl.builder()
                .behavior(BehaviorImpl.builder()
                        .executor(new LookAtEntityExecutor(MemoryTypes.NEAREST_PLAYER, 100))
                        .evaluator(LogicHelper.all(
                                new MemoryCheckNotEmptyEvaluator(MemoryTypes.NEAREST_PLAYER),
                                new ProbabilityEvaluator(2, 5)))
                        .priority(2)
                        .period(100)
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
