package org.cloudburstmc.server.block.component;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.component.*;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.gamerule.GameRules;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.BoundingBox;
import org.cloudburstmc.api.util.CollisionContext;
import org.cloudburstmc.api.util.VoxelShape;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.server.entity.EntityCreature;
import org.cloudburstmc.server.entity.EntityLiving;
import org.cloudburstmc.server.entity.misc.EntityFallingBlock;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.level.Sound;
import org.cloudburstmc.server.level.collision.CloudVoxelShapes;
import org.cloudburstmc.server.level.particle.DestroyBlockNoSoundParticle;
import org.cloudburstmc.server.level.particle.FizzEffectParticle;
import org.cloudburstmc.server.registry.EntityRegistry;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PowderSnowBlockHandlers {

    public static final BucketPickupHandler BUCKET_PICKUP = (block, player) ->
            ItemStack.from(ItemTypes.POWDER_SNOW_BUCKET);

    public static final CollisionShapeHandler COLLISION_SHAPE = new CollisionShapeHandler() {
        @Override
        public VoxelShape execute(BlockState state, BlockShapeContext blockContext, CollisionContext collisionContext) {
            Entity entity = collisionContext.getEntity();
            if (entity == null || !blockContext.hasLevel()) {
                return CloudVoxelShapes.empty();
            }

            float fallDistance = Math.max(0, entity.getHighestPosition() - entity.getY());
            if (collisionContext.isDescending() && fallDistance > 2.5f) {
                return CloudVoxelShapes.box(0, 0, 0, 1, 0.9f, 1);
            }

            if (entity instanceof EntityFallingBlock || canWalkOnPowderSnow(entity)
                    && collisionContext.isAbove(CloudVoxelShapes.block(), blockContext.position(), false)
                    && !collisionContext.isDescending()) {
                return CloudVoxelShapes.block();
            }

            return CloudVoxelShapes.empty();
        }

        @Override
        public ShapeContextRequirement contextRequirement() {
            return ShapeContextRequirement.ENTITY;
        }
    };

    public static final FallOnBlockHandler FALL_ON = (block, entity, fallDistance) -> {
        if (entity instanceof EntityLiving && fallDistance >= 4) {
            ((CloudLevel) block.getLevel()).addSound(entity.getPosition(), Sound.FALL_POWDER_SNOW);
        }
    };

    public static final EntityInsideBlockHandler ENTITY_INSIDE = (block, entity, precise) -> {
        entity.makeStuckInBlock(block.getState(), Vector3f.from(0.9f, 1.5f, 0.9f));
        if (entity instanceof EntityLiving livingEntity) {
            livingEntity.markInPowderSnow();
        }

        if (entity.isOnFire()) {
            extinguish(block, entity);
        }
    };

    private static void extinguish(Block block, Entity entity) {
        if (entity instanceof Player || block.getLevel().getGameRules().get(GameRules.MOB_GRIEFING)) {
            CloudLevel level = (CloudLevel) block.getLevel();
            BlockState state = block.getState();
            BoundingBox bounds = entity.getBoundingBox();
            level.addParticle(new FizzEffectParticle(Vector3f.from(bounds.getMinX(), bounds.getMinY(), bounds.getMinZ())));
            level.addParticle(new DestroyBlockNoSoundParticle(block.getPosition().toFloat(), state));
            level.addLevelSoundEvent(entity.getPosition(), SoundEvent.FIZZ);
            block.set(BlockStates.AIR);
        }

        entity.extinguish();
    }

    private static boolean canWalkOnPowderSnow(Entity entity) {
        return EntityRegistry.get().requireComponent(entity.getType(), EntityComponents.CAN_WALK_ON_POWDER_SNOW)
                .execute(entity)
                || entity instanceof EntityCreature creature
                && creature.getArmor().getBoots().getType() == ItemTypes.LEATHER_BOOTS;
    }
}
