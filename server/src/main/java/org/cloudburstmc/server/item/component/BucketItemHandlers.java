package org.cloudburstmc.server.item.component;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.entity.Bucketable;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.event.player.PlayerBucketEmptyEvent;
import org.cloudburstmc.api.event.player.PlayerBucketFillEvent;
import org.cloudburstmc.api.item.ItemKeys;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.item.component.UseOnHandler;
import org.cloudburstmc.api.item.data.BucketEntityData;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.block.component.LiquidBlockHandlers;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.level.Sound;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.EntityRegistry;

import java.util.Objects;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BucketItemHandlers {

    public static final UseOnHandler PICK_UP = (item, entity, position, face, click) -> {
        if (!(entity instanceof CloudPlayer player)) {
            return item;
        }

        CloudLevel level = player.getLevel();
        Block clicked = level.getBlock(position);
        ItemStack blockBucket = clicked.requireComponent(BlockComponents.BUCKET_PICKUP).execute(clicked, player);
        if (!blockBucket.isEmpty()) {
            ItemStack result = player.isCreative() ? item : blockBucket;
            PlayerBucketFillEvent event = new PlayerBucketFillEvent(player, clicked, clicked, face, item, result);
            level.getServer().getEventManager().fire(event);
            if (event.isCancelled()) {
                return item;
            }

            clicked.set(BlockStates.AIR);
            addCreativeFilledBucket(player, blockBucket);
            level.addSound(position, Sound.BUCKET_FILL_POWDER_SNOW);
            return event.getItem();
        }

        LiquidState liquid = clicked.getLiquid();
        if (!liquid.isSource()) {
            return item;
        }

        ItemType filled = isWater(liquid) ? ItemTypes.WATER_BUCKET : ItemTypes.LAVA_BUCKET;
        ItemStack filledBucket = ItemStack.from(filled);
        ItemStack result = player.isCreative() ? item : filledBucket;
        PlayerBucketFillEvent event = new PlayerBucketFillEvent(player, clicked, clicked, face, item, result);
        level.getServer().getEventManager().fire(event);
        if (event.isCancelled()) {
            return item;
        }

        if (!level.removeLiquid(position)) {
            return item;
        }

        addCreativeFilledBucket(player, filledBucket);
        level.addSound(position, isWater(liquid) ? Sound.BUCKET_FILL_WATER : Sound.BUCKET_FILL_LAVA);
        return event.getItem();
    };

    public static UseOnHandler place(BlockState state) {
        return place(state, null);
    }

    public static <T extends Entity> UseOnHandler placeEntity(BlockState state, EntityType<T> entityType) {
        Objects.requireNonNull(entityType, "entityType");
        return place(state, entityType);
    }

    public static <T extends Entity> UseOnHandler placeEntity(EntityType<T> entityType) {
        Objects.requireNonNull(entityType, "entityType");
        return (item, entity, position, face, click) -> {
            if (!(entity instanceof CloudPlayer player)) {
                return item;
            }

            CloudLevel level = player.getLevel();
            Block clicked = level.getBlock(position);
            Vector3i target = clicked.getState().isReplaceable() ? position : face.relative(position);
            if (!level.getBlockState(target).isReplaceable()) {
                return item;
            }

            ItemStack result = player.isCreative() ? item : ItemStack.from(ItemTypes.BUCKET);
            Block affected = level.getBlock(target);
            PlayerBucketEmptyEvent event = new PlayerBucketEmptyEvent(player, affected, clicked, face, item, result);
            level.getServer().getEventManager().fire(event);
            if (event.isCancelled()) {
                return item;
            }

            spawnBucketEntity(level, target, entityType, item);
            level.addSound(target, Sound.BUCKET_EMPTY_FISH);
            return event.getItem();
        };
    }

    public static UseOnHandler placePowderSnow(BlockState state) {
        Objects.requireNonNull(state, "state");
        return (item, entity, position, face, click) -> {
            if (!(entity instanceof CloudPlayer player)) {
                return item;
            }

            CloudLevel level = player.getLevel();
            Block clicked = level.getBlock(position);
            Block side = clicked.getSide(face);
            Block target = clicked.getState().isReplaceable() && clicked.getState().getType() != state.getType()
                    ? clicked : side;

            ItemStack result = player.isCreative() ? item : ItemStack.from(ItemTypes.BUCKET);
            PlayerBucketEmptyEvent event = new PlayerBucketEmptyEvent(player, target, clicked, face, item, result);
            level.getServer().getEventManager().fire(event);
            if (event.isCancelled()) {
                return item;
            }

            if (level.tryPlaceBlock(clicked, side, face, click, item, player, false) == null) {
                return item;
            }

            level.addSound(target.getPosition(), Sound.BUCKET_EMPTY_POWDER_SNOW);
            return event.getItem();
        };
    }

    private static UseOnHandler place(BlockState state, EntityType<?> entityType) {
        Objects.requireNonNull(state, "state");
        if (!state.getType().isLiquid()) {
            throw new IllegalArgumentException("Block state is not liquid: " + state);
        }

        return (item, entity, position, face, click) -> {
            if (!(entity instanceof CloudPlayer player)) {
                return item;
            }

            LiquidState liquid = LiquidState.of(state);
            CloudLevel level = player.getLevel();
            Block clicked = level.getBlock(position);
            Vector3i target = LiquidBlockHandlers.canOccupySecondaryLayer(liquid)
                    && clicked.getState().canContainLiquidSource() ? position : face.relative(position);
            if (!level.canSetLiquidState(target, liquid)) {
                return item;
            }

            ItemStack result = player.isCreative() ? item : ItemStack.from(ItemTypes.BUCKET);
            Block affected = level.getBlock(target);
            PlayerBucketEmptyEvent event = new PlayerBucketEmptyEvent(player, affected, clicked, face, item, result);
            level.getServer().getEventManager().fire(event);
            if (event.isCancelled()) {
                return item;
            }

            if (isWater(liquid) && level.getDimension() == CloudLevel.DIMENSION_NETHER) {
                if (entityType != null) {
                    spawnBucketEntity(level, target, entityType, item);
                }

                level.addSound(target, Sound.RANDOM_FIZZ, 0.5f, 2.6f);
                return event.getItem();
            }

            if (!level.setLiquidState(target, liquid)) {
                return item;
            }

            if (entityType != null) {
                spawnBucketEntity(level, target, entityType, item);
            }

            level.addSound(target, entityType != null ? Sound.BUCKET_EMPTY_FISH
                    : isWater(liquid) ? Sound.BUCKET_EMPTY_WATER : Sound.BUCKET_EMPTY_LAVA);
            return event.getItem();
        };
    }

    private static <T extends Entity> void spawnBucketEntity(CloudLevel level, Vector3i position, EntityType<T> type, ItemStack bucket) {
        T spawned = EntityRegistry.get().newEntity(type, Location.from(position.toFloat().add(0.5f, 0.5f, 0.5f), level));
        if (spawned instanceof Bucketable bucketable) {
            bucketable.setFromBucket(true);
        }

        BucketEntityData data = bucket.get(ItemKeys.BUCKET_ENTITY_DATA);
        if (data != null) {
            spawned.setHealth(data.health());
            if (spawned instanceof CloudEntity cloudEntity) {
                cloudEntity.setInvulnerable(data.invulnerable());
                cloudEntity.setImmobile(data.immobile());
            }
        }

        String customName = bucket.get(ItemKeys.CUSTOM_NAME);
        if (customName != null) {
            spawned.setNameTag(customName);
        }
        spawned.spawnToAll();
    }

    private static void addCreativeFilledBucket(CloudPlayer player, ItemStack filledBucket) {
        if (player.isCreative() && !player.getInventory().contains(filledBucket)) {
            player.getInventory().addItem(filledBucket);
        }
    }

    private static boolean isWater(LiquidState state) {
        return state.getType().isSameFamily(LiquidTypes.WATER);
    }
}
