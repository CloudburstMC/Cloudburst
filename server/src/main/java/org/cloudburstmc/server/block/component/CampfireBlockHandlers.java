package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.block.component.*;
import org.cloudburstmc.api.enchantment.EnchantmentTypes;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.Living;
import org.cloudburstmc.api.entity.damage.DamageSource;
import org.cloudburstmc.api.entity.damage.DamageTypes;
import org.cloudburstmc.api.entity.projectile.AbstractArrow;
import org.cloudburstmc.api.entity.projectile.SplashPotion;
import org.cloudburstmc.api.event.block.BlockIgniteCause;
import org.cloudburstmc.api.event.block.BlockIgniteEvent;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.particle.ParticleEmission;
import org.cloudburstmc.api.level.particle.ParticleTypes;
import org.cloudburstmc.api.level.sound.SoundTypes;
import org.cloudburstmc.api.potion.PotionTypes;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.LevelEvent;
import org.cloudburstmc.server.blockentity.CampfireBlockEntity;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.level.collision.CloudVoxelShapes;
import org.cloudburstmc.server.registry.CloudBlockEntityRegistry;

import java.util.List;
import java.util.Map;

import static org.cloudburstmc.api.blockentity.BlockEntityTypes.CAMPFIRE;

@UtilityClass
public class CampfireBlockHandlers {

    public static final PlacementStateHandler RESOLVE_PLACEMENT_STATE = (state, block, player, face, click) ->
            DefaultPlacementStateHandler.INSTANCE.execute(state, block, player, face, click)
                    .withTrait(BlockTraits.IS_EXTINGUISHED, isWater(block.getLiquid()) && block.getLiquid().isSource());

    public static final ComplexBlockHandler ON_ADDED = block -> {
        if (block.getLevel().getBlockEntity(block.getPosition()) == null) {
            CloudBlockEntityRegistry.get().newEntity(CAMPFIRE, block);
        }
    };

    public static final ComplexBlockHandler ON_STATE_CHANGED = block -> {
        if (block.getLevel().getBlockEntity(block.getPosition()) instanceof CampfireBlockEntity campfire) {
            campfire.scheduleUpdate();
        }
    };

    public static final UseBlockHandler USE = (block, player, face, item) -> {
        if (player == null) {
            return false;
        }

        if (item.getOrDefault(ItemDataComponents.ENCHANTMENTS, Map.of()).containsKey(EnchantmentTypes.FIRE_ASPECT) && ignite(block, player, BlockIgniteCause.FIRE_ASPECT)) {
            return true;
        }

        if (!(block.getLevel().getBlockEntity(block.getPosition()) instanceof CampfireBlockEntity campfire) || !campfire.addItem(item, player)) {
            return false;
        }

        if (!player.isCreative()) {
            player.getInventory().setSelectedItem(item.decreaseCount());
        }

        if (block.getLevel() instanceof CloudLevel level) {
            level.addLevelEvent(block.getPosition().toFloat().add(0.5f, 0.5f, 0.5f), LevelEvent.SOUND_ITEMFRAME_ITEM_ADD, 0);
        }

        return true;
    };

    public static final LiquidChangeBlockHandler ON_LIQUID_CHANGED = (block, previous) -> {
        if (!isWater(block.getLiquid()) || isWater(previous)) {
            return;
        }

        extinguish(block);
        if (block.getLevel().getBlockEntity(block.getPosition()) instanceof CampfireBlockEntity campfire) {
            campfire.ejectIngredients();
        }
    };

    public static final NeighborBlockHandler ON_NEIGHBOUR_CHANGED = (block, neighbour) -> {
        if (neighbour.getPosition().equals(block.getPosition().add(0, 1, 0)) && isWater(neighbour.getLiquid())) {
            extinguish(block);
        }
    };

    public static final VoxelShapeBlockHandler INSIDE_SHAPE = (state, context) -> CloudVoxelShapes.box(0, 0, 0, 1, 0.5f, 1);

    public static final EntityInsideBlockHandler ON_ENTITY_INSIDE = (block, entity, precise) -> {
        if (!(entity instanceof Living)) {
            return;
        }

        if (block.getState().ensureTrait(BlockTraits.IS_EXTINGUISHED)) {
            if (entity.isOnFire()) {
                ignite(block, entity, BlockIgniteCause.BURNING_ENTITY);
            }
        } else {
            int damage = block.getState().getType() == BlockTypes.SOUL_CAMPFIRE ? 2 : 1;
            entity.damage(damage, DamageSource.builder(DamageTypes.CAMPFIRE).block(block).build());
        }
    };

    public static final EntityBlockHandler ON_PROJECTILE_HIT = (block, projectile) -> {
        if (projectile instanceof SplashPotion potion && potion.getPotionType() == PotionTypes.WATER) {
            extinguish(block);
            return;
        }

        if (!projectile.isOnFire()) {
            return;
        }

        BlockIgniteCause cause = projectile instanceof AbstractArrow ? BlockIgniteCause.ARROW : BlockIgniteCause.FIREBALL;
        ignite(block, projectile, cause);
    };

    public static final BlockLootHandler GET_LOOT = (block, context) -> {
        if (context.enchantmentLevel(EnchantmentTypes.SILK_TOUCH) > 0) {
            return DefaultBlockHandlers.GET_LOOT.execute(block, context);
        }

        return block.getState().getType() == BlockTypes.SOUL_CAMPFIRE ? List.of(ItemStack.from(ItemTypes.SOUL_SOIL)) : List.of(ItemStack.from(ItemTypes.CHARCOAL, 2));
    };

    public static final ComplexBlockHandler ON_REMOVE = block -> {
        if (block.getLevel().getBlockEntity(block.getPosition()) instanceof CampfireBlockEntity campfire && !campfire.isValid()) {
            campfire.onBreak();
        }
    };

    private static boolean ignite(Block block, Entity source, BlockIgniteCause cause) {
        BlockState before = block.getState();
        BlockState ignited = block.requireComponent(BlockComponents.GET_IGNITED_STATE).execute(block);
        if (ignited == null) {
            return false;
        }

        BlockIgniteEvent event = new BlockIgniteEvent(block, cause, source, null);
        block.getLevel().getServer().getEventManager().fire(event);
        if (!event.isCancelled() && block.getLevel().getBlockState(block.getPosition()) == before && block.getLiquid().isEmpty()) {
            block.set(ignited, true);
            block.getLevel().playSound(block.getPosition(), SoundTypes.FIRE_IGNITE);
        }

        return true;
    }

    private static void extinguish(Block block) {
        BlockState state = block.getState();
        if (state.ensureTrait(BlockTraits.IS_EXTINGUISHED)) {
            return;
        }

        block.set(state.withTrait(BlockTraits.IS_EXTINGUISHED, true), true);
        block.getLevel().playSound(block.getPosition(), SoundTypes.RANDOM_FIZZ);
        Vector3f center = block.getPosition().toFloat().add(0.5f, 0.45f, 0.5f);
        block.getLevel().spawnParticle(new ParticleEmission(ParticleTypes.SMOKE, 10, Vector3f.from(0.2f, 0.1f, 0.2f)), center);
    }

    private static boolean isWater(LiquidState liquid) {
        return !liquid.isEmpty() && liquid.getType().isSameFamily(LiquidTypes.WATER);
    }
}
