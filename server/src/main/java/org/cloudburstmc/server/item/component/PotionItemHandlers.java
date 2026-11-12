package org.cloudburstmc.server.item.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.BlockTags;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.damage.DamageSource;
import org.cloudburstmc.api.entity.damage.DamageTypes;
import org.cloudburstmc.api.entity.projectile.SplashPotion;
import org.cloudburstmc.api.event.entity.EntityChangeBlockEvent;
import org.cloudburstmc.api.event.entity.PotionEffectCause;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.item.component.FinishUseHandler;
import org.cloudburstmc.api.item.component.UseHandler;
import org.cloudburstmc.api.item.component.UseOnHandler;
import org.cloudburstmc.api.level.particle.ParticleTypes;
import org.cloudburstmc.api.level.sound.SoundTypes;
import org.cloudburstmc.api.potion.PotionType;
import org.cloudburstmc.api.potion.PotionTypes;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.potion.CloudPotion;

import java.util.random.RandomGenerator;

@UtilityClass
public class PotionItemHandlers {

    public static final UseHandler DRINK = ConsumableItemHandlers.USE;

    public static final FinishUseHandler FINISH_DRINK = (item, entity) -> {
        if (!(entity instanceof CloudPlayer player)) {
            return item;
        }

        PotionType type = item.getOrDefault(ItemDataComponents.POTION_TYPE, PotionTypes.WATER);
        DamageSource source = DamageSource.of(DamageTypes.MAGIC, player);
        new CloudPotion(type).apply(player, 1.0, 1.0f, source, player, PotionEffectCause.POTION_DRINK);
        return ConsumableItemHandlers.afterConsumption(item, player);
    };

    public static final UseHandler THROW_SPLASH = (item, entity) -> throwPotion(item, entity, EntityTypes.SPLASH_POTION);

    public static final UseHandler THROW_LINGERING = (item, entity) -> throwPotion(item, entity, EntityTypes.LINGERING_POTION);

    public static final UseOnHandler POUR_WATER = (item, entity, position, face, _) -> {
        if (!(entity instanceof CloudPlayer player) || face == Direction.DOWN || item.getOrDefault(ItemDataComponents.POTION_TYPE, PotionTypes.WATER) != PotionTypes.WATER) {
            return null;
        }

        CloudLevel level = player.getLevel();
        Block target = level.getBlock(position);
        if (!target.getState().is(BlockTags.CONVERTIBLE_TO_MUD)) {
            return null;
        }

        EntityChangeBlockEvent event = new EntityChangeBlockEvent(player, target, BlockStates.MUD);
        player.getServer().getEventManager().fire(event);

        ItemStack held = player.getInventory().getSelectedItem();
        if (event.isCancelled()
                || player.getLevel() != level
                || !item.equals(held)
                || level.getBlockState(position) != target.getState()
                || !level.setBlockState(position, BlockStates.MUD, true, true)) {
            return held;
        }

        playPourEffects(level, position);
        return ConsumableItemHandlers.afterConsumption(item, player, ItemTypes.GLASS_BOTTLE);
    };

    private static void playPourEffects(CloudLevel level, Vector3i position) {
        level.playSound(position, SoundTypes.BOTTLE_EMPTY);

        RandomGenerator random = level.getRandom();
        Vector3f surface = position.toFloat().add(0, 1, 0);
        for (int i = 0; i < 5; i++) {
            Vector3f splash = surface.add(random.nextFloat(), 0, random.nextFloat());
            level.spawnParticle(ParticleTypes.WATER_SPLASH, splash);
        }
    }

    private static ItemStack throwPotion(ItemStack item, Entity entity, EntityType<? extends SplashPotion> type) {
        if (!(entity instanceof CloudPlayer player)) {
            return item;
        }

        PotionType potionType = item.getOrDefault(ItemDataComponents.POTION_TYPE, PotionTypes.WATER);
        SplashPotion projectile = player.launchProjectile(type, ThrowableItemHandlers.launchMotion(player, 0.5f, -20),
                potion -> potion.setPotionType(potionType));
        if (projectile == null) {
            return item;
        }

        ThrowableItemHandlers.playThrowSound(player, SoundEvent.THROW);
        return player.isCreative() ? item : item.decreaseCount();
    }
}
