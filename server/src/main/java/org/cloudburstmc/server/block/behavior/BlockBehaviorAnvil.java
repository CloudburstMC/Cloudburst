package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import lombok.val;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockIds;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.inventory.AnvilInventory;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemTool;
import org.cloudburstmc.server.level.Sound;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.network.protocol.types.ContainerIds;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.BlockRegistry;
import org.cloudburstmc.server.registry.ItemRegistry;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.data.AnvilDamage;

import static org.cloudburstmc.server.block.BlockIds.SNOW_LAYER;

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
    public float getHardness() {
        return 5;
    }

    @Override
    public float getResistance() {
        return 6000;
    }

    @Override
    public int getToolType() {
        return ItemTool.TYPE_PICKAXE;
    }

    @Override
    public boolean place(Item item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        val state = block.getState();
        if (!target.getState().getBehavior().isTransparent() || state.getType() == SNOW_LAYER) {
            BlockState anvil = BlockRegistry.get().getBlock(BlockIds.ANVIL)
                    .withTrait(BlockTraits.DIRECTION, player.getDirection().getOpposite());

            int meta = item.getMeta();
            if (meta >= 4 && meta <= 7) {
                anvil = anvil.withTrait(BlockTraits.DAMAGE, AnvilDamage.SLIGHTLY_DAMAGED);
            } else if (meta >= 8 && meta <= 11) {
                anvil = anvil.withTrait(BlockTraits.DAMAGE, AnvilDamage.VERY_DAMAGED);
            }

            block.set(anvil, true);
            block.getLevel().addSound(block.getPosition().toFloat(), Sound.RANDOM_ANVIL_LAND, 1, 0.8F);
            return true;
        }
        return false;
    }

    @Override
    public boolean onActivate(Block block, Item item, Player player) {
        if (player != null) {
            player.addWindow(new AnvilInventory(player.getUIInventory(), block), ContainerIds.ANVIL);
        }
        return true;
    }

    @Override
    public Item toItem(Block block) {
        BlockState state = block.getState();

        return ItemRegistry.get().getItem(
                state.defaultState().withTrait(BlockTraits.DAMAGE, state.ensureTrait(BlockTraits.DAMAGE))
        );
    }

    @Override
    public Item[] getDrops(Block block, Item hand) {
        if (hand.isPickaxe() && hand.getTier() >= ItemTool.TIER_WOODEN) {
            return new Item[]{
                    this.toItem(block)
            };
        }
        return new Item[0];
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
