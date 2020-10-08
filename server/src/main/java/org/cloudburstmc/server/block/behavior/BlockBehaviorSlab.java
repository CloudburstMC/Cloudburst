package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.SoundEvent;
import com.nukkitx.protocol.bedrock.packet.LevelSoundEvent2Packet;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.cloudburstmc.server.block.*;
import org.cloudburstmc.server.block.trait.BlockTrait;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.Identifier;
import org.cloudburstmc.server.utils.data.StoneSlabType;

import java.util.EnumMap;

@RequiredArgsConstructor
public class BlockBehaviorSlab extends BlockBehaviorTransparent {

    static final EnumMap<StoneSlabType, BlockColor> COLORS = new EnumMap<>(StoneSlabType.class);

    static {
        COLORS.put(StoneSlabType.SMOOTH_STONE, BlockColor.STONE_BLOCK_COLOR);
        COLORS.put(StoneSlabType.SANDSTONE, BlockColor.SAND_BLOCK_COLOR);
        COLORS.put(StoneSlabType.WOOD, BlockColor.WOOD_BLOCK_COLOR);
        COLORS.put(StoneSlabType.COBBLESTONE, BlockColor.STONE_BLOCK_COLOR);
        COLORS.put(StoneSlabType.BRICK, BlockColor.STONE_BLOCK_COLOR);
        COLORS.put(StoneSlabType.STONE_BRICK, BlockColor.STONE_BLOCK_COLOR);
        COLORS.put(StoneSlabType.QUARTZ, BlockColor.QUARTZ_BLOCK_COLOR);
        COLORS.put(StoneSlabType.NETHER_BRICK, BlockColor.NETHERRACK_BLOCK_COLOR);
        COLORS.put(StoneSlabType.RED_SANDSTONE, BlockColor.ORANGE_BLOCK_COLOR);
        COLORS.put(StoneSlabType.PURPUR, BlockColor.PURPLE_BLOCK_COLOR);
        COLORS.put(StoneSlabType.PRISMARINE_ROUGH, BlockColor.CYAN_BLOCK_COLOR);
        COLORS.put(StoneSlabType.PRISMARINE_DARK, BlockColor.CYAN_BLOCK_COLOR);
        COLORS.put(StoneSlabType.PRISMARINE_BRICK, BlockColor.CYAN_BLOCK_COLOR);
        COLORS.put(StoneSlabType.MOSSY_COBBLESTONE, BlockColor.STONE_BLOCK_COLOR);
        COLORS.put(StoneSlabType.SMOOTH_SANDSTONE, BlockColor.SAND_BLOCK_COLOR);
        COLORS.put(StoneSlabType.RED_NETHER_BRICK, BlockColor.NETHERRACK_BLOCK_COLOR);
        COLORS.put(StoneSlabType.END_STONE_BRICK, BlockColor.WHITE_BLOCK_COLOR);
        COLORS.put(StoneSlabType.SMOOTH_RED_SANDSTONE, BlockColor.ORANGE_BLOCK_COLOR);
        COLORS.put(StoneSlabType.POLISHED_ANDESITE, BlockColor.STONE_BLOCK_COLOR);
        COLORS.put(StoneSlabType.ANDESITE, BlockColor.STONE_BLOCK_COLOR);
        COLORS.put(StoneSlabType.DIORITE, BlockColor.WHITE_BLOCK_COLOR);
        COLORS.put(StoneSlabType.POLISHED_DIORITE, BlockColor.WHITE_BLOCK_COLOR);
        COLORS.put(StoneSlabType.GRANITE, BlockColor.PINK_BLOCK_COLOR);
        COLORS.put(StoneSlabType.POLISHED_GRANITE, BlockColor.PINK_BLOCK_COLOR);
        COLORS.put(StoneSlabType.MOSSY_STONE_BRICK, BlockColor.STONE_BLOCK_COLOR);
        COLORS.put(StoneSlabType.SMOOTH_QUARTZ, BlockColor.QUARTZ_BLOCK_COLOR);
        COLORS.put(StoneSlabType.STONE, BlockColor.STONE_BLOCK_COLOR);
        COLORS.put(StoneSlabType.CUT_SANDSTONE, BlockColor.SAND_BLOCK_COLOR);
        COLORS.put(StoneSlabType.CUT_RED_SANDSTONE, BlockColor.ORANGE_BLOCK_COLOR);
    }

