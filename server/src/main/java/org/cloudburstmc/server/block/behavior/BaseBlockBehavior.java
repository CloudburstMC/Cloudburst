package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.block.behavior.BlockBehavior;
import org.cloudburstmc.api.enchantment.EnchantmentInstance;
import org.cloudburstmc.api.enchantment.EnchantmentTypes;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.TierType;
import org.cloudburstmc.api.item.ToolType;
import org.cloudburstmc.api.item.ToolTypes;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.potion.Effect;
import org.cloudburstmc.api.potion.EffectTypes;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudBlockRegistry;

import java.util.Objects;
import java.util.Optional;

import static org.cloudburstmc.api.block.BlockTypes.WEB;
import static org.cloudburstmc.api.block.BlockTypes.WOOL;

public abstract class BaseBlockBehavior extends BlockBehavior {

    public float getBreakTime(BlockState state, ItemStack item, Player p) {
        CloudPlayer player = (CloudPlayer)p;
        Objects.requireNonNull(item, "getBreakTime: Item can not be null");
        //Objects.requireNonNull(p, "getBreakTime: Player can not be null");
        float blockHardness = getHardness(state);
        var toolType = getToolType(state);

        var itemBehavior = item.getBehavior();
        var itemToolType = itemBehavior.getToolType(item);
        var itemTier = itemBehavior.getTier(item);

        boolean correctTool = toolType == null || itemToolType == toolType;
        boolean canHarvestWithHand = canHarvestWithHand(state);
        var blockType = state.getType();
        int efficiencyLoreLevel = Optional.ofNullable(item.getEnchantment(EnchantmentTypes.EFFICIENCY))
                .map(EnchantmentInstance::getLevel).orElse(0);
        int hasteEffectLevel = Optional.ofNullable(player).map((p1) -> p1.getEffect(EffectTypes.HASTE))
                .map(Effect::getAmplifier).orElse(0);
        boolean insideOfWaterWithoutAquaAffinity = player != null && player.isInsideOfWater() &&
                Optional.ofNullable(player.getInventory().getHelmet().getEnchantment(EnchantmentTypes.WATER_WORKER))
                        .map(EnchantmentInstance::getLevel).map(l -> l >= 1).orElse(false);
        boolean outOfWaterButNotOnGround = player != null && (!player.isInsideOfWater()) && (!player.isOnGround());
        return breakTime0(blockHardness, correctTool, canHarvestWithHand, blockType, itemToolType, itemTier,
                efficiencyLoreLevel, hasteEffectLevel, insideOfWaterWithoutAquaAffinity, outOfWaterButNotOnGround);
    }

    public boolean placeBlock(Block block, BlockState newState, boolean update) {
        var state = block.getLiquid();
        BlockBehavior behavior = CloudBlockRegistry.get().getBehavior(state.getType());
        if (behavior instanceof BlockBehaviorLiquid && ((BlockBehaviorLiquid) behavior).usesWaterLogging()) {
            boolean flowing = state.ensureTrait(BlockTraits.IS_FLOWING) || state.ensureTrait(BlockTraits.FLUID_LEVEL) != 0;

            var newBehavior = newState.getBehavior();
            if (!flowing && newBehavior.canWaterlogSource(newState) || flowing && newBehavior.canWaterlogFlowing(newState)) {
                block.set(state, 1, true, false);
            } else {
                block.setExtra(BlockStates.AIR, true, false);
            }
        }

        return block.getLevel().setBlockState(block.getPosition(), newState, true, update);
    }

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

}
