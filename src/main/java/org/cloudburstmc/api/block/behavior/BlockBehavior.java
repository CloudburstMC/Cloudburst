package org.cloudburstmc.api.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.math.vector.Vector3i;
import lombok.val;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.TierType;
import org.cloudburstmc.api.item.ToolType;
import org.cloudburstmc.api.item.ToolTypes;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.data.BlockColor;
import org.cloudburstmc.server.math.AxisAlignedBB;
import org.cloudburstmc.server.math.SimpleAxisAlignedBB;

import static org.cloudburstmc.api.block.BlockTypes.WEB;
import static org.cloudburstmc.api.block.BlockTypes.WOOL;

public abstract class BlockBehavior {

    public static final AxisAlignedBB DEFAULT_AABB = new SimpleAxisAlignedBB(0, 0, 0, 1, 1, 1);

    //http://minecraft.gamepedia.com/Breaking
    private static float breakTime0(float blockHardness, boolean correctTool, boolean canHarvestWithHand,
                                    BlockType id, ToolType toolType, TierType toolTier, int efficiencyLoreLevel, int hasteEffectLevel,
                                    boolean insideOfWaterWithoutAquaAffinity, boolean outOfWaterButNotOnGround) {
        float baseTime = ((correctTool || canHarvestWithHand) ? 1.5f : 5.0f) * blockHardness;
        float speed = 1.0f / baseTime;
        boolean isWoolBlock = id == WOOL, isCobweb = id == WEB;
        if (correctTool) speed *= toolBreakTimeBonus0(toolType, toolTier, isWoolBlock, isCobweb);
        speed += speedBonusByEfficiencyLore0(efficiencyLoreLevel);
        speed *= speedRateByHasteLore0(hasteEffectLevel);
        if (insideOfWaterWithoutAquaAffinity) speed *= 0.2f;
        if (outOfWaterButNotOnGround) speed *= 0.2f;
        return 1.0f / speed;
    }

    private static float toolBreakTimeBonus0(ToolType toolType, TierType toolTier, boolean isWoolBlock, boolean isCobweb) {
        if (toolType == ToolTypes.SWORD) return isCobweb ? 15.0f : 1.0f;
        if (toolType == ToolTypes.SHEARS) return isWoolBlock ? 5.0f : 15.0f;
        if (toolType == null) return 1.0f;

        return Math.max(1, toolTier.getMiningEfficiency());
    }

    private static float speedBonusByEfficiencyLore0(int efficiencyLoreLevel) {
        if (efficiencyLoreLevel == 0) return 0;
        return efficiencyLoreLevel * efficiencyLoreLevel + 1;
    }

    private static float speedRateByHasteLore0(int hasteLoreLevel) {
        return 1.0f + (0.2f * hasteLoreLevel);
    }

    //http://minecraft.gamepedia.com/Breaking
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

    protected boolean placeBlock(Block block, ItemStack item) {
        return placeBlock(block, item, true);
    }

    protected boolean placeBlock(Block block, ItemStack item, boolean update) {
        return placeBlock(block, item.getBehavior().getBlock(item), update);
    }

    protected boolean placeBlock(Block block, BlockState newState) {
        return placeBlock(block, newState, true);
    }

    protected boolean placeBlock(Block block, BlockState newState, boolean update) {
        val state = block.getLiquid();
        BlockBehavior behavior = state.getBehavior();
        if (behavior instanceof BlockBehaviorLiquid && ((BlockBehaviorLiquid) behavior).usesWaterLogging()) {
            boolean flowing = state.ensureTrait(BlockTraits.IS_FLOWING) || state.ensureTrait(BlockTraits.FLUID_LEVEL) != 0;

            val newBehavior = newState.getBehavior();
            if (!flowing && newBehavior.canWaterlogSource(newState) || flowing && newBehavior.canWaterlogFlowing(newState)) {
                block.set(state, 1, true, false);
            } else {
                block.setExtra(BlockStates.AIR, true, false);
            }
        }

        return block.getLevel().setBlock(block.getPosition(), newState, true, update);
    }

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

            block.setExtra(BlockStates.AIR, true, false);
        } else {
            state = BlockStates.AIR;
        }

        return block.getLevel().setBlock(block.getPosition(), state, true, update);
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

    public float getBreakTime(BlockState state, ItemStack item, Player player) {
/*        Objects.requireNonNull(item, "getBreakTime: Item can not be null");
//        Objects.requireNonNull(player, "getBreakTime: Player can not be null");
        float blockHardness = getHardness(state);
        val toolType = getToolType(state);

        val itemBehavior = item.getBehavior();
        val itemToolType = itemBehavior.getToolType(item);
        val itemTier = itemBehavior.getTier(item);

        boolean correctTool = toolType == null || itemToolType == toolType;
        boolean canHarvestWithHand = canHarvestWithHand(state);
        val blockType = state.getType();
        int efficiencyLoreLevel = Optional.ofNullable(item.getEnchantment(EnchantmentTypes.EFFICIENCY))
                .map(EnchantmentInstance::getLevel).orElse(0);
        int hasteEffectLevel = Optional.ofNullable(player).map((p) -> p.getEffect(Effect.HASTE))
                .map(Effect::getAmplifier).orElse((byte) 0);
        boolean insideOfWaterWithoutAquaAffinity = player != null && player.isInsideOfWater() &&
                Optional.ofNullable(player.getInventory().getHelmet().getEnchantment(EnchantmentTypes.WATER_WORKER))
                        .map(EnchantmentInstance::getLevel).map(l -> l >= 1).orElse(false);
        boolean outOfWaterButNotOnGround = player != null && (!player.isInsideOfWater()) && (!player.isOnGround());
        return breakTime0(blockHardness, correctTool, canHarvestWithHand, blockType, itemToolType, itemTier,
                efficiencyLoreLevel, hasteEffectLevel, insideOfWaterWithoutAquaAffinity, outOfWaterButNotOnGround);*/
        return 0f; // TODO implement on Server side?
    }

    public boolean canBeBrokenWith(BlockState state, ItemStack item) {
        return this.getHardness(state) != -1;
    }

    /**
     * @param blockState
     * @param item       item used
     * @return break time
     * @deprecated This function is lack of Player class and is not accurate enough, use #getBreakTime(Item, Player)
     */
    @Deprecated
    public float getBreakTime(BlockState blockState, ItemStack item) {
/*        val behavior = item.getBehavior();
        float base = this.getHardness(blockState) * 1.5f;
        if (this.canBeBrokenWith(blockState, item)) {
            if (this.getToolType(blockState) == ToolTypes.SHEARS && behavior.isShears()) {
                base /= 15;
            } else if (
                    (this.getToolType(blockState) == ToolTypes.PICKAXE && behavior.isPickaxe()) ||
                            (this.getToolType(blockState) == ToolTypes.AXE && behavior.isAxe()) ||
                            (this.getToolType(blockState) == ToolTypes.SHOVEL && behavior.isShovel())
            ) {
                base /= behavior.getTier(item).getMiningEfficiency();
            }
        } else {
            base *= 3.33f;
        }

        if (behavior.isSword()) {
            base *= 0.5f;
        }

        return base;*/
        return 0f;
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
        return ItemStack.get(block.getState());
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
