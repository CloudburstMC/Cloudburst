package org.cloudburstmc.server.enchantment.behavior;

import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.enchantment.Enchantment;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.damage.DamageSource;
import org.cloudburstmc.api.entity.damage.DamageTypeTags;
import org.cloudburstmc.api.event.block.EntityBlockFormEvent;
import org.cloudburstmc.api.item.EquipmentSlot;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;

public class EnchantmentFrostWalker extends EnchantmentBehavior {

    @Override
    public boolean grantsDamageImmunity(Enchantment enchantment, EquipmentSlot slot, DamageSource source) {
        return slot == EquipmentSlot.FEET
                && source.getDamageType().is(DamageTypeTags.BURN_FROM_STEPPING)
                && !source.getDamageType().is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    @Override
    public void onLocationChanged(Enchantment enchantment, Entity wearer, EquipmentSlot slot) {
        if (slot != EquipmentSlot.FEET || !wearer.isOnGround() || wearer.getVehicle() != null) {
            return;
        }

        Level level = wearer.getLevel();
        Vector3f position = wearer.getPosition();
        Vector3i center = position.toInt().add(0, -1, 0);
        int radius = (int) Math.min(16L, enchantment.level() + 2L);

        for (int z = -radius; z <= radius; z++) {
            for (int x = -radius; x <= radius; x++) {
                Vector3i target = center.add(x, 0, z);
                double dx = target.getX() + 0.5 - position.getX();
                double dz = target.getZ() + 0.5 - position.getZ();
                if (dx * dx + dz * dz >= radius * radius) {
                    continue;
                }

                Block water = level.getLoadedBlock(target);
                Block above = level.getLoadedBlock(target.add(0, 1, 0));
                if (water == null || above == null || blocksFreezing(water, above)) {
                    continue;
                }

                EntityBlockFormEvent event = new EntityBlockFormEvent(wearer, water, BlockTypes.FROSTED_ICE.getDefaultState());
                level.getServer().getEventManager().fire(event);

                if (wearer.getLevel() != level || !wearer.getPosition().equals(position) || !wearer.isOnGround() || wearer.getVehicle() != null) {
                    return;
                }

                Block current = level.getLoadedBlock(target);
                Block currentAbove = level.getLoadedBlock(target.add(0, 1, 0));
                if (event.isCancelled() || current == null || currentAbove == null || current.getState() != water.getState() || blocksFreezing(current, currentAbove)) {
                    continue;
                }

                level.setBlockState(target, event.getNewState());
            }
        }
    }

    private static boolean blocksFreezing(Block water, Block above) {
        return !water.getState().getType().isLiquid()
                || !water.getLiquid().isSource() || !water.getLiquid().getType().isSameFamily(LiquidTypes.WATER)
                || water.getState(BlockLayer.SECONDARY) != BlockStates.AIR
                || above.getState() != BlockStates.AIR || above.getState(BlockLayer.SECONDARY) != BlockStates.AIR
                || water.getLevel().hasPlacementObstruction(BlockTypes.FROSTED_ICE.getDefaultState().getCollisionShape(), water.getPosition());
    }
}
