package org.cloudburstmc.server.level.light;

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.registry.BlockRegistry;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.VoxelShape;
import org.cloudburstmc.server.level.collision.CloudVoxelShapes;

/**
 * Caches dampening, emission and face shapes per block state.
 *
 * <p>Not thread-safe; use one instance per pass.
 */
final class LightStateCache {

    private final BlockRegistry registry;
    private final Reference2ObjectOpenHashMap<BlockState, Entry> entries = new Reference2ObjectOpenHashMap<>();

    LightStateCache(BlockRegistry registry) {
        this.registry = registry;
    }

    int opacity(BlockState primary, BlockState secondary) {
        return opacity(entry(primary), entry(secondary));
    }

    int emission(BlockState primary, BlockState secondary) {
        return emission(entry(primary), entry(secondary));
    }

    boolean occludes(BlockState fromPrimary, BlockState fromSecondary, BlockState toPrimary, BlockState toSecondary, Direction direction) {
        return occludes(entry(fromPrimary), entry(fromSecondary), entry(toPrimary), entry(toSecondary), direction);
    }

    /**
     * Returns the cached entry for a state, for callers that reuse the same
     * states across several lookups within one cell visit.
     */
    Entry entryFor(BlockState state) {
        return entry(state);
    }

    int opacity(Entry primary, Entry secondary) {
        return Math.max(primary.dampening(), secondary.dampening());
    }

    int emission(Entry primary, Entry secondary) {
        return Math.max(primary.emission(), secondary.emission());
    }

    boolean occludes(Entry fromPrimary, Entry fromSecondary, Entry toPrimary, Entry toSecondary, Direction direction) {
        Direction opposite = direction.getOpposite();
        FaceKind fromPrimaryFace = fromPrimary.faces()[direction.ordinal()];
        if (fromPrimaryFace == FaceKind.FULL) {
            return true;
        }
        FaceKind fromSecondaryFace = fromSecondary.faces()[direction.ordinal()];
        if (fromSecondaryFace == FaceKind.FULL) {
            return true;
        }
        FaceKind toPrimaryFace = toPrimary.faces()[opposite.ordinal()];
        if (toPrimaryFace == FaceKind.FULL) {
            return true;
        }
        FaceKind toSecondaryFace = toSecondary.faces()[opposite.ordinal()];
        if (toSecondaryFace == FaceKind.FULL) {
            return true;
        }
        if (fromPrimaryFace == FaceKind.EMPTY && fromSecondaryFace == FaceKind.EMPTY
                && toPrimaryFace == FaceKind.EMPTY && toSecondaryFace == FaceKind.EMPTY) {
            return false;
        }
        return LightOcclusion.occludes(this.registry, fromPrimary.state(), fromSecondary.state(),
                toPrimary.state(), toSecondary.state(), direction);
    }

    private Entry entry(BlockState state) {
        Entry entry = this.entries.get(state);
        if (entry == null) {
            entry = create(state);
            this.entries.put(state, entry);
        }
        return entry;
    }

    private Entry create(BlockState state) {
        FaceKind[] faces = new FaceKind[Direction.values().length];
        for (Direction direction : Direction.values()) {
            VoxelShape face = LightOcclusion.faceShape(this.registry, state, direction);
            faces[direction.ordinal()] = face.isEmpty() ? FaceKind.EMPTY
                    : face.covers(CloudVoxelShapes.block()) ? FaceKind.FULL : FaceKind.PARTIAL;
        }
        return new Entry(state, state.getLightDampening(), state.getLightEmission(), faces);
    }

    private enum FaceKind {
        EMPTY,
        FULL,
        PARTIAL
    }

    record Entry(BlockState state, int dampening, int emission, FaceKind[] faces) {
    }
}
