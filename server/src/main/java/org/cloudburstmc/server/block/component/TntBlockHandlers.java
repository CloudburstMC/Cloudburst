package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.component.BlockExplosionHandler;
import org.cloudburstmc.api.block.component.EntityBlockHandler;
import org.cloudburstmc.api.block.component.UseBlockHandler;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.misc.PrimedTnt;
import org.cloudburstmc.api.event.block.TntPrimeCause;
import org.cloudburstmc.api.event.block.TntPrimeEvent;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.level.gamerule.GameRules;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.LevelEvent;
import org.cloudburstmc.server.item.component.DefaultItemHandlers;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudEntityRegistry;

@UtilityClass
public class TntBlockHandlers {

    public static final UseBlockHandler USE = (block, player, face, item) -> {
        if (player instanceof CloudPlayer cloudPlayer && isIgnitionItem(item)) {
            cloudPlayer.getInventory().setSelectedItem(ignite(block, cloudPlayer, item));
            return true;
        }

        return false;
    };

    public static final BlockExplosionHandler ON_EXPLOSION_HIT = (block, source, sourceBlock) -> {
        CloudLevel level = (CloudLevel) block.getLevel();
        return !level.getGameRules().get(GameRules.TNT_EXPLODES)
                || spawnPrimed(block, block.getPosition().toFloat().add(0.5f, 0, 0.5f),
                        source, sourceBlock, TntPrimeCause.EXPLOSION, true);
    };

    public static final EntityBlockHandler ON_PROJECTILE_HIT = (block, projectile) -> {
        if (projectile.isOnFire()) {
            prime(block, projectile.getOwner(), TntPrimeCause.PROJECTILE);
        }
    };

    public static boolean prime(Block block, @Nullable Entity source, TntPrimeCause cause) {
        if (!isTnt(block)) {
            return false;
        }

        CloudLevel level = (CloudLevel) block.getLevel();
        if (!level.getGameRules().get(GameRules.TNT_EXPLODES)) {
            return false;
        }

        Vector3f position = block.getPosition().toFloat().add(0.5f, 0, 0.5f);
        if (!spawnPrimed(block, position, source, null, cause, false)) {
            return false;
        }

        block.set(BlockStates.AIR);
        return true;
    }

    public static ItemStack ignite(Block block, CloudPlayer player, ItemStack item) {
        if (!isIgnitionItem(item) || !prime(block, player, TntPrimeCause.PLAYER) || player.isCreative()) {
            return item;
        }

        if (item.getType() == ItemTypes.FLINT_AND_STEEL) {
            return DefaultItemHandlers.ON_DAMAGE.execute(item, 1, player);
        }

        return item.decreaseCount();
    }

    public static boolean isTnt(Block block) {
        return block.getState().getType().getTraits().contains(BlockTraits.EXPLODE);
    }

    private static boolean isIgnitionItem(ItemStack item) {
        return item.getType() == ItemTypes.FLINT_AND_STEEL || item.getType() == ItemTypes.FIRE_CHARGE;
    }

    private static @Nullable Entity ownerOf(@Nullable Entity source) {
        return source instanceof PrimedTnt primed && primed.getSource() != null ? primed.getSource() : source;
    }

    private static boolean spawnPrimed(Block block, Vector3f position, @Nullable Entity source,
                                       @Nullable Block sourceBlock, TntPrimeCause cause, boolean shortFuse) {
        CloudLevel level = (CloudLevel) block.getLevel();
        TntPrimeEvent event = new TntPrimeEvent(block, cause, source, sourceBlock);
        level.getServer().getEventManager().fire(event);
        if (event.isCancelled()) {
            return false;
        }

        PrimedTnt tnt = CloudEntityRegistry.get().newEntity(EntityTypes.TNT, Location.from(position, level));
        Entity owner = ownerOf(source);
        if (owner != null) {
            tnt.setSource(owner);
        }

        if (shortFuse) {
            int fuse = tnt.getFuse();
            tnt.setFuse(fuse / 8 + level.getRandom().nextInt(Math.max(1, fuse / 4)));
        }

        double angle = level.getRandom().nextDouble() * Math.PI * 2;
        tnt.setMotion(Vector3f.from(-Math.sin(angle) * 0.02, 0.2, -Math.cos(angle) * 0.02));
        if (!tnt.spawn()) {
            return false;
        }

        if (!shortFuse) {
            level.addLevelEvent(position, LevelEvent.SOUND_FUSE, 0);
        }

        return true;
    }
}
