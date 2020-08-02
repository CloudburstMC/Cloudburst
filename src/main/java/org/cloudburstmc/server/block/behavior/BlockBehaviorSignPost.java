package org.cloudburstmc.server.block.behavior;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.nukkitx.math.vector.Vector3f;
import lombok.val;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.blockentity.BlockEntityTypes;
import org.cloudburstmc.server.blockentity.Sign;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemIds;
import org.cloudburstmc.server.item.ItemTool;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.math.AxisAlignedBB;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.BlockEntityRegistry;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.Identifier;
import org.cloudburstmc.server.utils.data.CardinalDirection;

import static org.cloudburstmc.server.block.BlockTypes.AIR;

public class BlockBehaviorSignPost extends BlockBehaviorTransparent {

    private static final BiMap<Identifier, Identifier> STANDING_MAP = HashBiMap.create();
    private static final BiMap<Identifier, Identifier> WALL_MAP = HashBiMap.create();

    static {
        STANDING_MAP.put(ItemIds.SIGN, BlockTypes.STANDING_SIGN);
        STANDING_MAP.put(ItemIds.BIRCH_SIGN, BlockTypes.BIRCH_STANDING_SIGN);
        STANDING_MAP.put(ItemIds.SPRUCE_SIGN, BlockTypes.SPRUCE_STANDING_SIGN);
        STANDING_MAP.put(ItemIds.JUNGLE_SIGN, BlockTypes.JUNGLE_STANDING_SIGN);
        STANDING_MAP.put(ItemIds.ACACIA_SIGN, BlockTypes.ACACIA_STANDING_SIGN);
        STANDING_MAP.put(ItemIds.DARK_OAK_SIGN, BlockTypes.DARK_OAK_STANDING_SIGN);

        WALL_MAP.put(ItemIds.SIGN, BlockTypes.WALL_SIGN);
        WALL_MAP.put(ItemIds.BIRCH_SIGN, BlockTypes.BIRCH_WALL_SIGN);
        WALL_MAP.put(ItemIds.SPRUCE_SIGN, BlockTypes.SPRUCE_WALL_SIGN);
        WALL_MAP.put(ItemIds.JUNGLE_SIGN, BlockTypes.JUNGLE_WALL_SIGN);
        WALL_MAP.put(ItemIds.ACACIA_SIGN, BlockTypes.ACACIA_WALL_SIGN);
        WALL_MAP.put(ItemIds.DARK_OAK_SIGN, BlockTypes.DARK_OAK_WALL_SIGN);
    }

    @Override
    public float getHardness() {
        return 1;
    }

    @Override
    public float getResistance() {
        return 5;
    }

    @Override
    public boolean isSolid() {
        return false;
    }

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
    public boolean place(Item item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        if (face != Direction.DOWN) {
            if (face == Direction.UP) {
                val direction = player != null ? player.getCardinalDirection() : CardinalDirection.NORTH;

                placeBlock(block, BlockState.get(STANDING_MAP.get(item.getId())).withTrait(BlockTraits.CARDINAL_DIRECTION, direction));
            } else {
                placeBlock(block, BlockState.get(WALL_MAP.get(item.getId())).withTrait(BlockTraits.CARDINAL_DIRECTION, face.getCardinalDirection()));
            }

            Sign sign = BlockEntityRegistry.get().newEntity(BlockEntityTypes.SIGN, block);
            if (player != null && !item.hasNbtMap()) {
                sign.setTextOwner(player.getXuid());
            } else {
                sign.loadAdditionalData(item.getTag());
            }

            return true;
        }

        return false;
    }

    @Override
    public int getToolType() {
        return ItemTool.TYPE_AXE;
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.AIR_BLOCK_COLOR;
    }

    @Override
    public Item toItem(Block block) {
        val type = block.getState().getType();
        return Item.get(STANDING_MAP.inverse().getOrDefault(type, WALL_MAP.inverse().get(type)));
    }

    @Override
    public boolean canWaterlogSource() {
        return true;
    }
}
