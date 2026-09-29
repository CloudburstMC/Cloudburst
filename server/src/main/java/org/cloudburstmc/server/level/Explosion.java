package org.cloudburstmc.server.level;

import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.blockentity.BlockEntity;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.damage.DamageSource;
import org.cloudburstmc.api.entity.damage.DamageType;
import org.cloudburstmc.api.entity.damage.DamageTypes;
import org.cloudburstmc.api.entity.misc.PrimedTnt;
import org.cloudburstmc.api.event.block.BlockIgniteCause;
import org.cloudburstmc.api.event.block.BlockIgniteEvent;
import org.cloudburstmc.api.event.level.ExplosionEvent;
import org.cloudburstmc.api.item.ItemBehaviors;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.*;
import org.cloudburstmc.api.level.gamerule.GameRules;
import org.cloudburstmc.api.level.particle.ParticleTypes;
import org.cloudburstmc.api.player.Ability;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.BoundingBox;
import org.cloudburstmc.api.util.CollisionContext;
import org.cloudburstmc.api.util.MissHitResult;
import org.cloudburstmc.api.util.MissReason;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import java.util.*;

public class Explosion {

    private static final int RAY_GRID_SIZE = 16;
    private static final float RAY_STEP = 0.3f;

    private final CloudLevel level;
    private final Vector3f center;
    private final ExplosionSettings settings;

    public Explosion(CloudLevel level, Vector3f center, ExplosionSettings settings) {
        this.level = Objects.requireNonNull(level, "level");
        this.center = Objects.requireNonNull(center, "center");
        this.settings = Objects.requireNonNull(settings, "settings");

        if (!Float.isFinite(center.getX()) || !Float.isFinite(center.getY()) || !Float.isFinite(center.getZ())) {
            throw new IllegalArgumentException("Explosion center must be finite");
        }

        if (settings.sourceEntity() != null && settings.sourceEntity().getLevel() != level) {
            throw new IllegalArgumentException("Explosion source must be in the target level");
        }

        if (settings.sourceBlock() != null && settings.sourceBlock().getLevel() != level) {
            throw new IllegalArgumentException("Explosion source block must be in the target level");
        }
    }

    public boolean explode() {
        if (this.settings.radius() == 0) {
            return false;
        }

        List<Block> affected = this.settings.blockInteraction() == ExplosionBlockInteraction.KEEP && !this.settings.causesFire() ? List.of() : this.traceBlast();
        List<Block> blocks = this.settings.blockInteraction() == ExplosionBlockInteraction.KEEP ? new ArrayList<>() : new ArrayList<>(affected.stream()
                        .filter(block -> block.getState() != BlockStates.AIR).toList());
        float yield = this.settings.blockInteraction() == ExplosionBlockInteraction.DESTROY_WITH_DECAY ? Math.min(1, 1 / this.settings.radius()) : 1;
        this.damageEntities();

        boolean blocksCancelled = false;
        if (this.settings.blockInteraction() != ExplosionBlockInteraction.KEEP) {
            ExplosionEvent event = new ExplosionEvent(this.level, this.center, this.settings.sourceEntity(), this.settings.sourceBlock(), blocks, yield);
            this.level.getServer().getEventManager().fire(event);
            blocksCancelled = event.isCancelled();
            if (!blocksCancelled) {
                this.destroyBlocks(event.getBlocks(), event.getYield());
            }
        }

        if (this.settings.causesFire() && !blocksCancelled) {
            this.createFire(affected);
        }

        this.level.spawnParticle(ParticleTypes.LARGE_EXPLODE, this.center);
        this.level.addLevelSoundEvent(this.center, SoundEvent.EXPLODE);
        return true;
    }

