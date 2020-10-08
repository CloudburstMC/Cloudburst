package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import lombok.val;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockStates;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.blockentity.BlockEntityTypes;
import org.cloudburstmc.server.blockentity.Sign;
import org.cloudburstmc.server.item.CloudItemStack;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.ItemTypes;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.math.AxisAlignedBB;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.BlockEntityRegistry;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.data.CardinalDirection;
import org.cloudburstmc.server.utils.data.TreeSpecies;

import static org.cloudburstmc.server.block.BlockTypes.AIR;

public class BlockBehaviorSignPost extends BlockBehaviorTransparent {

    @Override
    public AxisAlignedBB getBoundingBox() {
        return null;
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == Level.BLOCK_UPDATE_NORMAL) {
            if (block.down().getState().getType() == AIR) {
                block.getLevel().useBreakOn(block.getPosition());

                return Level.BLOCK_UPDATE_NORMAL;
            }
        }

        return 0;
    }

    @Override
    public boolean place(ItemStack item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        if (face != Direction.DOWN) {
            TreeSpecies woodType = item.getMetadata(TreeSpecies.class);

            if (face == Direction.UP) {
                val direction = player != null ? player.getCardinalDirection() : CardinalDirection.NORTH;

                placeBlock(block, BlockStates.STANDING_SIGN.withTrait(BlockTraits.TREE_SPECIES, woodType).withTrait(BlockTraits.CARDINAL_DIRECTION, direction));
            } else {
                placeBlock(block, BlockStates.WALL_SIGN.withTrait(BlockTraits.TREE_SPECIES, woodType).withTrait(BlockTraits.FACING_DIRECTION, face));
            }

            Sign sign = BlockEntityRegistry.get().newEntity(BlockEntityTypes.SIGN, block);
            val cloudItem = (CloudItemStack) item;

            if (cloudItem.getDataTag().isEmpty()) {
                if (player != null) {
                    sign.setTextOwner(player.getXuid());
                }
            } else {
                sign.loadAdditionalData(cloudItem.getDataTag());
            }

            return true;
        }

        return false;
    }


    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.AIR_BLOCK_COLOR;
    }

    @Override
    public ItemStack toItem(Block block) {
        return ItemStack.get(ItemTypes.SIGN).withData(block.getState().ensureTrait(BlockTraits.TREE_SPECIES));
    }


}
