package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import lombok.val;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.inventory.AnvilInventory;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.TierTypes;
import org.cloudburstmc.server.item.ToolType;
import org.cloudburstmc.server.item.ToolTypes;
import org.cloudburstmc.server.level.Sound;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.network.protocol.types.ContainerIds;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.CloudItemRegistry;
import org.cloudburstmc.server.utils.BlockColor;

import static org.cloudburstmc.server.block.BlockTypes.SNOW_LAYER;

public class BlockBehaviorAnvil extends BlockBehaviorFallable {

    @Override
    public boolean canBeActivated(Block block) {
        return true;
    }

    @Override
    public boolean isTransparent() {
        return true;
    }

    @Override
    public float getResistance() {
        return 6000;
    }

    @Override
    public ToolType getToolType(BlockState state) {
        return ToolTypes.PICKAXE;
    }

    @Override
    public boolean place(ItemStack item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        val state = block.getState();
        if (!target.getState().getBehavior().isTransparent() || state.getType() == SNOW_LAYER) {
            BlockState anvil = item.getBehavior().getBlock(item)
                    .withTrait(BlockTraits.DIRECTION, player.getDirection().getOpposite());

            block.set(anvil, true);
            block.getLevel().addSound(block.getPosition().toFloat(), Sound.RANDOM_ANVIL_LAND, 1, 0.8F);
            return true;
        }
        return false;
    }

    @Override
    public boolean onActivate(Block block, ItemStack item, Player player) {
        if (player != null) {
            player.addWindow(new AnvilInventory(player.getUIInventory(), block), ContainerIds.ANVIL);
        }
        return true;
    }

    @Override
    public ItemStack toItem(Block block) {
        BlockState state = block.getState();

        return CloudItemRegistry.get().getItem(
                state.defaultState().withTrait(BlockTraits.DAMAGE, state.ensureTrait(BlockTraits.DAMAGE))
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

    @Override
    public boolean canHarvestWithHand() {
        return false;
    }

    @Override
    public boolean canWaterlogSource() {
        return true;
    }
}
