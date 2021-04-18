package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.blockentity.BlockEntityTypes;
import org.cloudburstmc.api.blockentity.Sign;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.data.BlockColor;
import org.cloudburstmc.api.util.data.CardinalDirection;
import org.cloudburstmc.api.util.data.TreeSpecies;
import org.cloudburstmc.server.blockentity.SignBlockEntity;
import org.cloudburstmc.server.item.CloudItemStack;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.registry.BlockEntityRegistry;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import static org.cloudburstmc.api.block.BlockTypes.AIR;

public class BlockBehaviorSignPost extends BlockBehaviorTransparent {

    @Override
    public int onUpdate(Block block, int type) {
        if (type == CloudLevel.BLOCK_UPDATE_NORMAL) {
            if (block.down().getState().getType() == AIR) {
                block.getLevel().useBreakOn(block.getPosition());

                return CloudLevel.BLOCK_UPDATE_NORMAL;
            }
        }

        return 0;
    }

    @Override
    public boolean place(ItemStack item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        if (face != Direction.DOWN) {
            TreeSpecies woodType = item.getMetadata(TreeSpecies.class);

            if (face == Direction.UP) {
                var direction = player != null ? player.getCardinalDirection() : CardinalDirection.NORTH;

                placeBlock(block, BlockStates.STANDING_SIGN.withTrait(BlockTraits.TREE_SPECIES, woodType).withTrait(BlockTraits.CARDINAL_DIRECTION, direction));
            } else {
                placeBlock(block, BlockStates.WALL_SIGN.withTrait(BlockTraits.TREE_SPECIES, woodType).withTrait(BlockTraits.FACING_DIRECTION, face));
            }

            Sign sign = BlockEntityRegistry.get().newEntity(BlockEntityTypes.SIGN, block);
            var cloudItem = (CloudItemStack) item;

            if (cloudItem.getDataTag().isEmpty()) {
                if (player != null) {
                    sign.setTextOwner(player.getXuid());
                }
            } else {
                ((SignBlockEntity) sign).loadAdditionalData(cloudItem.getDataTag());
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
        return CloudItemRegistry.get().getItem(ItemTypes.SIGN).withData(block.getState().ensureTrait(BlockTraits.TREE_SPECIES));
    }


}
