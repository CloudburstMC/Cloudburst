package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.block.component.SurviveBlockHandler;
import org.cloudburstmc.api.block.component.UseBlockHandler;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.level.sound.SoundTypes;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.server.block.util.BlockSupport;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.Map;

@UtilityClass
public class CakeBlockHandlers {

    public static final SurviveBlockHandler CAN_SURVIVE = block ->
            BlockSupport.isSolidSupport(block.getLevel().getServer().getBlockRegistry(), block.getRelativeState(0, -1, 0));

    public static UseBlockHandler cake(Map<ItemType, BlockType> candleCakes) {
        return (block, player, direction, item) -> {
            if (!(player instanceof CloudPlayer cloudPlayer) || player.isSneaking() && !item.isEmpty()) {
                return false;
            }

            int bites = block.getState().ensureTrait(BlockTraits.BITE_COUNTER);
            BlockType candleCake = candleCakes.get(item.getType());
            if (bites == 0 && candleCake != null) {
                block.set(candleCake.getDefaultState());
                block.getLevel().playSound(block.getPosition(), SoundTypes.CAKE_ADD_CANDLE);

                if (!cloudPlayer.isCreative()) {
                    cloudPlayer.getInventory().setSelectedItem(item.decreaseCount());
                }

                return true;
            }

            return eatSlice(block, player, false);
        };
    }

    public static UseBlockHandler candleCake(ItemType candle) {
        return (block, player, direction, item) -> {
            if (!eatSlice(block, player, true)) {
                return false;
            }

            block.getLevel().dropItem(block.getPosition().toFloat().add(0.5f, 0.5f, 0.5f), ItemStack.from(candle));
            return true;
        };
    }

    private static boolean eatSlice(Block block, Player player, boolean candleCake) {
        if (!(player instanceof CloudPlayer cloudPlayer) || !cloudPlayer.isCreative() && cloudPlayer.getFoodData().isFull()) {
            return false;
        }

        cloudPlayer.getFoodData().eat(2, 0.4f);
        block.getLevel().playSound(block.getPosition(), SoundTypes.RANDOM_BURP);
        if (candleCake) {
            block.set(BlockStates.CAKE.withTrait(BlockTraits.BITE_COUNTER, 1));
        } else {
            int bites = block.getState().ensureTrait(BlockTraits.BITE_COUNTER);
            block.set(bites < 6 ? block.getState().withTrait(BlockTraits.BITE_COUNTER, bites + 1) : BlockStates.AIR);
        }

        return true;
    }
}