    protected final BlockType type;
    protected final BlockType doubleSlabType;

    @Override
    public BlockColor getColor(Block block) {
        StoneSlabType type = block.getState().ensureTrait(BlockTraits.STONE_SLAB_TYPE);
        return COLORS.get(type);
    }

//    @Override //TODO: bounding box
//    public float getMinY() {
//        return this.isTopSlab() ? (this.getY() + 0.5f) : this.getY();
//    }
//
//    @Override
//    public float getMaxY() {
//        return this.isTopSlab() ? (this.getY() + 1f) : (this.getY() + 0.5f);
//    }




    @Override
    public ItemStack toItem(Block block) {
        return ItemStack.get(block.getState().resetTrait(BlockTraits.IS_TOP_SLOT).getType());
    }

    @Override
    public boolean place(ItemStack item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        boolean isTop;

        val state = item.getBehavior().getBlock(item);
        val blockState = block.getState();
        val targetState = target.getState();

        if (face == Direction.DOWN) {
            if (checkSlab(state, targetState) && targetState.ensureTrait(BlockTraits.IS_TOP_SLOT)) {
                if (placeBlock(target, getDoubleSlab(state))) {
                    playDoublePlaceSound(target);
                    return true;
                }

                return false;
            } else if (checkSlab(state, blockState) && !blockState.ensureTrait(BlockTraits.IS_TOP_SLOT)) {
                if (placeBlock(block, getDoubleSlab(state))) {
                    playDoublePlaceSound(block);
                    return true;
                }

                return false;
            } else {
                isTop = true;
            }
        } else if (face == Direction.UP) {
            if (checkSlab(state, targetState) && !targetState.ensureTrait(BlockTraits.IS_TOP_SLOT)) {
                if (placeBlock(target, getDoubleSlab(state))) {
                    playDoublePlaceSound(target);
                    return true;
                }
                return false;
            } else if (checkSlab(state, blockState) && blockState.ensureTrait(BlockTraits.IS_TOP_SLOT)) {
                if (placeBlock(block, getDoubleSlab(state))) {
                    playDoublePlaceSound(block);
                    return true;
                }
                return false;
            } else {
                isTop = false;
            }
        } else { // Horizontal face
            isTop = clickPos.getY() >= 0.5f;
            val isBlockTop = blockState.ensureTrait(BlockTraits.IS_TOP_SLOT);
            if (checkSlab(state, blockState)
                    && ((isTop && !isBlockTop)
                    || (!isTop && isBlockTop))) {

                if (placeBlock(block, getDoubleSlab(state))) {
                    playDoublePlaceSound(block);
                    return true;
                }

                return false;
            }
        }

        if (blockState.inCategory(BlockCategory.SLAB) && !checkSlab(state, targetState)) {
            return false;
        }

        return placeBlock(block, getState(state, this.type).withTrait(BlockTraits.IS_TOP_SLOT, isTop));
    }

    private BlockState getDoubleSlab(BlockState state) {
        return getState(state, this.doubleSlabType);
    }

    private BlockState getState(BlockState state, Identifier type) {
        BlockTrait trait = getSlabType(state);
        return BlockState.get(type).withTrait(trait, state.ensureTrait(trait));
    }

    private boolean checkSlab(BlockState state, BlockState other) {
        if (state.getType() != other.getType()) {
            return false;
        }

        BlockTrait<?> type = getSlabType(state);

        return state.ensureTrait(type) == other.ensureTrait(type);
    }

    private BlockTrait<?> getSlabType(BlockState state) {
        BlockTrait<?> type;
        if (state.getType() == BlockTypes.WOODEN_SLAB) {
            type = BlockTraits.TREE_SPECIES;
        } else {
            type = BlockTraits.STONE_SLAB_TYPE;
        }

        return type;
    }


    protected void playDoublePlaceSound(Block block) {
        LevelSoundEvent2Packet pk = new LevelSoundEvent2Packet();
        pk.setSound(SoundEvent.ITEM_USE_ON);
        pk.setExtraData(725); // Who knows what this means? It's what is sent per ProxyPass
        pk.setPosition(block.getPosition().toFloat().add(0.5f, 0.5f, 0.5f));
        pk.setIdentifier("");
        pk.setBabySound(false);
        pk.setRelativeVolumeDisabled(false);


        block.getLevel().addChunkPacket(block.getPosition(), pk);
    }
}
