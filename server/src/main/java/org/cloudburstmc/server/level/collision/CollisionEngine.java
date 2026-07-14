package org.cloudburstmc.server.level.collision;

import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockComponents;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.util.BoundingBox;
import org.cloudburstmc.api.util.CollisionContext;
import org.cloudburstmc.api.util.VoxelShape;
import org.cloudburstmc.math.GenericMath;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.registry.CloudBlockRegistry;

import java.util.*;
import java.util.function.Predicate;

@RequiredArgsConstructor
public final class CollisionEngine {

    private final CloudLevel level;

    public boolean hasCollision(@Nullable Entity entity, BoundingBox boundingBox, boolean includeEntities) {
        return this.hasBlockCollision(entity, boundingBox)
                || includeEntities && this.hasEntityCollision(entity, boundingBox);
    }

    public boolean hasBlockCollision(@Nullable Entity entity, BoundingBox boundingBox) {
        return this.scanBlockCollisions(entity, boundingBox, shape -> true);
    }

    public boolean hasBlockCollision(@Nullable Entity entity, BlockState state, Vector3i position, BoundingBox boundingBox) {
        VoxelShape collisionShape = CloudBlockRegistry.REGISTRY
                .getComponent(state.getType(), BlockComponents.GET_COLLISION_SHAPE)
                .execute(state, CollisionContext.of(entity));
        return !collisionShape.isEmpty()
                && collisionShape.overlaps(boundingBox, position.getX(), position.getY(), position.getZ());
    }

