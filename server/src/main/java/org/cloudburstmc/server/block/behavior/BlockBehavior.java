package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.item.BlockItem;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemTool;
import org.cloudburstmc.server.item.enchantment.Enchantment;
import org.cloudburstmc.server.math.AxisAlignedBB;
import org.cloudburstmc.server.math.BlockFace;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.potion.Effect;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.Identifier;

import java.util.Objects;
import java.util.Optional;

import static org.cloudburstmc.server.block.BlockTypes.*;

public abstract class BlockBehavior implements AxisAlignedBB {

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
        if (toolType == ItemTool.TYPE_SWORD) return isCobweb ? 15.0f : 1.0f;
        if (toolType == ItemTool.TYPE_SHEARS) return isWoolBlock ? 5.0f : 15.0f;
        if (toolType == ItemTool.TYPE_NONE) return 1.0f;
        switch (toolTier) {
            case ItemTool.TIER_WOODEN:
                return 2.0f;
            case ItemTool.TIER_STONE:
                return 4.0f;
            case ItemTool.TIER_IRON:
                return 6.0f;
            case ItemTool.TIER_DIAMOND:
                return 8.0f;
            case ItemTool.TIER_GOLD:
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

    public boolean isBreakable(Item item) {
        return true;
    }

    public int tickRate() {
        return 10;
    }

    public int onUpdate(Block block, int type) {
        return 0;
    }

    public boolean onActivate(Block block, Item item) {
        return this.onActivate(item, null);
    }

    public boolean onActivate(Block block, Item item, Player player) {
        return false;
    }

    public int getBurnChance() {
        return 0;
    }

    public int getBurnAbility() {
        return 0;
    }

    public int getToolType() {
        return ItemTool.TYPE_NONE;
    }

    public int getLightLevel() {
        return 0;
    }

    public boolean canBePlaced() {
        return true;
    }

    public boolean canBeReplaced() {
        return false;
    }

    public boolean isTransparent() {
        return false;
    }

    public boolean isSolid() {
        return true;
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

    public boolean canBeActivated() {
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

    public int getComparatorInputOverride() {
        return 0;
    }

    public boolean canBeClimbed() {
        return false;
    }

    public BlockColor getColor() {
        return BlockColor.VOID_BLOCK_COLOR;
    }

    public boolean canBeFlooded() {
        return false;
    }

    public boolean place(Item item, Block block, Block target, BlockFace face, Vector3f clickPos, Player player) {
        return block.getLevel().setBlock(block.getPosition(), item.getBlock(), true, true);
    }

    public boolean onBreak(Block block, Item item) {
        return removeBlock(true);
    }

    final protected boolean removeBlock(boolean update) {
        if (this.isWaterlogged()) {
            BlockBehavior water = getLevel().getBlock(getX(), getY(), getZ(), 1);
            getLevel().setBlock(this.getPosition(), water, true, false);
            return getLevel().setBlock(getX(), getY(), getZ(), 1, BlockBehavior.get(AIR), true, update);
        }
        return this.getLevel().setBlock(this.getPosition(), BlockBehavior.get(AIR), true, update);
    }

    public boolean onBreak(Block block, Item item, Player player) {
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

    public Vector3f addVelocityToEntity(Entity entity, Vector3f vector) {
        return vector;
    }

    private static int toolType0(Item item) {
        if (item.isSword()) return ItemTool.TYPE_SWORD;
        if (item.isShovel()) return ItemTool.TYPE_SHOVEL;
        if (item.isPickaxe()) return ItemTool.TYPE_PICKAXE;
        if (item.isAxe()) return ItemTool.TYPE_AXE;
        if (item.isShears()) return ItemTool.TYPE_SHEARS;
        return ItemTool.TYPE_NONE;
    }

    private static boolean correctTool0(int blockToolType, Item item) {
        return (blockToolType == ItemTool.TYPE_SWORD && item.isSword()) ||
                (blockToolType == ItemTool.TYPE_SHOVEL && item.isShovel()) ||
                (blockToolType == ItemTool.TYPE_PICKAXE && item.isPickaxe()) ||
                (blockToolType == ItemTool.TYPE_AXE && item.isAxe()) ||
                (blockToolType == ItemTool.TYPE_SHEARS && item.isShears()) ||
                blockToolType == ItemTool.TYPE_NONE;
    }

    public Item[] getDrops(BlockState blockState, Item hand) {
        return new Item[]{
                this.toItem(blockState)
        };
    }

    public float getBreakTime(BlockState state, Item item, Player player) {
        Objects.requireNonNull(item, "getBreakTime: Item can not be null");
        Objects.requireNonNull(player, "getBreakTime: Player can not be null");
        float blockHardness = getHardness();
        boolean correctTool = correctTool0(getToolType(), item);
        boolean canHarvestWithHand = canHarvestWithHand();
        Identifier blockType = state.getType();
        int itemToolType = toolType0(item);
        int itemTier = item.getTier();
        int efficiencyLoreLevel = Optional.ofNullable(item.getEnchantment(Enchantment.ID_EFFICIENCY))
                .map(Enchantment::getLevel).orElse(0);
        int hasteEffectLevel = Optional.ofNullable(player.getEffect(Effect.HASTE))
                .map(Effect::getAmplifier).orElse((byte) 0);
        boolean insideOfWaterWithoutAquaAffinity = player.isInsideOfWater() &&
                Optional.ofNullable(player.getInventory().getHelmet().getEnchantment(Enchantment.ID_WATER_WORKER))
                        .map(Enchantment::getLevel).map(l -> l >= 1).orElse(false);
        boolean outOfWaterButNotOnGround = (!player.isInsideOfWater()) && (!player.isOnGround());
        return breakTime0(blockHardness, correctTool, canHarvestWithHand, blockType, itemToolType, itemTier,
                efficiencyLoreLevel, hasteEffectLevel, insideOfWaterWithoutAquaAffinity, outOfWaterButNotOnGround);
    }

    public boolean canBeBrokenWith(Item item) {
        return this.getHardness() != -1;
    }

    /**
     * @param item item used
     * @return break time
     * @deprecated This function is lack of Player class and is not accurate enough, use #getBreakTime(Item, Player)
     */
    @Deprecated
    public float getBreakTime(Item item) {
        float base = this.getHardness() * 1.5f;
        if (this.canBeBrokenWith(item)) {
            if (this.getToolType() == ItemTool.TYPE_SHEARS && item.isShears()) {
                base /= 15;
            } else if (
                    (this.getToolType() == ItemTool.TYPE_PICKAXE && item.isPickaxe()) ||
                            (this.getToolType() == ItemTool.TYPE_AXE && item.isAxe()) ||
                            (this.getToolType() == ItemTool.TYPE_SHOVEL && item.isShovel())
            ) {
                int tier = item.getTier();
                switch (tier) {
                    case ItemTool.TIER_WOODEN:
                        base /= 2;
                        break;
                    case ItemTool.TIER_STONE:
                        base /= 4;
                        break;
                    case ItemTool.TIER_IRON:
                        base /= 6;
                        break;
                    case ItemTool.TIER_DIAMOND:
                        base /= 8;
                        break;
                    case ItemTool.TIER_GOLD:
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

    public boolean collidesWithBB(AxisAlignedBB bb) {
        return collidesWithBB(bb, false);
    }

    public boolean collidesWithBB(AxisAlignedBB bb, boolean collisionBB) {
        AxisAlignedBB bb1 = collisionBB ? this.getCollisionBoxes() : this.getBoundingBox();
        return bb1 != null && bb.intersectsWith(bb1);
    }

    public void onEntityCollide(Entity entity) {

    }

    public AxisAlignedBB getBoundingBox() {
        return this.recalculateBoundingBox();
    }

    public AxisAlignedBB getCollisionBoxes() {
        return this.recalculateCollisionBoundingBox();
    }

    public String getSaveId() {
        String name = getClass().getName();
        return name.substring(16);
    }

    public int getWeakPower(BlockFace face) {
        return 0;
    }

    public int getStrongPower(BlockFace side) {
        return 0;
    }

    public boolean isPowerSource() {
        return false;
    }

    public int getDropExp() {
        return 0;
    }

    public boolean isNormalBlock() {
        return !isTransparent() && isSolid() && !isPowerSource();
    }

    public BlockBehavior clone() {
        try {
            return (BlockBehavior) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException(e);
        }
    }

    public Item toItem(BlockState state) {
        return new BlockItem(state);
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
