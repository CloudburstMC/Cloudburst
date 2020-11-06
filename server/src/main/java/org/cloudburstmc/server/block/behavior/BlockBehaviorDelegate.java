package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.math.vector.Vector3i;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.ToolType;
import org.cloudburstmc.server.math.AxisAlignedBB;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.BlockColor;

import javax.annotation.Nonnull;
import java.util.Objects;

public class BlockBehaviorDelegate extends BlockBehavior {

    private final BlockBehavior parent;

    public BlockBehaviorDelegate(@Nonnull BlockBehavior parent) {
        Objects.requireNonNull(parent);
        this.parent = parent;
    }

    public BlockBehavior getParent() {
        return parent;
    }

    @Override
    public boolean canHarvestWithHand(BlockState state) {
        return parent.canHarvestWithHand(state);
    }

    @Override
    public boolean isBreakable(BlockState state, ItemStack item) {
        return parent.isBreakable(state, item);
    }

    @Override
    public int tickRate() {
        return parent.tickRate();
    }

    @Override
    public int onUpdate(Block block, int type) {
        return parent.onUpdate(block, type);
    }

    @Override
    public boolean onActivate(Block block, ItemStack item) {
        return parent.onActivate(block, item);
    }

    @Override
    public boolean onActivate(Block block, ItemStack item, Player player) {
        return parent.onActivate(block, item, player);
    }

    @Override
    public int getBurnChance(BlockState state) {
        return parent.getBurnChance(state);
    }

    @Override
    public int getBurnAbility(BlockState state) {
        return parent.getBurnAbility(state);
    }

    @Override
    public ToolType getToolType(BlockState state) {
        return parent.getToolType(state);
    }

    @Override
    public int getLightLevel(Block block) {
        return parent.getLightLevel(block);
    }

    @Override
    public boolean canBePlaced() {
        return parent.canBePlaced();
    }

    @Override
    public boolean canBeReplaced(Block block) {
        return parent.canBeReplaced(block);
    }

    @Override
    public boolean isTransparent(BlockState state) {
        return parent.isTransparent(state);
    }

    @Override
    public boolean isSolid(BlockState state) {
        return parent.isSolid(state);
    }

    @Override
    public boolean isLiquid() {
        return parent.isLiquid();
    }

    @Override
    public int getFilterLevel(BlockState state) {
        return parent.getFilterLevel(state);
    }

    @Override
    public boolean canBeActivated(Block block) {
        return parent.canBeActivated(block);
    }

    @Override
    public boolean hasEntityCollision() {
        return parent.hasEntityCollision();
    }

    @Override
    public boolean canPassThrough(BlockState state) {
        return parent.canPassThrough(state);
    }

    @Override
    public boolean canBePushed() {
        return parent.canBePushed();
    }

    @Override
    public boolean hasComparatorInputOverride(BlockState state) {
        return parent.hasComparatorInputOverride(state);
    }

    @Override
    public int getComparatorInputOverride(Block block) {
        return parent.getComparatorInputOverride(block);
    }

    @Override
    public boolean canBeClimbed() {
        return parent.canBeClimbed();
    }

    @Override
    public BlockColor getColor(Block block) {
        return parent.getColor(block);
    }

    @Override
    public boolean canBeFlooded(BlockState state) {
        return parent.canBeFlooded(state);
    }

    @Override
    public boolean place(ItemStack item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        return parent.place(item, block, target, face, clickPos, player);
    }

    @Override
    public boolean placeBlock(Block block, ItemStack item) {
        return parent.placeBlock(block, item);
    }

    @Override
    public boolean placeBlock(Block block, ItemStack item, boolean update) {
        return parent.placeBlock(block, item, update);
    }

    @Override
    public boolean placeBlock(Block block, BlockState newState) {
        return parent.placeBlock(block, newState);
    }

    @Override
    public boolean placeBlock(Block block, BlockState newState, boolean update) {
        return parent.placeBlock(block, newState, update);
    }

    @Override
    public boolean onBreak(Block block, ItemStack item) {
        return parent.onBreak(block, item);
    }

    @Override
    public boolean onBreak(Block block, ItemStack item, Player player) {
        return parent.onBreak(block, item, player);
    }

    @Override
    public float getHardness(BlockState blockState) {
        return parent.getHardness(blockState);
    }

    @Override
    public String getDescriptionId(BlockState state) {
        return parent.getDescriptionId(state);
    }

    @Override
    public float getResistance(BlockState blockState) {
        return parent.getResistance(blockState);
    }

    @Override
    public float getFrictionFactor(BlockState blockState) {
        return parent.getFrictionFactor(blockState);
    }

    @Override
    public Vector3f addVelocityToEntity(Block block, Vector3f vector, Entity entity) {
        return parent.addVelocityToEntity(block, vector, entity);
    }

    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        return parent.getDrops(block, hand);
    }

    @Override
    public float getBreakTime(BlockState state, ItemStack item, Player player) {
        return parent.getBreakTime(state, item, player);
    }

    @Override
    public boolean canBeBrokenWith(BlockState state, ItemStack item) {
        return parent.canBeBrokenWith(state, item);
    }

    @Override
    @Deprecated
    public float getBreakTime(BlockState blockState, ItemStack item) {
        return parent.getBreakTime(blockState, item);
    }

    @Override
    public boolean collidesWithBB(Block block, AxisAlignedBB bb) {
        return parent.collidesWithBB(block, bb);
    }

    @Override
    public boolean collidesWithBB(Block block, AxisAlignedBB bb, boolean collisionBB) {
        return parent.collidesWithBB(block, bb, collisionBB);
    }

    @Override
    public void onEntityCollide(Block block, Entity entity) {
        parent.onEntityCollide(block, entity);
    }

    @Override
    public AxisAlignedBB getCollisionBoxes(Vector3i pos, BlockState state) {
        return parent.getCollisionBoxes(pos, state);
    }

    @Override
    public String getSaveId() {
        return parent.getSaveId();
    }

    @Override
    public int getWeakPower(Block block, Direction face) {
        return parent.getWeakPower(block, face);
    }

    @Override
    public int getStrongPower(Block block, Direction side) {
        return parent.getStrongPower(block, side);
    }

    @Override
    public boolean isPowerSource(Block block) {
        return parent.isPowerSource(block);
    }

    @Override
    public int getDropExp() {
        return parent.getDropExp();
    }

    @Override
    public boolean isNormalBlock(Block block) {
        return parent.isNormalBlock(block);
    }

    @Override
    public BlockBehavior clone() {
        return parent.clone();
    }

    @Override
    public ItemStack toItem(Block block) {
        return parent.toItem(block);
    }

    @Override
    public boolean canSilkTouch(BlockState state) {
        return parent.canSilkTouch(state);
    }

    @Override
    public boolean canWaterlogSource(BlockState state) {
        return parent.canWaterlogSource(state);
    }

    @Override
    public boolean canWaterlogFlowing(BlockState state) {
        return parent.canWaterlogFlowing(state);
    }
}
