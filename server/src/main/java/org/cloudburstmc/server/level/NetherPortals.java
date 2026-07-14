package org.cloudburstmc.server.level;

import com.spotify.futures.CompletableFutures;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.hostile.ZombiePigman;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.level.gamerule.GameRules;
import org.cloudburstmc.api.util.BoundingBox;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.block.util.BlockSupport;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.level.chunk.CloudChunk;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudBlockRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.random.RandomGenerator;

/**
 * Static entry point for all nether portal operations: frame detection,
 * entity transfer, and portal construction.
 *
 * <p>Frame-specific geometry and mutation live on {@link PortalFrame}.
 * This class handles operations that require scanning or modifying the world
 * beyond a single frame's boundary.
 */
public final class NetherPortals {

    private static final Logger log = LogManager.getLogger(NetherPortals.class);

    private static final int MIN_WIDTH = 2;
    private static final int MAX_WIDTH = 21;
    private static final int MIN_HEIGHT = 3;
    private static final int MAX_HEIGHT = 21;

    private static final int SEARCH_RADIUS_NETHER = 16;
    private static final int SEARCH_RADIUS_OVERWORLD = 128;

    private static final int PLACE_Y_SEARCH_RANGE = 16;

    private static final int NEW_PORTAL_WIDTH = 2;
    private static final int NEW_PORTAL_HEIGHT = 3;

    private static final int PORTAL_SPAWN_DIFFICULTY_THRESHOLD = 2000;

    private NetherPortals() {
    }

    /**
     * Attempt to detect a valid nether portal frame containing {@code pos}.
     * Tests both X and Z axis orientations.
     *
     * @param level the level to scan
     * @param pos   any block position inside or adjacent to a portal frame
     * @return the frame if a valid frame is found, empty otherwise
     */
    public static Optional<PortalFrame> detect(CloudLevel level, Vector3i pos) {
        Optional<PortalFrame> result = detectAxis(level, pos, Direction.Axis.X);
        if (result.isPresent()) return result;
        return detectAxis(level, pos, Direction.Axis.Z);
    }

    private static Optional<PortalFrame> detectAxis(CloudLevel level, Vector3i pos, Direction.Axis axis) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        int minY = level.getMinHeight();

        // Scan down to locate the obsidian floor
        while (y > minY) {
            BlockState below = level.getBlockState(x, y - 1, z);
            if (below.getType() == BlockTypes.OBSIDIAN) {
                break;
            }
            if (!isInteriorBlock(below)) {
                return Optional.empty();
            }
            y--;
        }
        int bottomY = y;

        // Step along the axis to find the left-most interior column
        int leftX = x;
        int leftZ = z;
        for (int i = 0; i <= MAX_WIDTH; i++) {
            int nx = axis == Direction.Axis.X ? leftX - 1 : leftX;
            int nz = axis == Direction.Axis.Z ? leftZ - 1 : leftZ;
            BlockState state = level.getBlockState(nx, bottomY, nz);
            if (state.getType() == BlockTypes.OBSIDIAN) {
                break;
            }
            if (i == MAX_WIDTH) {
                return Optional.empty();
            }
            leftX = nx;
            leftZ = nz;
        }

        // Measure interior width
        int dx = axis == Direction.Axis.X ? 1 : 0;
        int dz = axis == Direction.Axis.Z ? 1 : 0;
        int width = 0;
        for (int i = 0; i <= MAX_WIDTH; i++) {
            BlockState state = level.getBlockState(leftX + dx * i, bottomY, leftZ + dz * i);
            if (state.getType() == BlockTypes.OBSIDIAN) {
                break;
            }
            width++;
            if (i == MAX_WIDTH) {
                return Optional.empty();
            }
        }
        if (width < MIN_WIDTH || width > MAX_WIDTH) {
            return Optional.empty();
        }

        // Measure interior height
        int height = 0;
        for (int j = 0; j <= MAX_HEIGHT; j++) {
            BlockState state = level.getBlockState(leftX, bottomY + j, leftZ);
            if (state.getType() == BlockTypes.OBSIDIAN) {
                break;
            }
            height++;
            if (j == MAX_HEIGHT) {
                return Optional.empty();
            }
        }
        if (height < MIN_HEIGHT || height > MAX_HEIGHT) {
            return Optional.empty();
        }

        if (!validateFrame(level, axis, leftX, leftZ, bottomY, width, height)) {
            return Optional.empty();
        }

