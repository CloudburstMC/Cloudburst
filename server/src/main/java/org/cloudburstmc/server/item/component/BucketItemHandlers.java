package org.cloudburstmc.server.item.component;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.LiquidState;
import org.cloudburstmc.api.event.player.PlayerBucketEmptyEvent;
import org.cloudburstmc.api.event.player.PlayerBucketFillEvent;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.item.component.UseOnHandler;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.block.component.LiquidBlockHandlers;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.level.Sound;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.Objects;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BucketItemHandlers {

    public static final UseOnHandler PICK_UP = (item, entity, position, face, click) -> {
        if (!(entity instanceof CloudPlayer player)) {
            return item;
        }

        CloudLevel level = player.getLevel();
        LiquidState liquid = level.getBlock(position).getLiquid();
        if (!liquid.isSource()) {
            return item;
        }

        ItemType filled = isWater(liquid) ? ItemTypes.WATER_BUCKET : ItemTypes.LAVA_BUCKET;
        ItemStack result = player.isCreative() ? item : ItemStack.from(filled);
        PlayerBucketFillEvent event = new PlayerBucketFillEvent(player, level.getBlockState(position), face, item, result);
        level.getServer().getEventManager().fire(event);
        if (event.isCancelled()) {
            return item;
        }

        if (!level.removeLiquid(position)) {
            return item;
        }

        level.addSound(position, isWater(liquid) ? Sound.BUCKET_FILL_WATER : Sound.BUCKET_FILL_LAVA);
        return event.getItem();
    };

    public static UseOnHandler place(BlockState state) {
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
            PlayerBucketEmptyEvent event = new PlayerBucketEmptyEvent(player, clicked, face, item, result);
            level.getServer().getEventManager().fire(event);
            if (event.isCancelled()) {
                return item;
            }

            if (isWater(liquid) && level.getDimension() == CloudLevel.DIMENSION_NETHER) {
                level.addSound(target, Sound.RANDOM_FIZZ, 0.5f, 2.6f);
                return event.getItem();
            }

            if (!level.setLiquidState(target, liquid)) {
                return item;
            }

            level.addSound(target, isWater(liquid) ? Sound.BUCKET_EMPTY_WATER : Sound.BUCKET_EMPTY_LAVA);
            return event.getItem();
        };
    }

    private static boolean isWater(LiquidState state) {
        return state.getType().isSameFamily(org.cloudburstmc.api.block.LiquidTypes.WATER);
    }
}
