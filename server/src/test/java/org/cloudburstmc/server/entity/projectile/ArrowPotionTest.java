package org.cloudburstmc.server.entity.projectile;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.damage.DamageSource;
import org.cloudburstmc.api.event.entity.PotionEffectCause;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.potion.PotionEffect;
import org.cloudburstmc.api.potion.PotionType;
import org.cloudburstmc.api.potion.PotionTypes;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.server.entity.EntityLiving;
import org.cloudburstmc.server.network.NetworkUtils;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes.AIR_SUPPLY;
import static org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes.CUSTOM_DISPLAY;
import static org.junit.jupiter.api.Assertions.*;

class ArrowPotionTest {

    @Test
    void derivesParticleMetadataAndPickupItemFromTheAmmunition() {
        EntityArrow arrow = newArrow();
        for (PotionType potion : PotionTypes.values()) {
            ItemStack ammunition = ItemStack.builder(ItemTypes.ARROW).amount(12)
                    .setData(ItemDataComponents.POTION_TYPE, potion).build();

            arrow.setItemStack(ammunition);

            assertEquals(1, arrow.getItemStack().getCount());
            assertEquals(potion, arrow.getItemStack().get(ItemDataComponents.POTION_TYPE));
            assertEquals(Byte.valueOf((byte) (NetworkUtils.potionToNetwork(potion) + 1)),
                    arrow.getData().get(CUSTOM_DISPLAY));
            assertEquals(12, ammunition.getCount());
        }

        arrow.setItemStack(ItemStack.from(ItemTypes.ARROW));
        assertEquals(Byte.valueOf((byte) 0), arrow.getData().get(CUSTOM_DISPLAY));
        assertFalse(arrow.getItemStack().has(ItemDataComponents.POTION_TYPE));
        assertThrows(IllegalArgumentException.class, () -> arrow.setItemStack(ItemStack.EMPTY));
        assertThrows(IllegalArgumentException.class, () -> arrow.setItemStack(ItemStack.from(ItemTypes.DIRT)));
    }

    @Test
    void restoresPotionMetadataFromTheSavedAmmunition() {
        EntityArrow original = newArrow();
        ItemStack ammunition = ItemStack.builder(ItemTypes.ARROW)
                .setData(ItemDataComponents.POTION_TYPE, PotionTypes.POISON)
                .setData(ItemDataComponents.CUSTOM_NAME, "Poison arrow").build();
        original.setItemStack(ammunition);
        original.getData().set(AIR_SUPPLY, (short) 300);
        NbtMapBuilder saved = NbtMap.builder();
        original.saveAdditionalData(saved);
        EntityArrow restored = newArrow();

        restored.loadAdditionalData(NbtMap.builder().putCompound("Item", saved.build().getCompound("Item")).build());

        assertEquals(ammunition, restored.getItemStack());
        assertEquals(original.getData().get(CUSTOM_DISPLAY), restored.getData().get(CUSTOM_DISPLAY));
    }

    @Test
    void appliesScaledPotionEffectsOnlyWhenTheHitIsAccepted() {
        EntityArrow arrow = newArrow();
        arrow.setItemStack(ItemStack.builder(ItemTypes.ARROW)
                .setData(ItemDataComponents.POTION_TYPE, PotionTypes.POISON).build());
        RecordingLiving target = new RecordingLiving();

        assertTrue(arrow.damageEntity(target));

        PotionEffect base = PotionTypes.POISON.getEffects().getFirst();
        assertEquals(List.of(base.withDuration(Math.max(1, base.getDuration() / 8))), target.receivedEffects);
        assertEquals(PotionEffectCause.ARROW, target.effectCause);
        assertSame(arrow, target.effectSource);

        target.receivedEffects.clear();
        target.acceptDamage = false;

        assertFalse(arrow.damageEntity(target));
        assertTrue(target.receivedEffects.isEmpty());
    }

    private static EntityArrow newArrow() {
        return new EntityArrow(EntityTypes.ARROW, Location.from(Vector3f.ZERO, InterfaceProxy.create(Level.class)));
    }

    private static class RecordingLiving extends EntityLiving {
        private final List<PotionEffect> receivedEffects = new ArrayList<>();
        private boolean acceptDamage = true;
        private @Nullable Entity effectSource;
        private @Nullable PotionEffectCause effectCause;

        private RecordingLiving() {
            super(EntityTypes.PIG, Location.from(Vector3f.ZERO, InterfaceProxy.create(Level.class)));
        }

        @Override
        public boolean damage(float amount, DamageSource source) {
            return this.acceptDamage;
        }

        @Override
        public boolean addPotionEffect(PotionEffect effect, @Nullable Entity source, PotionEffectCause cause) {
            this.receivedEffects.add(effect);
            this.effectSource = source;
            this.effectCause = cause;
            return true;
        }
    }
}
