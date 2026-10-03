package org.cloudburstmc.server.item.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockComponents;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.event.block.BlockIgniteCause;
import org.cloudburstmc.api.event.block.BlockIgniteEvent;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.component.UseOnHandler;
import org.cloudburstmc.api.level.sound.SoundType;
import org.cloudburstmc.api.level.sound.SoundTypes;
import org.cloudburstmc.server.block.component.FireBlockHandlers;
import org.cloudburstmc.server.block.component.TntBlockHandlers;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.level.NetherPortals;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.function.BiFunction;

@UtilityClass
public class IgnitionItemHandlers {

    public static final UseOnHandler FLINT_AND_STEEL_USE_ON = useOn(
            BlockIgniteCause.FLINT_AND_STEEL, SoundTypes.FIRE_IGNITE,
            (item, player) -> DefaultItemHandlers.ON_DAMAGE.execute(item, 1, player));

    public static final UseOnHandler FIRE_CHARGE_USE_ON = useOn(
            BlockIgniteCause.FIREBALL, SoundTypes.ITEM_FIRECHARGE_USE,
            (item, player) -> item.decreaseCount());

    private static UseOnHandler useOn(BlockIgniteCause cause, SoundType sound, BiFunction<ItemStack, CloudPlayer, ItemStack> consume) {
        return (item, entity, blockPosition, face, clickPosition) -> {
            if (!(entity instanceof CloudPlayer player)) {
                return item;
            }

            CloudLevel level = player.getLevel();
            Block clicked = level.getBlock(blockPosition);
            if (TntBlockHandlers.isTnt(clicked)) {
                return TntBlockHandlers.ignite(clicked, player, item);
            }

            BlockState ignited = level.getServer().getBlockRegistry()
                    .requireComponent(clicked.getState().getType(), BlockComponents.GET_IGNITED_STATE).execute(clicked);
            Block target = ignited == null ? level.getBlock(face.relative(blockPosition)) : clicked;
            if (ignited == null && target.getState().getType() != BlockTypes.AIR) {
                return item;
            }

            BlockState result = ignited == null ? FireBlockHandlers.placementState(target) : ignited;
            if (ignited == null && !level.getServer().getBlockRegistry()
                    .requireComponent(result.getType(), BlockComponents.CAN_SURVIVE).execute(target)
                    && NetherPortals.detect(level, target.getPosition()).isEmpty()) {
                return item;
            }

            BlockIgniteEvent event = new BlockIgniteEvent(target, cause, player, null);
            level.getServer().getEventManager().fire(event);
            if (event.isCancelled()) {
                return item;
            }

            if (!level.setBlockState(target.getPosition(), result, false, true)) {
                return item;
            }

            level.playSound(target.getPosition(), sound);
            if (ignited == null) {
                NetherPortals.detect(level, target.getPosition()).ifPresent(frame -> frame.fill(level));
            }

            return player.isCreative() ? item : consume.apply(item, player);
        };
    }
}
