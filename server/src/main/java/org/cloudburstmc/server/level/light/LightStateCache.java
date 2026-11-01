package org.cloudburstmc.server.level.light;

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.registry.BlockRegistry;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.VoxelShape;
import org.cloudburstmc.server.level.collision.CloudVoxelShapes;

public final class LightStateCache {

    private final BlockRegistry registry;
    private final Reference2ObjectOpenHashMap<BlockState, Entry> entries = new Reference2ObjectOpenHashMap<>();

    public LightStateCache(BlockRegistry registry) {
        this.registry = registry;
    }

    public int opacity(BlockState primary, BlockState secondary) {
        return Math.max(entry(primary).dampening(), entry(secondary).dampening());
    }

    public int emission(BlockState primary, BlockState secondary) {
        return Math.max(entry(primary).emission(), entry(secondary).emission());
    }

    public boolean occludes(BlockState fromPrimary, BlockState fromSecondary, BlockState toPrimary, BlockState toSecondary, Direction direction) {
        Direction opposite = direction.getOpposite();
        FaceKind fromPrimaryFace = face(fromPrimary, direction);
        if (fromPrimaryFace == FaceKind.FULL) {
            return true;
        }
        FaceKind fromSecondaryFace = face(fromSecondary, direction);
        if (fromSecondaryFace == FaceKind.FULL) {
            return true;
        }
        FaceKind toPrimaryFace = face(toPrimary, opposite);
        if (toPrimaryFace == FaceKind.FULL) {
            return true;
        }
        FaceKind toSecondaryFace = face(toSecondary, opposite);
        if (toSecondaryFace == FaceKind.FULL) {
            return true;
        }
        if (fromPrimaryFace == FaceKind.EMPTY && fromSecondaryFace == FaceKind.EMPTY
                && toPrimaryFace == FaceKind.EMPTY && toSecondaryFace == FaceKind.EMPTY) {
            return false;
        }
        return LightOcclusion.occludes(this.registry,
                new LightAccess.Sample(fromPrimary, fromSecondary, 0),
                new LightAccess.Sample(toPrimary, toSecondary, 0), direction);
    }

    private FaceKind face(BlockState state, Direction direction) {
        return entry(state).faces()[direction.ordinal()];
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
        return new Entry(state.getLightDampening(), state.getLightEmission(), faces);
    }

    private enum FaceKind {
        EMPTY,
        FULL,
        PARTIAL
    }

    private record Entry(int dampening, int emission, FaceKind[] faces) {
    }
}