    private List<Block> traceBlast() {
        Set<Vector3i> positions = new HashSet<>();
        for (int x = 0; x < RAY_GRID_SIZE; x++) {
            for (int y = 0; y < RAY_GRID_SIZE; y++) {
                for (int z = 0; z < RAY_GRID_SIZE; z++) {
                    if (x != 0 && x != 15 && y != 0 && y != 15 && z != 0 && z != 15) {
                        continue;
                    }

                    double dx = x / 15.0 * 2 - 1;
                    double dy = y / 15.0 * 2 - 1;
                    double dz = z / 15.0 * 2 - 1;

                    double scale = RAY_STEP / Math.sqrt(dx * dx + dy * dy + dz * dz);

                    double px = this.center.getX();
                    double py = this.center.getY();
                    double pz = this.center.getZ();

                    float power = this.settings.radius() * (0.7f + this.level.getRandom().nextFloat() * 0.6f);
                    while (power > 0) {
                        Vector3i position = Vector3i.from((int) Math.floor(px), (int) Math.floor(py), (int) Math.floor(pz));
                        if (this.level.isOutsideBuildHeight(position.getY())) {
                            break;
                        }

                        Block block = this.level.getLoadedBlock(position);
                        if (block == null) {
                            break;
                        }

                        BlockState primary = block.getState();
                        BlockState secondary = block.getSecondaryState();
                        if (primary != BlockStates.AIR || secondary != BlockStates.AIR) {
                            power -= (Math.max(primary.getExplosionResistance(), secondary.getExplosionResistance()) + 0.3f) * RAY_STEP;
                        }

                        if (power > 0) {
                            positions.add(position);
                        }

                        px += dx * scale;
                        py += dy * scale;
                        pz += dz * scale;
                        power -= RAY_STEP * 0.75f;
                    }
                }
            }
        }

        List<Block> blocks = new ArrayList<>(positions.size());
        for (Vector3i position : positions) {
            Block block = this.level.getLoadedBlock(position);
            if (block != null) {
                blocks.add(block);
            }
        }

        return blocks;
    }

    private void damageEntities() {
        float reach = this.settings.radius() * 2;
        BoundingBox bounds = new BoundingBox(this.center, this.center).inflate(reach + 1, reach + 1, reach + 1);

        Entity direct = this.settings.sourceEntity();
        Entity causing = direct != null && direct.getOwner() != null ? direct.getOwner() : direct;
        Block sourceBlock = this.settings.sourceBlock();

        boolean badRespawnPoint = sourceBlock != null && (sourceBlock.getState().is(BlockTags.BEDS)
                || sourceBlock.getState().getType() == BlockTypes.RESPAWN_ANCHOR);
        DamageType damageType = badRespawnPoint ? DamageTypes.BAD_RESPAWN_POINT
                : causing instanceof Player ? DamageTypes.PLAYER_EXPLOSION : DamageTypes.EXPLOSION;
        DamageSource.Builder source = DamageSource.builder(damageType)
                .damageLocation(Location.from(this.center, this.level));

        if (direct != null) {
            source.directEntity(direct).causingEntity(causing);
        } else if (sourceBlock != null) {
            source.block(sourceBlock);
        }

        DamageSource damageSource = source.build();
        for (Entity entity : this.level.getNearbyEntities(this.settings.sourceEntity(), bounds)) {
            float distance = entity.getPosition().distance(this.center) / reach;
            if (distance > 1) {
                continue;
            }

            Vector3f origin = entity instanceof PrimedTnt ? entity.getPosition() : entity.getPosition().add(0, entity.getEyeHeight(), 0);
            Vector3f offset = origin.sub(this.center);

            float impact = (1 - distance) * getSeenPercent(this.center, entity);
            entity.damage((impact * impact + impact) / 2 * 7 * reach + 1, damageSource);

            if (!(entity instanceof Player player && (player.isSpectator() || player.isCreative() && player.getAbilities().get(Ability.FLYING)))) {
                entity.setMotion(entity.getMotion().add(offset.lengthSquared() == 0 ? Vector3f.ZERO : offset.normalize().mul(impact)));
            }
        }
    }

    private void destroyBlocks(List<Block> blocks, float yield) {
        Set<Vector3i> processed = new HashSet<>();
        List<ExplosionDrop> collectedDrops = new ArrayList<>();

        for (Block selected : new ArrayList<>(blocks)) {
            if (selected == null || selected.getLevel() != this.level || !processed.add(selected.getPosition())) {
                continue;
            }

            Block block = this.level.getLoadedBlock(selected.getPosition());
            if (block == null || block.getState() == BlockStates.AIR) {
                continue;
            }

            if (!block.getComponents().require(BlockComponents.ON_EXPLOSION_HIT).execute(block, this.settings.sourceEntity(), this.settings.sourceBlock())) {
                continue;
            }

            List<ItemStack> drops = block.getComponents().require(BlockComponents.GET_EXPLOSION_LOOT)
                    .execute(block, new BlockLootContext(ItemStack.EMPTY, this.settings.sourceEntity(), this.level.getRandom()));
            BlockEntity blockEntity = this.level.getLoadedBlockEntity(block.getPosition());
            if (blockEntity != null) {
                blockEntity.onBreak();
                blockEntity.close();
                this.level.updateComparatorOutputLevel(block.getPosition());
            }

            block.getComponents().require(BlockComponents.ON_DESTROY).execute(block, this.settings.sourceEntity());
            if (!this.level.getGameRules().get(GameRules.DO_TILE_DROPS)) {
                continue;
            }

            for (ItemStack stack : drops) {
                if (stack == null || stack.isEmpty()) {
                    continue;
                }

                int count = 0;
                for (int i = 0; i < stack.getCount(); i++) {
                    if (this.level.getRandom().nextFloat() < yield) {
                        count++;
                    }
                }

                if (count > 0) {
                    collectDrop(collectedDrops, selected.getPosition(), stack.withCount(count));
                }
            }
        }

        for (ExplosionDrop drop : collectedDrops) {
            this.level.dropItem(drop.position(), drop.stack());
        }
    }

