package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import lombok.val;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.TierTypes;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.data.BlockColor;
import org.cloudburstmc.server.inventory.CloudAnvilInventory;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.level.Sound;
import org.cloudburstmc.server.network.protocol.types.ContainerIds;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import static org.cloudburstmc.api.block.BlockTypes.SNOW_LAYER;

public class BlockBehaviorAnvil extends BlockBehaviorFallable {

    @Override
    public boolean canBeActivated(Block block) {
        return true;
    }

    @Override
    public boolean place(ItemStack item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        val state = block.getState();
        if (!target.getState().getBehavior().isTransparent(state) || state.getType() == SNOW_LAYER) {
            BlockState anvil = item.getBehavior().getBlock(item)
                    .withTrait(BlockTraits.DIRECTION, player.getDirection().getOpposite());

            block.set(anvil, true);
            ((CloudLevel) block.getLevel()).addSound(block.getPosition().toFloat(), Sound.RANDOM_ANVIL_LAND, 1, 0.8F);
            return true;
        }
        return false;
    }

    @Override
    public boolean onActivate(Block block, ItemStack item, Player player) {
        if (player != null) {
            player.addWindow(new CloudAnvilInventory((CloudPlayer) player), ContainerIds.ANVIL);
        }
        return true;
    }

    @Override
    public ItemStack toItem(Block block) {
        BlockState state = block.getState();

        return CloudItemRegistry.get().getItem(
                state.getType().getDefaultState().withTrait(BlockTraits.DAMAGE, state.ensureTrait(BlockTraits.DAMAGE))
        );
    }

    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        if (hand.getBehavior().isPickaxe() && hand.getBehavior().getTier(hand).compareTo(TierTypes.WOOD) >= 0) {
            return new ItemStack[]{
                    this.toItem(block)
            };
        }
        return new ItemStack[0];
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.IRON_BLOCK_COLOR;
    }


}
