package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockIds;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.data.BlockColor;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.registry.CloudItemRegistry;

public class BlockBehaviorLog extends BlockBehaviorSolid {

//    protected static final Map<TreeSpecies, BlockType> STRIPPED_MAP = new EnumMap<>(TreeSpecies.class);
//
//    static {
//        STRIPPED_MAP.put(TreeSpecies.OAK, BlockTypes.STRIPPED_OAK_LOG);
//        STRIPPED_MAP.put(TreeSpecies.SPRUCE, BlockTypes.STRIPPED_SPRUCE_LOG);
//        STRIPPED_MAP.put(TreeSpecies.BIRCH, BlockTypes.STRIPPED_BIRCH_LOG);
//        STRIPPED_MAP.put(TreeSpecies.JUNGLE, BlockTypes.STRIPPED_JUNGLE_LOG);
//        STRIPPED_MAP.put(TreeSpecies.ACACIA, BlockTypes.STRIPPED_ACACIA_LOG);
//        STRIPPED_MAP.put(TreeSpecies.DARK_OAK, BlockTypes.STRIPPED_DARK_OAK_LOG);
//    }
//
//    protected Identifier identifier = BlockTypes.LOG;


    public static void upgradeLegacyBlock(int[] blockState) {
        if ((blockState[1] & 0b1100) == 0b1100) { // old full bark texture
            blockState[0] = CloudBlockRegistry.get().getLegacyId(BlockIds.WOOD);
            blockState[1] = blockState[1] & 0x03; // gets only the log type and set pillar to y
        }
    }

    @Override
    public boolean canBeActivated(Block block) {
        return true;
    }

    @Override
    public boolean onActivate(Block block, ItemStack item, Player player) {
        var behavior = item.getBehavior();
        var state = block.getState();

        if (state.ensureTrait(BlockTraits.IS_STRIPPED) || !behavior.isAxe() || behavior.useOn(item, state) == null) {
            return false;
        }

        block.set(state.withTrait(BlockTraits.IS_STRIPPED, true), true);
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
        return switch (block.getState().ensureTrait(BlockTraits.TREE_SPECIES)) {
            case OAK -> BlockColor.WOOD_BLOCK_COLOR;
            case SPRUCE -> BlockColor.SPRUCE_BLOCK_COLOR;
            case BIRCH -> BlockColor.SAND_BLOCK_COLOR;
            case JUNGLE -> BlockColor.DIRT_BLOCK_COLOR;
            case MANGROVE -> BlockColor.RED_BLOCK_COLOR; //TODO ?
            default -> BlockColor.WOOD_BLOCK_COLOR;
        };
    }

    @Override
    public ItemStack toItem(Block block) {
        return CloudItemRegistry.get().getItem(block.getState().withTrait(BlockTraits.AXIS, BlockTraits.AXIS.getDefaultValue()));
    }
}