        return Optional.of(new PortalFrame(Vector3i.from(leftX, bottomY, leftZ), width, height, axis));
    }

    public static boolean validateFrame(CloudLevel level, Direction.Axis axis, int leftX, int leftZ, int bottomY, int width, int height) {
        int dx = axis == Direction.Axis.X ? 1 : 0;
        int dz = axis == Direction.Axis.Z ? 1 : 0;

        for (int i = 0; i < width; i++) {
            if (level.getBlockState(leftX + dx * i, bottomY - 1, leftZ + dz * i).getType() != BlockTypes.OBSIDIAN) {
                return false;
            }
        }

        for (int i = 0; i < width; i++) {
            if (level.getBlockState(leftX + dx * i, bottomY + height, leftZ + dz * i).getType() != BlockTypes.OBSIDIAN) {
                return false;
            }
        }

        for (int j = 0; j < height; j++) {
            if (level.getBlockState(leftX - dx, bottomY + j, leftZ - dz).getType() != BlockTypes.OBSIDIAN) {
                return false;
            }
        }

        for (int j = 0; j < height; j++) {
            if (level.getBlockState(leftX + dx * width, bottomY + j, leftZ + dz * width).getType() != BlockTypes.OBSIDIAN) {
                return false;
            }
        }

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                if (!isInteriorBlock(level.getBlockState(leftX + dx * i, bottomY + j, leftZ + dz * i))) {
                    return false;
                }
            }
        }

        return true;
    }

    private static boolean isInteriorBlock(BlockState state) {
        BlockType type = state.getType();
        return type == BlockTypes.AIR
                || type == BlockTypes.FIRE
                || type == BlockTypes.SOUL_FIRE
                || type == BlockTypes.PORTAL;
    }

    /**
     * Scan for the nearest existing portal block to {@code center} within
     * {@code radius} blocks on each horizontal axis (full Y range searched).
     * Returns the portal block with the smallest squared XZ distance from
     * {@code center}, or empty if none exists within the radius.
     * On equal XZ distance, the candidate at the lower Y is preferred.
     */
    public static Optional<Vector3i> findNearestPortal(CloudLevel level, Vector3i center, int radius) {
        int cx = center.getX();
        int cz = center.getZ();
        int minY = level.getMinHeight();
        int maxY = level.getMaxHeight() - 1;

        Vector3i best = null;
        int bestDistSq = Integer.MAX_VALUE;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int bx = cx + dx;
                int bz = cz + dz;
                int distSq = dx * dx + dz * dz;

                if (distSq > bestDistSq) {
                    continue;
                }

                if (level.getLoadedChunk(bx >> 4, bz >> 4) == null) {
                    continue;
                }

                for (int by = minY; by <= maxY; by++) {
                    if (level.getBlockState(bx, by, bz).getType() == BlockTypes.PORTAL) {
                        if (distSq < bestDistSq || (best != null && by < best.getY())) {
                            best = Vector3i.from(bx, by, bz);
                            bestDistSq = distSq;
                        }
                        break;
                    }
                }
            }
        }
        return Optional.ofNullable(best);
    }

    /**
     * Build a standard obsidian portal frame and fill it with portal blocks
     * at the given position. The frame is 4 blocks wide and 5 blocks tall
     * (2x3 interior). Obsidian is also placed below the frame on both
     * perpendicular sides as a landing platform.
     *
     * @param level  the level to build in
     * @param origin bottom-left interior corner of the portal to create
     * @param axis   axis the portal faces
     */
    public static void createPortal(CloudLevel level, Vector3i origin, Direction.Axis axis) {
        int x = origin.getX();
        int y = origin.getY();
        int z = origin.getZ();
        int dx = axis == Direction.Axis.X ? 1 : 0;
        int dz = axis == Direction.Axis.Z ? 1 : 0;
        int px = dz;
        int pz = dx;

        BlockState obsidian = BlockTypes.OBSIDIAN.getDefaultState();
        BlockState air = BlockTypes.AIR.getDefaultState();
        BlockState portalState = BlockTypes.PORTAL.getDefaultState().withTrait(BlockTraits.PORTAL_AXIS, axis);

        for (int i = 0; i < NEW_PORTAL_WIDTH; i++) {
            for (int p = -1; p <= 1; p++) {
                int bx = x + dx * i + px * p;
                int bz = z + dz * i + pz * p;
                level.setBlockState(bx, y - 1, bz, 0, obsidian, false, true);
                for (int j = 0; j < NEW_PORTAL_HEIGHT; j++) {
                    level.setBlockState(bx, y + j, bz, 0, air, false, true);
                }
            }
        }

        for (int i = -1; i <= NEW_PORTAL_WIDTH; i++) {
            for (int j = -1; j <= NEW_PORTAL_HEIGHT; j++) {
                boolean isFrame = i == -1 || i == NEW_PORTAL_WIDTH || j == -1 || j == NEW_PORTAL_HEIGHT;
                if (!isFrame) continue;
                level.setBlockState(x + dx * i, y + j, z + dz * i, 0, obsidian, false, true);
            }
        }

        for (int i = 0; i < NEW_PORTAL_WIDTH; i++) {
            for (int j = 0; j < NEW_PORTAL_HEIGHT; j++) {
                level.setBlockState(x + dx * i, y + j, z + dz * i, 0, portalState, false, false);
            }
        }
    }

    /**
     * Find a safe Y position to place a new portal near {@code x, startY, z}.
     * Searches outward in an XZ spiral up to 16 blocks, then adjusts Y within
     * {@link #PLACE_Y_SEARCH_RANGE}, preferring the position closest to {@code startY}.
     */
    private static int findSafeY(CloudLevel level, int x, int startY, int z, Direction.Axis axis) {
        int minSafe = level.getMinHeight() + 2;
        int maxSafe = level.getMaxHeight() - 10;
        startY = Math.max(minSafe, Math.min(maxSafe, startY));

        for (int spiralR = 0; spiralR <= 16; spiralR++) {
            for (int sdx = -spiralR; sdx <= spiralR; sdx++) {
                for (int sdz = -spiralR; sdz <= spiralR; sdz++) {
                    if (Math.abs(sdx) != spiralR && Math.abs(sdz) != spiralR) continue;
                    int tx = x + sdx;
                    int tz = z + sdz;
                    for (int offset = 0; offset <= PLACE_Y_SEARCH_RANGE; offset++) {
                        int above = startY + offset;
                        if (above <= maxSafe && isSuitableForPortal(level, tx, above, tz, axis)) {
                            return above;
                        }

                        if (offset > 0) {
                            int below = startY - offset;
                            if (below >= minSafe && isSuitableForPortal(level, tx, below, tz, axis)) {
                                return below;
                            }
                        }
                    }
                }
            }
        }
        return startY;
    }

    /**
     * Returns true if the column at (x, y, z) is suitable to place a new portal frame:
     * the floor block at y-1 must support the portal, and the portal area plus a 1-block
     * perpendicular border on each side must all be replaceable and contain no fluid.
     */
    private static boolean isSuitableForPortal(CloudLevel level, int x, int y, int z, Direction.Axis axis) {
        int dx = axis == Direction.Axis.X ? 1 : 0;
        int dz = axis == Direction.Axis.Z ? 1 : 0;
        int px = dz;
        int pz = dx;

        BlockState floorState = level.getBlockState(x, y - 1, z);
        if (!BlockSupport.isFaceSturdy(floorState, Direction.UP, SupportType.FULL)) {
            return false;
        }

        for (int i = -1; i <= NEW_PORTAL_WIDTH; i++) {
            for (int j = 0; j < NEW_PORTAL_HEIGHT; j++) {
                for (int p = -1; p <= 1; p++) {
                    int bx = x + dx * i + px * p;
                    int bz = z + dz * i + pz * p;

                    BlockState state = level.getBlockState(bx, y + j, bz);
                    BlockType type = state.getType();

                    if (!CloudBlockRegistry.REGISTRY.getComponent(type, BlockComponents.REPLACEABLE).get()) {
                        return false;
                    }

                    if (type == BlockTypes.WATER || type == BlockTypes.FLOWING_WATER || type == BlockTypes.LAVA || type == BlockTypes.FLOWING_LAVA) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * Complete the nether portal transfer for an entity. Uses the pre-scaled
     * destination location produced by {@link EnumLevel#moveToNether}
     * to:
     * <ol>
     *   <li>Preload chunks around the destination.</li>
     *   <li>Search for an existing portal within the appropriate radius.</li>
     *   <li>Create a new portal at the destination if none is found.</li>
     *   <li>Teleport the entity to the portal entrance, preserving relative
     *       position within the portal and adjusting yaw when axes differ.</li>
     * </ol>
     *
     * @param entity    the entity being transferred
     * @param scaledLoc the pre-scaled destination location
     */
    public static void handlePortalTransfer(CloudEntity entity, Location scaledLoc) {
        if (!entity.isAlive() || entity.closed || entity.getVehicle() != null) {
            return;
        }

        if (entity.pendingPortalTransfer) {
            return;
        }

        entity.pendingPortalTransfer = true;

        CloudLevel targetLevel = (CloudLevel) scaledLoc.getLevel();
        boolean goingToNether = targetLevel == EnumLevel.NETHER.getLevel();
        int searchRadius = goingToNether ? SEARCH_RADIUS_NETHER : SEARCH_RADIUS_OVERWORLD;

        Direction.Axis sourceAxis = detectSourceAxis(entity);

        int chunkX = scaledLoc.getChunkX();
        int chunkZ = scaledLoc.getChunkZ();
        List<CompletableFuture<CloudChunk>> futures = new ArrayList<>();
        for (int cx = -1; cx <= 1; cx++) {
            for (int cz = -1; cz <= 1; cz++) {
                futures.add(targetLevel.getChunkFuture(chunkX + cx, chunkZ + cz));
            }
        }

        CompletableFutures.allAsList(futures).whenComplete((chunks, throwable) -> {
            if (throwable != null) {
                log.error("Chunk load failed for portal transfer", throwable);
                entity.pendingPortalTransfer = false;
                return;
            }

            if (chunks == null) {
                log.error("Chunk load returned null list during portal transfer");
                entity.pendingPortalTransfer = false;
                return;
            }

            entity.getServer().getGlobalScheduler().run(null, scheduledTask -> {
                if (!entity.isAlive() || entity.closed) {
                    entity.pendingPortalTransfer = false;
                    return;
                }

                int destX = scaledLoc.getFloorX();
                int destY = scaledLoc.getFloorY();
                int destZ = scaledLoc.getFloorZ();
                Vector3i destPos = Vector3i.from(destX, destY, destZ);

                Optional<Vector3i> existingPortal = findNearestPortal(targetLevel, destPos, searchRadius);

                Vector3f spawnPos;
                Direction.Axis destAxis;
                if (existingPortal.isPresent()) {
                    Vector3i portalPos = existingPortal.get();
                    BlockState portalBlock = targetLevel.getBlockState(portalPos);
                    destAxis = portalBlock.getType() == BlockTypes.PORTAL ? portalBlock.ensureTrait(BlockTraits.PORTAL_AXIS) : sourceAxis;

                    int bottomY = portalPos.getY();
                    int minY = targetLevel.getMinHeight();
                    while (bottomY > minY && targetLevel.getBlockState(portalPos.getX(), bottomY - 1, portalPos.getZ()).getType() == BlockTypes.PORTAL) {
                        bottomY--;
                    }

                    PortalExtents dest = findPortalExtents(targetLevel, portalPos.getX(), bottomY, portalPos.getZ(), destAxis);
                    float[] rel = PortalFrame.computeRelativePositionFor(entity, sourceAxis);
                    float relX = rel[0];
                    float relY = rel[1];
                    float perpendicularOffset = rel[2];

                    float entityWidth = entity.getWidth();
                    float entityHeight = entity.getHeight();
                    boolean destAxisX = destAxis == Direction.Axis.X;

                    double offsetAlongAxis = entityWidth / 2.0 + (dest.width() - entityWidth) * relX;
                    double offsetY = dest.height() > entityHeight ? (dest.height() - entityHeight) * relY : 0.0;

                    double ex = dest.leftX() + (destAxisX ? offsetAlongAxis : 0.5 + perpendicularOffset);
                    double ez = dest.leftZ() + (destAxisX ? 0.5 + perpendicularOffset : offsetAlongAxis);
                    double ey = bottomY + offsetY;
                    spawnPos = Vector3f.from((float) ex, (float) ey, (float) ez);
                } else {
                    destAxis = sourceAxis;
                    int safeY = findSafeY(targetLevel, destX, destY, destZ, destAxis);
                    Vector3i origin = Vector3i.from(destX, safeY, destZ);
                    createPortal(targetLevel, origin, destAxis);

                    PortalExtents dest = findPortalExtents(targetLevel, origin.getX(), origin.getY(), origin.getZ(), destAxis);
                    float[] rel = PortalFrame.computeRelativePositionFor(entity, sourceAxis);
                    float relX = rel[0];
                    float relY = rel[1];
                    float perpendicularOffset = rel[2];

                    float entityWidth = entity.getWidth();
                    float entityHeight = entity.getHeight();
                    boolean destAxisX = destAxis == Direction.Axis.X;

                    double offsetAlongAxis = entityWidth / 2.0 + (dest.width() - entityWidth) * relX;
                    double offsetY = dest.height() > entityHeight ? (dest.height() - entityHeight) * relY : 0.0;

                    double ex = dest.leftX() + (destAxisX ? offsetAlongAxis : 0.5 + perpendicularOffset);
                    double ez = dest.leftZ() + (destAxisX ? 0.5 + perpendicularOffset : offsetAlongAxis);
                    double ey = origin.getY() + offsetY;
                    spawnPos = Vector3f.from((float) ex, (float) ey, (float) ez);
                }

                float exitYaw = entity.getYaw();
                if (sourceAxis != destAxis) {
                    exitYaw += 90.0f;
                }

                spawnPos = findCollisionFreeSpawn(targetLevel, entity, spawnPos);
                targetLevel.addSound(spawnPos, Sound.PORTAL_TRAVEL);
                entity.teleport(Location.from(spawnPos, exitYaw, entity.getPitch(), targetLevel));
                if (entity instanceof CloudPlayer player) {
                    player.setChangingDimension(true);
                }

                entity.setMotion(Vector3f.ZERO);
                entity.portalCooldown = entity.getPortalCooldownTicks();
                entity.inPortalTicks = 0;
                entity.pendingPortalTransfer = false;
            });
        });
    }

    /**
     * Attempt to read the portal axis from the portal block recorded as the entity's
     * first point of contact this transfer cycle. Falls back to reading the block at
     * the entity's current foot position, then defaults to {@link Direction.Axis#X}.
     */
    private static Direction.Axis detectSourceAxis(CloudEntity entity) {
        Vector3i entry = entity.portalEntryBlock;
        if (entry != null) {
            BlockState state = entity.getLevel().getBlockState(entry);
            if (state.getType() == BlockTypes.PORTAL) {
                Direction.Axis axis = state.ensureTrait(BlockTraits.PORTAL_AXIS);
                if (axis == Direction.Axis.X || axis == Direction.Axis.Z) {
                    return axis;
                }
            }
        }

        Vector3f pos = entity.getPosition();
        BlockState state = entity.getLevel().getBlockState(pos.getFloorX(), pos.getFloorY(), pos.getFloorZ());
        if (state.getType() == BlockTypes.PORTAL) {
            Direction.Axis axis = state.ensureTrait(BlockTraits.PORTAL_AXIS);
            if (axis == Direction.Axis.X || axis == Direction.Axis.Z) {
                return axis;
            }
        }

        return Direction.Axis.X;
    }

    /**
     * Walk outward from a portal block to measure the extents of the portal
     * cluster it belongs to. Returns left edge coordinates, width, and height.
     */
    static PortalExtents findPortalExtents(CloudLevel level, int x, int y, int z, Direction.Axis axis) {
        int dx = axis == Direction.Axis.X ? 1 : 0;
        int dz = axis == Direction.Axis.Z ? 1 : 0;

        int bottomY = y;
        while (bottomY > level.getMinHeight()
                && level.getBlockState(x, bottomY - 1, z).getType() == BlockTypes.PORTAL) {
            bottomY--;
        }

        int leftX = x;
        int leftZ = z;
        for (int i = 0; i <= MAX_WIDTH; i++) {
            int nx = leftX - dx;
            int nz = leftZ - dz;
            if (level.getBlockState(nx, bottomY, nz).getType() != BlockTypes.PORTAL) {
                break;
            }
            leftX = nx;
            leftZ = nz;
        }

        int width = 0;
        for (int i = 0; i <= MAX_WIDTH; i++) {
            if (level.getBlockState(leftX + dx * i, bottomY, leftZ + dz * i).getType() != BlockTypes.PORTAL) {
                break;
            }
            width++;
        }

        int height = 0;
        for (int j = 0; j <= MAX_HEIGHT; j++) {
            if (level.getBlockState(leftX, bottomY + j, leftZ).getType() != BlockTypes.PORTAL) {
                break;
            }
            height++;
        }

        return new PortalExtents(leftX, leftZ, width, height);
    }

    /**
     * Attempt to spawn a Zombified Piglin from the portal block at {@code pos}.
     * Spawns only when difficulty is non-peaceful, the mob spawning gamerule
     * is enabled, random chance scales with difficulty, and a non-spectator
     * player is within 128 blocks.
     *
     * @param level  the level containing the portal
     * @param pos    the portal block position (any block in the portal column)
     * @param random the random source for this tick
     */
    public static void onPortalRandomTick(CloudLevel level, Vector3i pos, RandomGenerator random) {
        int difficulty = level.getDifficulty();
        if (difficulty <= 0) {
            return;
        }

        if (!level.getGameRules().get(GameRules.DO_MOB_SPAWNING)) {
            return;
        }

        if (random.nextInt(PORTAL_SPAWN_DIFFICULTY_THRESHOLD) >= difficulty) {
            return;
        }

        boolean playerNearby = false;
        for (CloudPlayer player : level.getPlayers().values()) {
            if (player.isSpectator()) {
                continue;
            }
            Vector3f ppos = player.getPosition();
            double distSq = (ppos.getX() - pos.getX()) * (ppos.getX() - pos.getX()) + (ppos.getZ() - pos.getZ()) * (ppos.getZ() - pos.getZ());
            if (distSq <= 128.0 * 128.0) {
                playerNearby = true;
                break;
            }
        }

        if (!playerNearby) {
            return;
        }

        int bx = pos.getX();
        int by = pos.getY();
        int bz = pos.getZ();
        while (by > level.getMinHeight() && level.getBlockState(bx, by - 1, bz).getType() == BlockTypes.PORTAL) {
            by--;
        }

        BlockState floor = level.getBlockState(bx, by - 1, bz);
        if (!BlockSupport.isFaceSturdy(floor, Direction.UP, SupportType.FULL)) {
            return;
        }

        // Require 2 non-blocking, non-fluid blocks of headroom at the spawn column
        for (int headroom = 0; headroom < 2; headroom++) {
            BlockState above = level.getBlockState(bx, by + headroom, bz);
            BlockType aboveType = above.getType();
            if (BlockSupport.blocksMotion(above)) {
                return;
            }

            if (aboveType == BlockTypes.WATER || aboveType == BlockTypes.FLOWING_WATER || aboveType == BlockTypes.LAVA || aboveType == BlockTypes.FLOWING_LAVA) {
                return;
            }
        }

        Vector3f spawnPos = Vector3f.from(bx + 0.5f, by, bz + 0.5f);
        Location loc = Location.from(spawnPos, level);
        ZombiePigman piglin = level.entityRegistry.newEntity(EntityTypes.ZOMBIE_PIGMAN, loc);
        if (piglin instanceof CloudEntity cloudEntity) {
            cloudEntity.portalCooldown = cloudEntity.getPortalCooldownTicks();
        }
        piglin.spawnToAll();
    }

    /**
     * Adjust {@code spawnPos} upward until the entity's bounding box at that
     * position no longer overlaps any motion-blocking block, or the search limit is reached.
     * Skips the search entirely for oversized entities (width or height {@literal >} 4).
     *
     * @param level    the destination level
     * @param entity   the entity being teleported (provides dimensions)
     * @param spawnPos the initially computed spawn position
     * @return a collision-free position, or the original if none found within the limit
     */
    private static Vector3f findCollisionFreeSpawn(CloudLevel level, CloudEntity entity, Vector3f spawnPos) {
        float width = entity.getWidth();
        float height = entity.getHeight();
        if (width > 4.0f || height > 4.0f) {
            return spawnPos;
        }

        float halfW = width / 2.0f;
        int maxY = level.getMaxHeight() - 1;

        for (int offset = 0; offset <= 4; offset++) {
            float testY = spawnPos.getY() + offset;
            if (testY + height > maxY) {
                break;
            }

            BoundingBox bb = new BoundingBox(
                    spawnPos.getX() - halfW, testY, spawnPos.getZ() - halfW,
                    spawnPos.getX() + halfW, testY + height, spawnPos.getZ() + halfW);
            if (!level.hasBlockCollision(entity, bb)) {
                return Vector3f.from(spawnPos.getX(), testY, spawnPos.getZ());
            }
        }
        return spawnPos;
    }

    /**
     * Measured extents of an existing portal cluster: bottom-left block
     * coordinates along each axis, plus interior width and height.
     */
    record PortalExtents(int leftX, int leftZ, int width, int height) {
    }
}
