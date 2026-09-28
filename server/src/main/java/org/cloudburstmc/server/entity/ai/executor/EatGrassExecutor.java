package org.cloudburstmc.server.entity.ai.executor;

import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.entity.EntityIntelligent;
import org.cloudburstmc.api.entity.ai.behavior.BehaviorExecutor;
import org.cloudburstmc.api.level.gamerule.GameRules;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityEventType;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.entity.passive.Animal;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.level.particle.DestroyBlockParticle;

/**
 * Sheep-specific grass eating executor. Plays the eat animation,
 * then destroys short grass or converts grass block to dirt.
 *
 * @author daoge_cmd
 */
public class EatGrassExecutor implements BehaviorExecutor {

    protected final int duration;
    protected int tickCounter;

    public EatGrassExecutor(int duration) {
        this.duration = duration;
    }

    public EatGrassExecutor() {
        this(40);
    }

    @Override
    public void onStart(EntityIntelligent entity) {
        tickCounter = 0;
        ((CloudEntity) entity).broadcastEntityEvent(EntityEventType.EAT_GRASS);
    }

    @Override
    public boolean execute(EntityIntelligent entity) {
        tickCounter++;
        return tickCounter < duration;
    }

    @Override
    public void onStop(EntityIntelligent entity) {
        var level = entity.getLevel();
        var loc = entity.getLocation();
        int x = (int) Math.floor(loc.getX());
        int y = (int) Math.floor(loc.getY());
        int z = (int) Math.floor(loc.getZ());

        // Spawn block break particle
        ((CloudLevel)level).addParticle(new DestroyBlockParticle(loc.getPosition(), BlockTypes.SHORT_GRASS.getDefaultState()));

        if (Boolean.FALSE.equals(level.getGameRules().get(GameRules.MOB_GRIEFING))) {
            return;
        }

        // Defer block modifications to sequential tick to avoid concurrent world writes
        entity.getBehaviorGroup().addSyncedAction(() -> {
            var feetBlock = level.getBlockState(x, y, z);
            if (feetBlock != null && feetBlock.getType() == BlockTypes.SHORT_GRASS) {
                ((CloudLevel) level).addParticle(new DestroyBlockParticle(Vector3f.from(x + 0.5f, y + 0.5f, z + 0.5f), feetBlock));
                level.setBlockState(x, y, z, BlockTypes.AIR.getDefaultState());
                onEatGrass(entity);
                return;
            }

            var belowBlock = level.getBlockState(x, y - 1, z);
            if (belowBlock != null && belowBlock.getType() == BlockTypes.GRASS_BLOCK) {
                ((CloudLevel) level).addParticle(new DestroyBlockParticle(Vector3f.from(x + 0.5f, y - 0.5f, z + 0.5f), belowBlock));
                level.setBlockState(x, y - 1, z, BlockTypes.DIRT.getDefaultState());
                onEatGrass(entity);
            }
        });
    }

    protected void onEatGrass(EntityIntelligent entity) {
        if (entity instanceof Animal animal && animal.isBaby()) {
            animal.setBaby(false);
        }
    }
}
