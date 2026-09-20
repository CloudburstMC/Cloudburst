package org.cloudburstmc.server.entity.component;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.LiquidTypes;
import org.cloudburstmc.api.entity.component.TickEntityHandler;
import org.cloudburstmc.api.entity.damage.DamageSource;
import org.cloudburstmc.api.entity.damage.DamageTypes;
import org.cloudburstmc.api.event.block.EntityBlockFormEvent;
import org.cloudburstmc.api.level.gamerule.GameRules;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.GenericMath;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.block.util.BlockSupport;
import org.cloudburstmc.server.entity.passive.EntitySnowGolem;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.level.biome.CloudBiome;
import org.cloudburstmc.server.registry.CloudBiomeRegistry;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SnowGolemEntityHandlers {

    public static final TickEntityHandler ON_TICK = (entity, currentTick) -> {
        EntitySnowGolem snowGolem = (EntitySnowGolem) entity;
        if (!snowGolem.isAlive()) {
            return false;
        }

        damageFromEnvironment(snowGolem);
        if (snowGolem.getLevel().getGameRules().get(GameRules.MOB_GRIEFING)) {
            placeSnowTrail(snowGolem);
        }

        return true;
    };

    private static void damageFromEnvironment(EntitySnowGolem snowGolem) {
        CloudLevel level = snowGolem.getLevel();
        Vector3i position = snowGolem.getPosition().toInt();
        CloudBiome biome = CloudBiomeRegistry.get().getBiome(level.getBiomeId(position.getX(), position.getY(), position.getZ()));
        if (biome != null && biome.temperatureAt(position.getX(), position.getY(), position.getZ(), level.getSeaLevel()) > 1) {
            snowGolem.damage(1, DamageSource.of(DamageTypes.ON_FIRE));
        }

        if (level.getBlock(position).getLiquid().getType().isSameFamily(LiquidTypes.WATER)) {
            snowGolem.damage(1, DamageSource.of(DamageTypes.DROWN));
        }
    }

    private static void placeSnowTrail(EntitySnowGolem snowGolem) {
        for (int index = 0; index < 4; index++) {
            int x = GenericMath.floor(snowGolem.getX() + ((index & 1) * 2 - 1) * 0.25f);
            int y = GenericMath.floor(snowGolem.getY());
            int z = GenericMath.floor(snowGolem.getZ() + (((index >> 1) & 1) * 2 - 1) * 0.25f);

            Block block = snowGolem.getLevel().getBlock(x, y, z);
            if (block.getState() != BlockStates.AIR) {
                continue;
            }

            Block below = block.getSide(Direction.DOWN);
            if (!BlockSupport.isFaceSturdy(snowGolem.getLevel(), below.getPosition(), Direction.UP)) {
                continue;
            }

            EntityBlockFormEvent event = new EntityBlockFormEvent(snowGolem, block, BlockStates.SNOW_LAYER);
            snowGolem.getServer().getEventManager().fire(event);
            if (!event.isCancelled()) {
                block.set(event.getNewState(), false, true);
            }
        }
    }
}
