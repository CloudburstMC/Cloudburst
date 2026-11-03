package org.cloudburstmc.server.level.light;

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.registry.BlockRegistry;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.VoxelShape;
import org.cloudburstmc.server.level.collision.CloudVoxelShapes;

/**
 * Caches dampening, emission and face shapes per block state.
 * <p>
 * Instances are thread-confined.
 */
public class LightStateCache {

    private final BlockRegistry registry;
    private final Reference2ObjectOpenHashMap<BlockState, Entry> entries = new Reference2ObjectOpenHashMap<>();

    public LightStateCache(BlockRegistry registry) {
        this.registry = registry;
    }

    /**
     * Returns the cached light properties and face classifications for a state.
     */
    public Entry entryFor(BlockState state) {
        return entry(state);
    }

    /**
     * Returns the combined opacity of the two block layers.
     */
    public int opacity(BlockState primary, BlockState secondary) {
        return opacity(entry(primary), entry(secondary));
    }

    /**
     * Returns the combined opacity of the two cached layer entries.
     */
    public int opacity(Entry primary, Entry secondary) {
        return Math.max(primary.dampening(), secondary.dampening());
    }

    /**
     * Returns the greatest light emission of the two block layers.
     */
    public int emission(BlockState primary, BlockState secondary) {
        return emission(entry(primary), entry(secondary));
    }

    /**
     * Returns the greatest emission of the two cached layer entries.
     */
    public int emission(Entry primary, Entry secondary) {
        return Math.max(primary.emission(), secondary.emission());
    }

    /**
     * Returns whether the layers seal the shared face in the source-to-target direction.
     */
    public boolean occludes(BlockState fromPrimary, BlockState fromSecondary, BlockState toPrimary, BlockState toSecondary, Direction direction) {
        return occludes(entry(fromPrimary), entry(fromSecondary), entry(toPrimary), entry(toSecondary), direction);
    }

    /**
     * Tests the shared face using cached source and target entries.
     */
    public boolean occludes(Entry fromPrimary, Entry fromSecondary, Entry toPrimary, Entry toSecondary, Direction direction) {
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

        if (fromPrimaryFace == FaceKind.EMPTY && fromSecondaryFace == FaceKind.EMPTY && toPrimaryFace == FaceKind.EMPTY && toSecondaryFace == FaceKind.EMPTY) {
            return false;
        }

        return LightOcclusion.occludes(this.registry, fromPrimary.state(), fromSecondary.state(), toPrimary.state(), toSecondary.state(), direction);
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

    /**
     * Coverage of a block state's face for light occlusion.
     */
    public enum FaceKind {
        EMPTY,
        FULL,
        PARTIAL
    }

    /**
     * Cached properties of one block state.
     *
     * @param state     the block state
     * @param dampening the light attenuation
     * @param emission  the emitted light level
     * @param faces     direction-indexed classifications, which callers must not modify
     */
    public record Entry(BlockState state, int dampening, int emission, FaceKind[] faces) {
    }
}