    public boolean collidesWithSuffocatingBlock(@Nullable Entity entity, BoundingBox boundingBox) {
        BlockCollisionIterator collisions = new BlockCollisionIterator(entity, boundingBox, true);
        while (collisions.hasNext()) {
            if (!collisions.next().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public Iterable<VoxelShape> getBlockCollisions(@Nullable Entity entity, BoundingBox boundingBox) {
        return () -> new BlockCollisionIterator(entity, boundingBox, false);
    }

    public List<VoxelShape> getEntityCollisions(@Nullable Entity entity, BoundingBox boundingBox) {
        if (entity == null) {
            return List.of();
        }

        List<VoxelShape> shapes = new ArrayList<>();
        for (Entity other : this.level.getCollidingEntities(entity, boundingBox.inflate(0.25f, 0.25f, 0.25f))) {
            if (other.canBeCollidedWith(entity) || other.isPushable()) {
                BoundingBox box = other.getBoundingBox();
                shapes.add(CloudVoxelShapes.box(
                        box.getMinX(), box.getMinY(), box.getMinZ(),
                        box.getMaxX(), box.getMaxY(), box.getMaxZ()
                ));
            }
        }

        return List.copyOf(shapes);
    }

    public boolean hasEntityCollision(@Nullable Entity entity, BoundingBox boundingBox) {
        return !this.getEntityCollisions(entity, boundingBox).isEmpty();
    }

    public boolean hasEntityCollision(@Nullable Entity entity, VoxelShape shape, Vector3i position) {
        return shape.anyBox(position.getX(), position.getY(), position.getZ(), (minX, minY, minZ, maxX, maxY, maxZ) ->
                this.hasEntityCollision(entity, new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ)));
    }

    public Optional<Vector3i> findSupportingBlock(Entity entity, BoundingBox boundingBox) {
        CollisionContext context = CollisionContext.of(entity);
        Vector3f entityPosition = entity.getPosition();
        Vector3i support = null;
        float supportDistance = Float.MAX_VALUE;

        for (BlockPosition position : this.intersectingBlockPositions(boundingBox)) {
            Block block = this.level.getLoadedBlock(position.x(), position.y(), position.z());
            if (block == null) {
                continue;
            }

            BlockState state = block.getState();
            VoxelShape collisionShape = block.getComponents().get(BlockComponents.GET_COLLISION_SHAPE).execute(state, context);
            if (collisionShape.isEmpty() || !collisionShape.overlaps(boundingBox, position.x(), position.y(), position.z())) {
                continue;
            }

            float dx = entityPosition.getX() - (position.x() + 0.5f);
            float dy = entityPosition.getY() - (position.y() + 0.5f);
            float dz = entityPosition.getZ() - (position.z() + 0.5f);
            float distance = dx * dx + dy * dy + dz * dz;
            if (distance < supportDistance || distance == supportDistance && compareBlockPosition(position, support) > 0) {
                support = Vector3i.from(position.x(), position.y(), position.z());
                supportDistance = distance;
            }
        }

        return Optional.ofNullable(support);
    }

    public Vector3f collideBoundingBox(@Nullable Entity entity, Vector3f movement, BoundingBox boundingBox) {
        BoundingBox searchBox = boundingBox.expandTowards(movement);
        List<VoxelShape> collisions = this.collectCollisions(entity, searchBox);
        return CloudVoxelShapes.collide(boundingBox, collisions, movement);
    }

    public List<VoxelShape> collectCollisions(@Nullable Entity entity, BoundingBox boundingBox) {
        List<VoxelShape> entityCollisions = this.getEntityCollisions(entity, boundingBox);
        List<VoxelShape> collisions = new ArrayList<>(entityCollisions.size() + 1);
        collisions.addAll(entityCollisions);
        for (VoxelShape blockCollision : this.getBlockCollisions(entity, boundingBox)) {
            collisions.add(blockCollision);
        }
        return List.copyOf(collisions);
    }

    private boolean scanBlockCollisions(@Nullable Entity entity, BoundingBox boundingBox, Predicate<VoxelShape> consumer) {
        for (VoxelShape shape : this.getBlockCollisions(entity, boundingBox)) {
            if (consumer.test(shape)) {
                return true;
            }
        }
        return false;
    }

    private Iterable<BlockPosition> intersectingBlockPositions(BoundingBox boundingBox) {
        int minX = GenericMath.floor(boundingBox.getMinX());
        int minY = GenericMath.floor(boundingBox.getMinY());
        int minZ = GenericMath.floor(boundingBox.getMinZ());
        int maxX = GenericMath.ceil(boundingBox.getMaxX());
        int maxY = GenericMath.ceil(boundingBox.getMaxY());
        int maxZ = GenericMath.ceil(boundingBox.getMaxZ());
        return () -> new BlockPositionIterator(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static int compareBlockPosition(BlockPosition position, @Nullable Vector3i other) {
        if (other == null) {
            return 1;
        }
        if (position.y() != other.getY()) {
            return Integer.compare(position.y(), other.getY());
        }
        if (position.z() != other.getZ()) {
            return Integer.compare(position.z(), other.getZ());
        }
        return Integer.compare(position.x(), other.getX());
    }

    private static boolean intersectsUnitBlock(BoundingBox boundingBox, int x, int y, int z) {
        return boundingBox.getMaxX() > x
                && boundingBox.getMinX() < x + 1
                && boundingBox.getMaxY() > y
                && boundingBox.getMinY() < y + 1
                && boundingBox.getMaxZ() > z
                && boundingBox.getMinZ() < z + 1;
    }

    private record BlockPosition(int x, int y, int z) {
    }

    private static final class BlockPositionIterator implements Iterator<BlockPosition> {
        private final int minX;
        private final int minY;
        private final int maxX;
        private final int maxY;
        private final int maxZ;
        private int x;
        private int y;
        private int z;
        private boolean hasNext;

        private BlockPositionIterator(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
            this.minX = minX;
            this.minY = minY;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
            this.x = minX;
            this.y = minY;
            this.z = minZ;
            this.hasNext = minX <= maxX && minY <= maxY && minZ <= maxZ;
        }

        @Override
        public boolean hasNext() {
            return this.hasNext;
        }

        @Override
        public BlockPosition next() {
            if (!this.hasNext) {
                throw new NoSuchElementException();
            }

            BlockPosition position = new BlockPosition(this.x, this.y, this.z);
            this.advance();
            return position;
        }

        private void advance() {
            if (this.y < this.maxY) {
                this.y++;
                return;
            }
            this.y = this.minY;

            if (this.x < this.maxX) {
                this.x++;
                return;
            }
            this.x = this.minX;

            if (this.z < this.maxZ) {
                this.z++;
                return;
            }
            this.hasNext = false;
        }
    }

    private final class BlockCollisionIterator implements Iterator<VoxelShape> {
        private final CollisionContext context;
        private final BoundingBox boundingBox;
        private final Iterator<BlockPosition> positions;
        private final boolean suffocatingOnly;
        private VoxelShape next;

        private BlockCollisionIterator(@Nullable Entity entity, BoundingBox boundingBox, boolean suffocatingOnly) {
            this.context = CollisionContext.of(entity);
            this.boundingBox = boundingBox;
            this.positions = intersectingBlockPositions(boundingBox).iterator();
            this.suffocatingOnly = suffocatingOnly;
        }

        @Override
        public boolean hasNext() {
            if (this.next != null) {
                return true;
            }

            while (this.positions.hasNext()) {
                BlockPosition position = this.positions.next();
                VoxelShape shape = this.nextCollision(position);
                if (shape != null) {
                    this.next = shape;
                    return true;
                }
            }

            return false;
        }

        @Override
        public VoxelShape next() {
            if (!this.hasNext()) {
                throw new NoSuchElementException();
            }

            VoxelShape result = this.next;
            this.next = null;
            return result;
        }

        private @Nullable VoxelShape nextCollision(BlockPosition position) {
            Block block = level.getLoadedBlock(position.x(), position.y(), position.z());
            if (block == null) {
                return !this.suffocatingOnly && intersectsUnitBlock(this.boundingBox, position.x(), position.y(), position.z())
                        ? CloudVoxelShapes.block().move(position.x(), position.y(), position.z())
                        : null;
            }

            BlockState state = block.getState();
            if (this.suffocatingOnly && !block.getComponents().get(BlockComponents.SUFFOCATING).execute(state)) {
                return null;
            }

            VoxelShape collisionShape = block.getComponents().get(BlockComponents.GET_COLLISION_SHAPE).execute(state, this.context);
            return !collisionShape.isEmpty() && collisionShape.overlaps(this.boundingBox, position.x(), position.y(), position.z())
                    ? collisionShape.move(position.x(), position.y(), position.z())
                    : null;
        }
    }
}
