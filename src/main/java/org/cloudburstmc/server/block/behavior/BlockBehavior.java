package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockStates;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.enchantment.CloudEnchantmentInstance;
import org.cloudburstmc.server.enchantment.EnchantmentInstance;
import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.behavior.ItemToolBehavior;
import org.cloudburstmc.server.math.AxisAlignedBB;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.math.SimpleAxisAlignedBB;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.potion.Effect;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.Identifier;

import java.util.Objects;
import java.util.Optional;

import static org.cloudburstmc.server.block.BlockIds.WEB;
import static org.cloudburstmc.server.block.BlockIds.WOOL;

public abstract class BlockBehavior {

    //http://minecraft.gamepedia.com/Breaking
    private static float breakTime0(float blockHardness, boolean correctTool, boolean canHarvestWithHand,
                                    Identifier id, int toolType, int toolTier, int efficiencyLoreLevel, int hasteEffectLevel,
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

    private static float toolBreakTimeBonus0(
            int toolType, int toolTier, boolean isWoolBlock, boolean isCobweb) {
        if (toolType == ItemToolBehavior.TYPE_SWORD) return isCobweb ? 15.0f : 1.0f;
        if (toolType == ItemToolBehavior.TYPE_SHEARS) return isWoolBlock ? 5.0f : 15.0f;
        if (toolType == ItemToolBehavior.TYPE_NONE) return 1.0f;
        switch (toolTier) {
            case ItemToolBehavior.TIER_WOODEN:
                return 2.0f;
            case ItemToolBehavior.TIER_STONE:
                return 4.0f;
            case ItemToolBehavior.TIER_IRON:
                return 6.0f;
            case ItemToolBehavior.TIER_DIAMOND:
                return 8.0f;
            case ItemToolBehavior.TIER_GOLD:
                return 12.0f;
            default:
                return 1.0f;
        }
    }

    private static float speedBonusByEfficiencyLore0(int efficiencyLoreLevel) {
        if (efficiencyLoreLevel == 0) return 0;
        return efficiencyLoreLevel * efficiencyLoreLevel + 1;
    }

    private static float speedRateByHasteLore0(int hasteLoreLevel) {
        return 1.0f + (0.2f * hasteLoreLevel);
    }

    //http://minecraft.gamepedia.com/Breaking
    public boolean canHarvestWithHand() {  //used for calculating breaking time
        return true;
    }

    public boolean isBreakable(ItemStack item) {
        return true;
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

    public int getBurnChance() {
        return 0;
    }

    public int getBurnAbility() {
        return 0;
    }

    public int getToolType() {
        return ItemToolBehavior.TYPE_NONE;
    }

    public int getLightLevel(Block block) {
        return 0;
    }

    public boolean canBePlaced() {
        return true;
    }

    public boolean canBeReplaced(Block block) {
        return false;
    }

    public boolean isTransparent() {
        return false;
    }

    public boolean isSolid() {
        return true;
    }

    public boolean isLiquid() {
        return false;
    }

    public int getFilterLevel() {
        if (isSolid()) {
            if (isTransparent()) {
                if (this instanceof BlockBehaviorLiquid || this instanceof BlockBehaviorIce) {
                    return 2;
                }
            } else {
                return 15;
            }
        }
        return 1;
    }

    public boolean canBeActivated(Block block) {
        return false;
    }

    public boolean hasEntityCollision() {
        return false;
    }

    public boolean canPassThrough() {
        return false;
    }

    public boolean canBePushed() {
        return true;
    }

    public boolean hasComparatorInputOverride() {
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

    public boolean canBeFlooded() {
        return false;
    }

    public boolean place(ItemStack item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        return placeBlock(block, item);
    }

    protected boolean placeBlock(Block block, ItemStack item) {
        return placeBlock(block, item, true);
    }

    protected boolean placeBlock(Block block, ItemStack item, boolean update) {
        return placeBlock(block, item.getBlock(), update);
    }

    protected boolean placeBlock(Block block, BlockState newState) {
        return placeBlock(block, newState, true);
    }

    protected boolean placeBlock(Block block, BlockState newState, boolean update) {
        BlockBehavior behavior = block.getState().getBehavior();
        if (behavior instanceof BlockBehaviorLiquid && ((BlockBehaviorLiquid) behavior).usesWaterLogging()) {
            BlockState state = block.getState();
            boolean flowing = state.ensureTrait(BlockTraits.IS_FLOWING) || state.ensureTrait(BlockTraits.FLUID_LEVEL) != 0;

            if (!flowing && canWaterlogSource() || flowing && canWaterlogFlowing()) {
                block.set(block.getState(), 1, true, false);
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

    public float getHardness() {
        return 10;
    }

    public String getDescriptionId(BlockState state) {
        return "tile." + state.getType().getName() + ".name";
    }

    public float getResistance() {
        return 1;
    }

    public float getFrictionFactor() {
        return 0.6f;
    }

    public Vector3f addVelocityToEntity(Block block, Vector3f vector, Entity entity) {
        return vector;
    }

    private static int toolType0(ItemStack item) {
        if (item.isSword()) return ItemToolBehavior.TYPE_SWORD;
        if (item.isShovel()) return ItemToolBehavior.TYPE_SHOVEL;
        if (item.isPickaxe()) return ItemToolBehavior.TYPE_PICKAXE;
        if (item.isAxe()) return ItemToolBehavior.TYPE_AXE;
        if (item.isShears()) return ItemToolBehavior.TYPE_SHEARS;
        return ItemToolBehavior.TYPE_NONE;
    }

    private static boolean correctTool0(int blockToolType, ItemStack item) {
        return (blockToolType == ItemToolBehavior.TYPE_SWORD && item.isSword()) ||
                (blockToolType == ItemToolBehavior.TYPE_SHOVEL && item.isShovel()) ||
                (blockToolType == ItemToolBehavior.TYPE_PICKAXE && item.isPickaxe()) ||
                (blockToolType == ItemToolBehavior.TYPE_AXE && item.isAxe()) ||
                (blockToolType == ItemToolBehavior.TYPE_SHEARS && item.isShears()) ||
                blockToolType == ItemToolBehavior.TYPE_NONE;
    }

    public ItemStack[] getDrops(Block block, ItemStack hand) {
        return new ItemStack[]{
                this.toItem(block)
        };
    }

    public float getBreakTime(BlockState state, ItemStack item, Player player) {
        Objects.requireNonNull(item, "getBreakTime: Item can not be null");
        Objects.requireNonNull(player, "getBreakTime: Player can not be null");
        float blockHardness = getHardness();
        boolean correctTool = correctTool0(getToolType(), item);
        boolean canHarvestWithHand = canHarvestWithHand();
        Identifier blockType = state.getType();
        int itemToolType = toolType0(item);
        int itemTier = item.getTier();
        int efficiencyLoreLevel = Optional.ofNullable(item.getEnchantment(CloudEnchantmentInstance.ID_EFFICIENCY))
                .map(EnchantmentInstance::getLevel).orElse(0);
        int hasteEffectLevel = Optional.ofNullable(player.getEffect(Effect.HASTE))
                .map(Effect::getAmplifier).orElse((byte) 0);
        boolean insideOfWaterWithoutAquaAffinity = player.isInsideOfWater() &&
                Optional.ofNullable(player.getInventory().getHelmet().getEnchantment(CloudEnchantmentInstance.ID_WATER_WORKER))
                        .map(EnchantmentInstance::getLevel).map(l -> l >= 1).orElse(false);
        boolean outOfWaterButNotOnGround = (!player.isInsideOfWater()) && (!player.isOnGround());
        return breakTime0(blockHardness, correctTool, canHarvestWithHand, blockType, itemToolType, itemTier,
                efficiencyLoreLevel, hasteEffectLevel, insideOfWaterWithoutAquaAffinity, outOfWaterButNotOnGround);
    }

    public boolean canBeBrokenWith(ItemStack item) {
        return this.getHardness() != -1;
    }

    /**
     * @param item item used
     * @return break time
     * @deprecated This function is lack of Player class and is not accurate enough, use #getBreakTime(Item, Player)
     */
    @Deprecated
    public float getBreakTime(ItemStack item) {
        float base = this.getHardness() * 1.5f;
        if (this.canBeBrokenWith(item)) {
            if (this.getToolType() == ItemToolBehavior.TYPE_SHEARS && item.isShears()) {
                base /= 15;
            } else if (
                    (this.getToolType() == ItemToolBehavior.TYPE_PICKAXE && item.isPickaxe()) ||
                            (this.getToolType() == ItemToolBehavior.TYPE_AXE && item.isAxe()) ||
                            (this.getToolType() == ItemToolBehavior.TYPE_SHOVEL && item.isShovel())
            ) {
                int tier = item.getTier();
                switch (tier) {
                    case ItemToolBehavior.TIER_WOODEN:
                        base /= 2;
                        break;
                    case ItemToolBehavior.TIER_STONE:
                        base /= 4;
                        break;
                    case ItemToolBehavior.TIER_IRON:
                        base /= 6;
                        break;
                    case ItemToolBehavior.TIER_DIAMOND:
                        base /= 8;
                        break;
                    case ItemToolBehavior.TIER_GOLD:
                        base /= 12;
                        break;
                }
            }
        } else {
            base *= 3.33f;
        }

        if (item.isSword()) {
            base *= 0.5f;
        }

        return base;
    }

    public boolean collidesWithBB(Block block, AxisAlignedBB bb) {
        return collidesWithBB(block, bb, false);
    }

    public boolean collidesWithBB(Block block, AxisAlignedBB bb, boolean collisionBB) {
        AxisAlignedBB bb1 = collisionBB ? this.getCollisionBoxes(block) : this.getBoundingBox();
        return bb1 != null && bb.intersectsWith(bb1);
    }

    public void onEntityCollide(Block block, Entity entity) {

    }

    public AxisAlignedBB getBoundingBox() {
//        Vector3i pos = block.getPosition();
//        return new SimpleAxisAlignedBB(pos, pos.add(1, 1, 1));
        return new SimpleAxisAlignedBB(0, 0, 0, 1, 1, 1);
    }

    public AxisAlignedBB getCollisionBoxes(Block block) {
        return getBoundingBox();
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
        return false;
    }

    public int getDropExp() {
        return 0;
    }

    public boolean isNormalBlock(Block block) {
        return !isTransparent() && isSolid() && !isPowerSource(block);
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

    public boolean canSilkTouch() {
        return false;
    }

    public boolean canWaterlogSource() {
        return false;
    }

    public boolean canWaterlogFlowing() {
        return false;
    }
}
