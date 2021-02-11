package org.cloudburstmc.api.block.behavior;

import lombok.val;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.TierType;
import org.cloudburstmc.api.item.ToolType;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.AxisAlignedBB;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.data.BlockColor;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;

import static org.cloudburstmc.api.block.BlockStates.AIR;

public abstract class BlockBehavior {

    public boolean canHarvestWithHand(BlockState state) {  //used for calculating breaking time
        val type = state.getType();
        return type.isDiggable() && type.getToolType() == null && type.getTierType() == null;
    }

    public boolean isBreakable(BlockState state, ItemStack item) {
        return state.getType().isDiggable();
    }

    public int tickRate() {
        return 10;
    }

    public int onUpdate(Block block, int type) {
        return 0;
    }

    public boolean onActivate(Block block, ItemStack item) {
        return this.onActivate(block, item, null);
    }

    public boolean onActivate(Block block, ItemStack item, Player player) {
        return false;
    }

    public int getBurnChance(BlockState state) {
        return state.getType().getBurnChance();
    }

    public int getBurnAbility(BlockState state) {
        return state.getType().getBurnAbility();
    }

    public ToolType getToolType(BlockState state) {
        return state.getType().getToolType();
    }

    public TierType getMinimalTier(BlockState state) {
        return state.getType().getTierType();
    }

    public boolean checkTool(BlockState state, ItemStack item) {
        val toolType = getToolType(state);
        val tier = getMinimalTier(state);

        val type = item.getType();
        if (toolType != null && type.getToolType() != toolType) {
            return false;
        }

        if (tier == null) {
            return true;
        }

        return type.getTierType() != null && type.getTierType().compareTo(tier) >= 0;
    }

    public int getLightLevel(Block block) {
        return 0;
    }

    public boolean canBePlaced() {
        return true;
    }

    public boolean canBeReplaced(Block block) {
        return block.getState()
                .getType()
                .isReplaceable();
    }

    public boolean isTransparent(BlockState state) {
        val type = state.getType();
        return type.isTransparent() || type.getTranslucency() > 0;
    }

    public boolean isSolid(BlockState state) {
        return state.getType().isSolid();
    }

    public boolean isLiquid() {
        return false;
    }

    public int getFilterLevel(BlockState state) {
        return state.getType().getFilterLevel();
    }

    public boolean canBeActivated(Block block) {
        return false;
    }

    public boolean hasEntityCollision() {
        return false;
    }

    public boolean canPassThrough(BlockState state) {
        return !state.getType().blocksMotion();
    }

    public boolean canBePushed() {
        return true;
    }

    public boolean hasComparatorInputOverride(BlockState state) {
        //return state.getType().hasComparatorSignal();
        return false;
    }

    public int getComparatorInputOverride(Block block) {
        return 0;
    }

    public boolean canBeClimbed() {
        return false;
    }

    public BlockColor getColor(Block block) {
        return BlockColor.VOID_BLOCK_COLOR;
    }

    public boolean canBeFlooded(BlockState state) {
        val type = state.getType();
        return type.isFloodable() || !type.blocksWater();
    }

    public boolean place(ItemStack item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        return placeBlock(block, item);
    }

    public boolean placeBlock(Block block, ItemStack item) {
        return placeBlock(block, item, true);
    }

    public boolean placeBlock(Block block, ItemStack item, boolean update) {
        return placeBlock(block, item.getBehavior().getBlock(item), update);
    }

    public boolean placeBlock(Block block, BlockState newState) {
        return placeBlock(block, newState, true);
    }

    public abstract boolean placeBlock(Block block, BlockState newState, boolean update);

    public boolean onBreak(Block block, ItemStack item) {
        return removeBlock(block, true);
    }

    final protected boolean removeBlock(Block block) {
        return removeBlock(block, true);
    }

    final protected boolean removeBlock(Block block, boolean update) {
        BlockState state;

        if (block.isWaterlogged()) {
            state = block.getExtra();

            block.setExtra(AIR, true, false);
        } else {
            state = AIR;
        }

        return block.getLevel().setBlockAt(block.getPosition(), state, true, update);
    }

    public boolean onBreak(Block block, ItemStack item, Player player) {
        return onBreak(block, item);
    }

    public float getHardness(BlockState blockState) {
        return blockState.getType().getHardness();
    }

    public String getDescriptionId(BlockState state) {
        return "tile." + state.getType().getId().getName() + ".name";
    }

    public float getResistance(BlockState blockState) {
        return blockState.getType().getResistance();
    }

    public float getFrictionFactor(BlockState blockState) {
        return blockState.getType().getFriction();
    }

    public Vector3f addVelocityToEntity(Block block, Vector3f vector, Entity entity) {
        return vector;
    }

    public ItemStack[] getDrops(Block block, ItemStack hand) {
        if (checkTool(block.getState(), hand)) {
            return new ItemStack[]{
                    this.toItem(block)
            };
        } else {
            return new ItemStack[0];
        }
    }

    public abstract float getBreakTime(BlockState state, ItemStack item, Player player);

    public boolean canBeBrokenWith(BlockState state, ItemStack item) {
        return this.getHardness(state) != -1;
    }

    public boolean collidesWithBB(Block block, AxisAlignedBB bb) {
        return collidesWithBB(block, bb, false);
    }

    public boolean collidesWithBB(Block block, AxisAlignedBB bb, boolean collisionBB) {
        AxisAlignedBB bb1 = collisionBB ? this.getCollisionBoxes(block.getPosition(), block.getState()) : this.getBoundingBox(block);
        return bb1 != null && bb.intersectsWith(bb1);
    }

    public void onEntityCollide(Block block, Entity entity) {

    }

    public AxisAlignedBB getBoundingBox(BlockState state) {
        return getBoundingBox(null, state);
    }

    public final AxisAlignedBB getBoundingBox(Block block) {
        return getBoundingBox(block.getPosition(), block.getState());
    }

    public AxisAlignedBB getBoundingBox(Vector3i pos, BlockState state) {
        val type = state.getType();

        AxisAlignedBB bb = type.getBoundingBox();

        if (bb != null && pos != null) {
            bb = bb.offset(pos);
        }

        return bb;
    }

    public final AxisAlignedBB getCollisionBoxes(Block block) {
        return getCollisionBoxes(block.getPosition(), block.getState());
    }

    public AxisAlignedBB getCollisionBoxes(Vector3i pos, BlockState state) {
        return getBoundingBox(pos, state);
    }

    public String getSaveId() {
        String name = getClass().getName();
        return name.substring(16);
    }

    public int getWeakPower(Block block, Direction face) {
        return 0;
    }

    public int getStrongPower(Block block, Direction side) {
        return 0;
    }

    public boolean isPowerSource(Block block) {
        return block.getState().getType().isPowerSource();
    }

    public int getDropExp() {
        return 0;
    }

    public boolean isNormalBlock(Block block) {
        val state = block.getState();
        return !isTransparent(state) && isSolid(state) && !isPowerSource(block);
    }

    public BlockBehavior clone() {
        try {
            return (BlockBehavior) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException(e);
        }
    }

    public ItemStack toItem(Block block) {
        return block.getState().getType().createItem();
    }

    public boolean canSilkTouch(BlockState state) {
        return state.getType().canBeSilkTouched();
    }

    public boolean canWaterlogSource(BlockState state) {
        return state.getType().waterlogsSource();
    }

    public boolean canWaterlogFlowing(BlockState state) {
        val type = state.getType();
        return !type.breaksFlowing() && !type.blocksWater();
    }
}
