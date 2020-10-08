package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import lombok.val;
import org.cloudburstmc.server.block.*;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.BlockRegistry;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.Identifier;
import org.cloudburstmc.server.utils.data.TreeSpecies;

import java.util.EnumMap;
import java.util.Map;

public class BlockBehaviorLog extends BlockBehaviorSolid {

    protected static final Map<TreeSpecies, BlockType> STRIPPED_MAP = new EnumMap<>(TreeSpecies.class);

    static {
        STRIPPED_MAP.put(TreeSpecies.OAK, BlockTypes.STRIPPED_OAK_LOG);
        STRIPPED_MAP.put(TreeSpecies.SPRUCE, BlockTypes.STRIPPED_SPRUCE_LOG);
        STRIPPED_MAP.put(TreeSpecies.BIRCH, BlockTypes.STRIPPED_BIRCH_LOG);
        STRIPPED_MAP.put(TreeSpecies.JUNGLE, BlockTypes.STRIPPED_JUNGLE_LOG);
        STRIPPED_MAP.put(TreeSpecies.ACACIA, BlockTypes.STRIPPED_ACACIA_LOG);
        STRIPPED_MAP.put(TreeSpecies.DARK_OAK, BlockTypes.STRIPPED_DARK_OAK_LOG);
    }

    protected Identifier identifier = BlockTypes.LOG;


    public static void upgradeLegacyBlock(int[] blockState) {
        if ((blockState[1] & 0b1100) == 0b1100) { // old full bark texture
            blockState[0] = BlockRegistry.get().getLegacyId(BlockTypes.WOOD);
            blockState[1] = blockState[1] & 0x03; // gets only the log type and set pillar to y
        }
    }

    @Override
    public boolean canBeActivated(Block block) {
        return true;
    }

    @Override
    public boolean onActivate(Block block, ItemStack item, Player player) {
        val behavior = item.getBehavior();

        if (!behavior.isAxe() || !behavior.useOn(item, block)) {
            return false;
        }

        val state = block.getState();
        val stripped = STRIPPED_MAP.get(state.ensureTrait(BlockTraits.TREE_SPECIES));
        block.set(BlockState.get(stripped).copyTrait(BlockTraits.AXIS, state), true);
        return true;
    }

    @Override
    public boolean place(ItemStack item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        // Convert the old log bark to the new wood block
        placeBlock(block, item.getBehavior().getBlock(item).withTrait(BlockTraits.AXIS, face.getAxis()));
        return true;
    }


    @Override
    public BlockColor getColor(Block block) {
        switch (block.getState().ensureTrait(BlockTraits.TREE_SPECIES)) {
            default:
            case OAK:
                return BlockColor.WOOD_BLOCK_COLOR;
            case SPRUCE:
                return BlockColor.SPRUCE_BLOCK_COLOR;
            case BIRCH:
                return BlockColor.SAND_BLOCK_COLOR;
            case JUNGLE:
                return BlockColor.DIRT_BLOCK_COLOR;
        }
    }

    @Override
    public ItemStack toItem(Block block) {
        return ItemStack.get(block.getState().resetTrait(BlockTraits.AXIS));
    }
}
