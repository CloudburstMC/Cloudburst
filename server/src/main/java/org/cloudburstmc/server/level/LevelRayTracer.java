package org.cloudburstmc.server.level;

import lombok.experimental.UtilityClass;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.LiquidState;
import org.cloudburstmc.api.block.LiquidTypes;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.level.BlockShapeMode;
import org.cloudburstmc.api.level.FluidCollisionMode;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.RayTraceContext;
import org.cloudburstmc.api.util.BlockHitResult;
import org.cloudburstmc.api.util.BoundingBox;
import org.cloudburstmc.api.util.BoxIntersection;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.EntityHitResult;
import org.cloudburstmc.api.util.HitResult;
import org.cloudburstmc.api.util.MissHitResult;
import org.cloudburstmc.api.util.MissReason;
import org.cloudburstmc.api.util.VoxelShape;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.math.BlockRayTrace;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Performs level ray traces against block and liquid shapes and entity hitboxes.
 */
@UtilityClass
public class LevelRayTracer {

    public static HitResult rayTraceBlocks(Level level, RayTraceContext context) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(context, "context");

        Vector3f start = context.from();
        Vector3f end = context.to();
        if (start.equals(end)) {
            return new MissHitResult(end, MissReason.CLEAR);
        }

        for (Vector3i position : BlockRayTrace.of(start, end)) {
            Block block = level.getLoadedBlock(position);
            if (block == null) {
                BoxIntersection boundary = intersect(new BoundingBox(position.getX(), position.getY(), position.getZ(),
                        position.getX() + 1, position.getY() + 1, position.getZ() + 1), start, end);
                return new MissHitResult(boundary == null ? start : boundary.position(), MissReason.UNLOADED);
            }

            BlockHitResult closest = null;
            double closestDistance = Double.POSITIVE_INFINITY;
            VoxelShape shape = context.blocks() == BlockShapeMode.COLLIDER ? block.getCollisionShape(context.collision()) : block.getOutlineShape();

            for (BoundingBox localBox : shape.getBoundingBoxes()) {
                BoundingBox box = localBox.move(position.getX(), position.getY(), position.getZ());
                BoxIntersection intersection = intersect(box, start, end);
                if (intersection == null) {
                    continue;
                }

                double distance = start.distanceSquared(intersection.position());
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closest = new BlockHitResult(intersection.position(), block, intersection.face(), false);
                }
            }

            LiquidState liquid = block.getLiquid();
            if (matchesLiquid(context.fluids(), liquid)) {
                Block above = level.getLoadedBlock(position.add(0, 1, 0));
                float height = above != null && liquid.isSameFamily(above.getLiquid()) ? 1 : liquid.getOwnHeight();
                BoxIntersection intersection = intersect(new BoundingBox(position.getX(), position.getY(), position.getZ(),
                        position.getX() + 1, position.getY() + height, position.getZ() + 1), start, end);
                if (intersection != null && start.distanceSquared(intersection.position()) < closestDistance) {
                    closest = new BlockHitResult(intersection.position(), block, intersection.face(), true);
                }
            }

            if (closest != null) {
                return closest;
            }
        }

        return new MissHitResult(end, MissReason.CLEAR);
    }

    public static HitResult rayTraceEntities(Level level, Vector3f start, Vector3f end, float raySize, Predicate<? super Entity> filter) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        Objects.requireNonNull(filter, "filter");

        if (nonFinite(start) || nonFinite(end)) {
            throw new IllegalArgumentException("Ray endpoints must be finite");
        }

        if (!Float.isFinite(raySize) || raySize < 0) {
            throw new IllegalArgumentException("raySize must be finite and nonnegative");
        }

        if (start.equals(end)) {
            return new MissHitResult(end, MissReason.CLEAR);
        }

        BoundingBox search = new BoundingBox(
                Math.min(start.getX(), end.getX()) - raySize,
                Math.min(start.getY(), end.getY()) - raySize,
                Math.min(start.getZ(), end.getZ()) - raySize,
                Math.max(start.getX(), end.getX()) + raySize,
                Math.max(start.getY(), end.getY()) + raySize,
                Math.max(start.getZ(), end.getZ()) + raySize).inflate(1, 1, 1
        );

        EntityHitResult closest = null;
        double closestDistance = Double.POSITIVE_INFINITY;
        for (Entity candidate : level.getNearbyEntities(search, filter)) {
            BoundingBox box = candidate.getBoundingBox().inflate(raySize, raySize, raySize);
            BoxIntersection intersection = intersect(box, start, end);
            if (intersection == null) {
                continue;
            }

            double distance = start.distanceSquared(intersection.position());
            if (distance < closestDistance) {
                closestDistance = distance;
                closest = new EntityHitResult(intersection.position(), candidate);
            }
        }

        return closest == null ? new MissHitResult(end, MissReason.CLEAR) : closest;
    }

    public static HitResult rayTrace(Level level, RayTraceContext context, float raySize, Predicate<? super Entity> entityFilter) {
        HitResult blocks = rayTraceBlocks(level, context);
        HitResult entityHit = rayTraceEntities(level, context.from(), blocks.position(), raySize, entityFilter);
        return entityHit instanceof EntityHitResult ? entityHit : blocks;
    }

    private static boolean matchesLiquid(FluidCollisionMode mode, LiquidState liquid) {
        return !liquid.isEmpty() && switch (mode) {
            case NONE -> false;
            case SOURCE_ONLY -> liquid.isSource();
            case ANY -> true;
            case WATER -> liquid.getType().isSameFamily(LiquidTypes.WATER);
        };
    }

    private static @Nullable BoxIntersection intersect(BoundingBox box, Vector3f start, Vector3f end) {
        return box.contains(start.getX(), start.getY(), start.getZ()) ? new BoxIntersection(start, null) : box.intersectSegment(start, end);
    }

    private static boolean nonFinite(Vector3f vector) {
        return !Float.isFinite(vector.getX()) || !Float.isFinite(vector.getY()) || !Float.isFinite(vector.getZ());
    }
}
