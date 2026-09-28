package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.entity.EntityIntelligent;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.ai.behaviorgroup.BehaviorGroup;
import org.cloudburstmc.api.entity.ai.memory.MemoryTypes;
import org.cloudburstmc.api.entity.passive.Sheep;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.data.DyeColor;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.server.entity.ai.behavior.BehaviorImpl;
import org.cloudburstmc.server.entity.ai.behaviorgroup.BehaviorGroupImpl;
import org.cloudburstmc.server.entity.ai.controller.FluctuateController;
import org.cloudburstmc.server.entity.ai.controller.LookController;
import org.cloudburstmc.server.entity.ai.controller.WalkController;
import org.cloudburstmc.server.entity.ai.evaluator.*;
import org.cloudburstmc.server.entity.ai.executor.*;
import org.cloudburstmc.server.entity.ai.route.finder.FlatAStarRouteFinder;
import org.cloudburstmc.server.entity.ai.route.posevaluator.WalkingPosEvaluator;
import org.cloudburstmc.server.entity.ai.sensor.NearestFeedingPlayerSensor;
import org.cloudburstmc.server.entity.ai.sensor.NearestPlayerSensor;
import org.cloudburstmc.server.level.Sound;

import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

import static org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes.COLOR;
import static org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag.SHEARED;
import static org.cloudburstmc.server.entity.ai.evaluator.LogicHelper.all;
import static org.cloudburstmc.server.entity.ai.evaluator.LogicHelper.any;

/**
 * Author: BeYkeRYkt Nukkit Project
 */
public class EntitySheep extends Animal implements Sheep {

    public static final int NETWORK_ID = 13;

    public EntitySheep(EntityType<Sheep> type, Location location) {
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
        if (isBaby()) {
            return 0.65f;
        }
        return 1.3f;
    }

    @Override
    public String getName() {
        return "Sheep";
    }

    @Override
    public void initEntity() {
        super.initEntity();
        this.setMaxHealth(8);
        this.data.set(COLOR, (byte) randomColor());
    }

    @Override
    public void loadAdditionalData(NbtMap tag) {
        super.loadAdditionalData(tag);

        //TODO: Kinda hacky but works for now
        tag.listenForByte("Color", this::setColor);
        tag.listenForBoolean("Sheared", this::setSheared);
    }

    @Override
    public void saveAdditionalData(NbtMapBuilder tag) {
        super.saveAdditionalData(tag);

        tag.putByte("Color", (byte) this.getColor().getWoolData());
        tag.putBoolean("Sheared", this.isSheared());
    }

    @Override
    public boolean onInteract(Player player, ItemStack item) {
        if (item.has(ItemDataComponents.COLOR)) {
            this.setColor(Objects.requireNonNull(item.get(ItemDataComponents.COLOR)));
            return true;
        }

        return item.getType() == ItemTypes.SHEARS && shear();
    }

    public boolean shear() {
        if (isSheared()) {
            return false;
        }

        this.setSheared(true);
        this.data.setFlag(SHEARED, true);
        this.level.addSound(this.getPosition(), Sound.MOB_SHEEP_SHEAR);

        ItemStack itemStack = ItemStack.builder(getWoolState(getColor()))
                .amount(ThreadLocalRandom.current().nextInt(2) + 1)
                .build();

        this.level.dropItem(this.getPosition(), itemStack);
        return true;
    }

    @Override
    public ItemStack[] getDrops() {
        if (this.lastDamageCause != null && this.lastDamageCause.getDamageSource().getCausingEntity() != null) {
            return new ItemStack[]{ItemStack.builder(getWoolState(getColor()))
                    .amount(1)
                    .build()};
        }
        return new ItemStack[0];
    }

    private static BlockState getWoolState(DyeColor color) {
        return switch (color) {
            case WHITE, NONE -> BlockStates.WHITE_WOOL;
            case ORANGE -> BlockStates.ORANGE_WOOL;
            case MAGENTA -> BlockStates.MAGENTA_WOOL;
            case LIGHT_BLUE -> BlockStates.LIGHT_BLUE_WOOL;
            case YELLOW -> BlockStates.YELLOW_WOOL;
            case LIME -> BlockStates.LIME_WOOL;
            case PINK -> BlockStates.PINK_WOOL;
            case GRAY -> BlockStates.GRAY_WOOL;
            case LIGHT_GRAY, SILVER -> BlockStates.LIGHT_GRAY_WOOL;
            case CYAN -> BlockStates.CYAN_WOOL;
            case PURPLE -> BlockStates.PURPLE_WOOL;
            case BLUE -> BlockStates.BLUE_WOOL;
            case BROWN -> BlockStates.BROWN_WOOL;
            case GREEN -> BlockStates.GREEN_WOOL;
            case RED -> BlockStates.RED_WOOL;
            case BLACK -> BlockStates.BLACK_WOOL;
        };
    }