    private static void collectDrop(List<ExplosionDrop> drops, Vector3i position, ItemStack stack) {
        int maximum = Math.clamp(CloudItemRegistry.get()
                .requireComponent(stack.getType(), ItemBehaviors.GET_MAX_STACK_SIZE).execute(stack), 1, 16);
        int remaining = stack.getCount();

        for (int i = 0; i < drops.size() && remaining > 0; i++) {
            ExplosionDrop existing = drops.get(i);
            if (!existing.stack().isStackableWith(stack) || existing.stack().getCount() >= maximum) {
                continue;
            }

            int merged = Math.min(maximum - existing.stack().getCount(), remaining);
            drops.set(i, new ExplosionDrop(existing.position(), existing.stack().withCount(existing.stack().getCount() + merged)));
            remaining -= merged;
        }

        while (remaining > 0) {
            int count = Math.min(maximum, remaining);
            drops.add(new ExplosionDrop(position, stack.withCount(count)));
            remaining -= count;
        }
    }

    private void createFire(List<Block> blocks) {
        for (Block block : blocks) {
            if (this.level.getRandom().nextInt(3) != 0) {
                continue;
            }

            Vector3i position = block.getPosition();
            Block at = this.level.getLoadedBlock(position);
            Block below = this.level.getLoadedBlock(position.add(0, -1, 0));

            if (at != null && below != null && at.getState() == BlockStates.AIR && at.getSecondaryState() == BlockStates.AIR && below.getState().isSolid()) {
                BlockIgniteEvent event = new BlockIgniteEvent(at, BlockIgniteCause.EXPLOSION, this.settings.sourceEntity(), this.settings.sourceBlock());
                this.level.getServer().getEventManager().fire(event);
                if (event.isCancelled()) {
                    continue;
                }

                BlockType support = below.getState().getType();
                at.set((support == BlockTypes.SOUL_SAND || support == BlockTypes.SOUL_SOIL ? BlockTypes.SOUL_FIRE : BlockTypes.FIRE).getDefaultState());
            }
        }
    }

    public static float getSeenPercent(Vector3f center, Entity entity) {
        BoundingBox box = entity.getBoundingBox();

        double xStep = 1 / ((box.getMaxX() - box.getMinX()) * 2 + 1);
        double yStep = 1 / ((box.getMaxY() - box.getMinY()) * 2 + 1);
        double zStep = 1 / ((box.getMaxZ() - box.getMinZ()) * 2 + 1);

        double xOffset = (1 - Math.floor(1 / xStep) * xStep) / 2;
        double zOffset = (1 - Math.floor(1 / zStep) * zStep) / 2;

        int clear = 0;
        int samples = 0;

        for (double x = 0; x <= 1; x += xStep) {
            for (double y = 0; y <= 1; y += yStep) {
                for (double z = 0; z <= 1; z += zStep) {
                    Vector3f sample = Vector3f.from(
                            box.getMinX() + (box.getMaxX() - box.getMinX()) * x + xOffset,
                            box.getMinY() + (box.getMaxY() - box.getMinY()) * y,
                            box.getMinZ() + (box.getMaxZ() - box.getMinZ()) * z + zOffset
                    );

                    if (entity.getLevel().rayTraceBlocks(new RayTraceContext(sample, center,
                            BlockShapeMode.COLLIDER, FluidCollisionMode.NONE,
                            CollisionContext.of(entity))) instanceof MissHitResult miss
                            && miss.reason() == MissReason.CLEAR) {
                        clear++;
                    }

                    samples++;
                }
            }
        }

        return samples == 0 ? 0 : (float) clear / samples;
    }

    private record ExplosionDrop(Vector3i position, ItemStack stack) {
    }
}
