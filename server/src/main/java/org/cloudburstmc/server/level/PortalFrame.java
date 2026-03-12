package org.cloudburstmc.server.level;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.entity.CloudEntity;

/**
 * Immutable description of a valid nether portal frame detected in the world.
 *
 * <p>A {@code PortalFrame} describes the geometry of a specific frame: its
 * position, size, and orientation. It does not hold a reference to the level
 * it was found in; operations that read or modify the world require a
 * {@link CloudLevel} to be passed explicitly.
 */
public record PortalFrame(
        Vector3i bottomLeft,
        int width,
        int height,
        Direction.Axis axis
) {
    /**
     * Returns true if every interior position in this frame already contains
     * a portal block. A complete frame requires no further filling.
     */
    public boolean isFull(CloudLevel level) {
        int dx = axis == Direction.Axis.X ? 1 : 0;
        int dz = axis == Direction.Axis.Z ? 1 : 0;
        int bx = bottomLeft.getX();
        int by = bottomLeft.getY();
        int bz = bottomLeft.getZ();
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                if (level.getBlockState(bx + dx * i, by + j, bz + dz * i).getType() != BlockTypes.PORTAL) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Returns true if the obsidian frame surrounding this portal is still intact.
     * Re-runs the full frame validation scan, so any missing obsidian block will
     * be detected regardless of which column or row was removed.
     */
    public boolean isFrameIntact(CloudLevel level) {
        return NetherPortals.validateFrame(
                level, axis,
                bottomLeft.getX(), bottomLeft.getZ(), bottomLeft.getY(),
                width, height
        );
    }

    /**
     * Fill every interior position in this frame with portal blocks.
     * Neighbor updates are suppressed during filling so partially-placed
     * portal blocks do not break themselves mid-fill.
     */
    public void fill(CloudLevel level) {
        BlockState portalState = BlockTypes.PORTAL.getDefaultState().withTrait(BlockTraits.PORTAL_AXIS, axis);
        int dx = axis == Direction.Axis.X ? 1 : 0;
        int dz = axis == Direction.Axis.Z ? 1 : 0;
        int bx = bottomLeft.getX();
        int by = bottomLeft.getY();
        int bz = bottomLeft.getZ();
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                level.setBlockState(bx + dx * i, by + j, bz + dz * i, 0, portalState, false, false);
            }
        }
    }

    /**
     * Compute the entity's relative position within a portal cluster at the
     * given entry block.
     *
     * <p>Returns a {@code float[3]} where:
     * <ul>
     *   <li>[0] {@code relX}               - position along the portal's horizontal axis</li>
     *   <li>[1] {@code relY}               - position along the portal's vertical axis</li>
     *   <li>[2] {@code perpendicularOffset} - signed depth offset from the portal mid-plane along the
     *                                         axis perpendicular to the portal face</li>
     * </ul>
     *
     * <p>Falls back to {@code [0.5, 0.0, 0.0]} if the entry block is unknown.
     *
     * @param entity the entity entering the portal
     * @param axis   the axis of the source portal
     */
    static float[] computeRelativePositionFor(CloudEntity entity, Direction.Axis axis) {
        Vector3i entry = entity.portalEntryBlock;
        if (entry == null) {
            return new float[]{0.5f, 0.0f, 0.0f};
        }

        CloudLevel level = entity.getLevel();
        NetherPortals.PortalExtents extents = NetherPortals.findPortalExtents(level, entry.getX(), entry.getY(), entry.getZ(), axis);
        int leftX = extents.leftX();
        int leftZ = extents.leftZ();
        int width = extents.width();
        int height = extents.height();

        int bottomY = entry.getY();
        while (bottomY > level.getMinHeight() && level.getBlockState(entry.getX(), bottomY - 1, entry.getZ()).getType() == BlockTypes.PORTAL) {
            bottomY--;
        }

        if (width <= 0) {
            return new float[]{0.5f, 0.0f, 0.0f};
        }

        float entityWidth = entity.getWidth();
        float entityHeight = entity.getHeight();

        float portalAxisPos = axis == Direction.Axis.X ? entity.getPosition().getX() : entity.getPosition().getZ();
        float leftEdge = axis == Direction.Axis.X ? leftX : leftZ;
        float relX;
        float axisRange = width - entityWidth;
        if (axisRange > 0) {
            relX = Math.max(0, Math.min(1, (portalAxisPos - (leftEdge + entityWidth / 2f)) / axisRange));
        } else {
            relX = 0.5f;
        }

        float relY;
        float yRange = height - entityHeight;
        if (yRange > 0) {
            relY = Math.max(0, Math.min(1, (entity.getPosition().getY() - bottomY) / yRange));
        } else {
            relY = 0.0f;
        }

        float perpPos = axis == Direction.Axis.X ? entity.getPosition().getZ() : entity.getPosition().getX();
        float leftPerpEdge = axis == Direction.Axis.X ? leftZ : leftX;
        float perpendicularOffset = perpPos - (leftPerpEdge + 0.5f);

        return new float[]{relX, relY, perpendicularOffset};
    }
}