    public boolean isSheared() {
        return this.data.getFlag(SHEARED);
    }

    public void setSheared(boolean sheared) {
        this.data.setFlag(SHEARED, sheared);
    }

    public DyeColor getColor() {
        return DyeColor.getByWoolData(this.data.get(COLOR));
    }

    public void setColor(DyeColor color) {
        this.data.set(COLOR, (byte) color.getWoolData());
    }

    private void setColor(int color) {
        this.data.set(COLOR, (byte) color);
    }

    private int randomColor() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double rand = random.nextDouble(1, 100);

        if (rand <= 0.164) {
            return DyeColor.PINK.getWoolData();
        }

        if (rand <= 15) {
            return random.nextBoolean() ? DyeColor.BLACK.getWoolData() : random.nextBoolean() ? DyeColor.GRAY.getWoolData() : DyeColor.LIGHT_GRAY.getWoolData();
        }

        return DyeColor.WHITE.getWoolData();
    }

    @Override
    public BehaviorGroup createBehaviorGroup() {
        return BehaviorGroupImpl.builder()
                .sensor(new NearestFeedingPlayerSensor(8))
                .sensor(new NearestPlayerSensor(8, 0, 20))
                // Core behavior: enter love mode when fed
                .coreBehavior(BehaviorImpl.builder()
                        .executor(new InLoveExecutor(400))
                        .evaluator(all(
                                entity -> !entity.getMemoryStorage().get(MemoryTypes.IS_IN_LOVE),
                                entity -> {
                                    var lastLoveTime = entity.getMemoryStorage().get(MemoryTypes.LAST_IN_LOVE_TIME);
                                    return lastLoveTime == null || lastLoveTime <= 0 || entity.getTick() - lastLoveTime >= 6000;
                                },
                                new PassByTimeEvaluator(MemoryTypes.LAST_BE_FEED_TIME, 0, 400)
                        ))
                        .priority(1)
                        .build())
                // Priority 6 (highest): flee when attacked
                .behavior(BehaviorImpl.builder()
                        .executor(new FlatRandomRoamExecutor(this.getMovementSpeed(), 12, 40, true, 100, true, 10))
                        .evaluator(new PassByTimeEvaluator(EntityIntelligent::getLastDamageTime, 0, 100))
                        .priority(6)
                        .build())
                // Priority 5: breed when in love
                .behavior(BehaviorImpl.builder()
                        .executor(new EntityBreedingExecutor(100, 0.23f))
                        .evaluator(entity -> entity.getMemoryStorage().get(MemoryTypes.IS_IN_LOVE))
                        .priority(5)
                        .build())
                // Priority 4: follow player holding wheat
                .behavior(BehaviorImpl.builder()
                        .executor(new FollowEntityExecutor(MemoryTypes.NEAREST_FEEDING_PLAYER, 0.2f, 64, 2.25))
                        .evaluator(new MemoryCheckNotEmptyEvaluator(MemoryTypes.NEAREST_FEEDING_PLAYER))
                        .priority(4)
                        .build())
                // Priority 3: eat grass
                .behavior(BehaviorImpl.builder()
                        .executor(new SheepEatGrassExecutor())
                        .evaluator(all(
                                any(
                                        all(
                                                entity -> entity instanceof Animal animal && animal.isBaby(),
                                                new ProbabilityEvaluator(86, 100)
                                        ),
                                        all(
                                                entity -> !(entity instanceof Animal animal && animal.isBaby()),
                                                new ProbabilityEvaluator(1, 100)
                                        )
                                ),
                                any(
                                        new BlockCheckEvaluator(BlockTypes.SHORT_GRASS, Vector3i.from(0, 0, 0)),
                                        new BlockCheckEvaluator(BlockTypes.GRASS_BLOCK, Vector3i.from(0, -1, 0))
                                )
                        ))
                        .priority(3)
                        .period(100)
                        .build())
                // Priority 2: look at nearest player
                .behavior(BehaviorImpl.builder()
                        .executor(new LookAtEntityExecutor(MemoryTypes.NEAREST_PLAYER, 100))
                        .evaluator(all(
                                new MemoryCheckNotEmptyEvaluator(MemoryTypes.NEAREST_PLAYER),
                                new ProbabilityEvaluator(2, 5)
                        ))
                        .priority(2)
                        .period(100)
                        .build())
                // Priority 1 (lowest): random wandering
                .behavior(BehaviorImpl
                        .builder()
                        .executor(new FlatRandomRoamExecutor(this.getMovementSpeed(), 12, 100, false, -1, true, 10))
                        .evaluator(entity -> true)
                        .priority(1)
                        .build())
                // Controllers
                .controller(new WalkController())
                .controller(new LookController(true, true))
                .controller(new FluctuateController())
                // Route finder
                .routeFinder(new FlatAStarRouteFinder(new WalkingPosEvaluator()))
                .build();
    }
}
