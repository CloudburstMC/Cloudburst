package org.cloudburstmc.server.entity.hostile;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockComponents;
import org.cloudburstmc.api.block.BlockLayer;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.damage.DamageSource;
import org.cloudburstmc.api.entity.damage.DamageTypeTags;
import org.cloudburstmc.api.entity.hostile.Silverfish;
import org.cloudburstmc.api.event.entity.EntityRemoveCause;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.level.gamerule.GameRules;
import org.cloudburstmc.api.level.particle.ParticleEmitter;
import org.cloudburstmc.api.level.particle.ParticleEmitterTypes;
import org.cloudburstmc.api.registry.BlockRegistry;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.block.component.InfestedBlockHandlers;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.level.chunk.CloudChunk;

public class EntitySilverfish extends EntityHostile implements Silverfish {

    private static final int WAKE_DELAY = 20;
    private static final int WAKE_HORIZONTAL_RADIUS = 10;
    private static final int WAKE_VERTICAL_RADIUS = 5;
    private static final int INFESTATION_CHANCE = 10;
    private static final Direction[] DIRECTIONS = Direction.values();

    private int wakeTicks;

    public EntitySilverfish(EntityType<Silverfish> type, Location location) {
        super(type, location);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setDefaultMaxHealth(8);
    }

    @Override
    protected String getDefaultName() {
        return "Silverfish";
    }

    @Override
    public float getWidth() {
        return 0.4f;
    }

    @Override
    public float getHeight() {
        return 0.3f;
    }

    @Override
    protected void onDamageApplied(DamageSource source) {
        if (this.isAlive() && this.wakeTicks == 0 && (source.getCausingEntity() != null || source.getDamageType().is(DamageTypeTags.ALWAYS_TRIGGERS_SILVERFISH))) {
            this.wakeTicks = WAKE_DELAY;
        }

        super.onDamageApplied(source);
    }

    @Override
    protected boolean tickEntity(int currentTick, int elapsedTicks) {
        boolean updated = super.tickEntity(currentTick, elapsedTicks);
        if (!this.isAlive()) {
            return updated;
        }

        if (this.wakeTicks > 0) {
            this.wakeTicks = Math.max(0, this.wakeTicks - elapsedTicks);
            if (this.wakeTicks == 0) {
                this.wakeNearbyInfestations();
            }
        }

        if (this.isAlive() && this.wakeTicks == 0 && this.getTarget() == null) {
            this.tryInfestNeighbor();
        }

        return updated && !this.isClosed();
    }

    private void wakeNearbyInfestations() {
        CloudLevel level = this.getLevel();
        BlockRegistry registry = level.getServer().getBlockRegistry();
        Vector3i origin = this.getPosition().toInt();
        for (int y = 0; Math.abs(y) <= WAKE_VERTICAL_RADIUS; y = nextOffset(y)) {
            int blockY = origin.getY() + y;
            if (level.isOutsideBuildHeight(blockY)) {
                continue;
            }

            for (int x = 0; Math.abs(x) <= WAKE_HORIZONTAL_RADIUS; x = nextOffset(x)) {
                for (int z = 0; Math.abs(z) <= WAKE_HORIZONTAL_RADIUS; z = nextOffset(z)) {
                    if (!this.isAlive() || this.getLevel() != level) {
                        return;
                    }

                    Block block = findInfestation(level, registry, origin.getX() + x, blockY, origin.getZ() + z);
                    if (block != null && InfestedBlockHandlers.wake(block, this) && level.getRandom().nextBoolean()) {
                        return;
                    }
                }
            }
        }
    }

    private void tryInfestNeighbor() {
        CloudLevel level = this.getLevel();
        if (!level.getGameRules().get(GameRules.MOB_GRIEFING) || level.getRandom().nextInt(INFESTATION_CHANCE) != 0) {
            return;
        }

        Direction direction = DIRECTIONS[level.getRandom().nextInt(DIRECTIONS.length)];
        Vector3i origin = this.getPosition().add(0f, 0.5f, 0f).toInt();
        Block neighbor = level.getLoadedBlock(direction.relative(origin));
        if (neighbor != null && InfestedBlockHandlers.infest(neighbor, this)) {
            level.spawnParticleEffect(new ParticleEmitter(ParticleEmitterTypes.SILVERFISH_GRIEF_EMITTER), this.getPosition());
            this.remove(EntityRemoveCause.ENTER_BLOCK);
        }
    }

    private static @Nullable Block findInfestation(CloudLevel level, BlockRegistry registry, int x, int y, int z) {
        CloudChunk chunk = level.getLoadedChunk(x >> 4, z >> 4);
        if (chunk == null) {
            return null;
        }

        BlockState state = chunk.getBlockState(x & 15, y, z & 15, BlockLayer.PRIMARY);
        return registry.getComponent(state.getType(), BlockComponents.INFESTATION_HOST) != null ? level.getLoadedBlock(x, y, z) : null;
    }

    private static int nextOffset(int offset) {
        return (offset <= 0 ? 1 : 0) - offset;
    }
}
