package org.cloudburstmc.server.level.feature.tree;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.TreeType;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3i;

import java.util.*;
import java.util.random.RandomGenerator;

public class MangroveTreeGenerator {

    private static final Profile NORMAL = new Profile(TreeType.MANGROVE, 2, 4, 1, 3, 4, 2);
    private static final Profile TALL = new Profile(TreeType.TALL_MANGROVE, 4, 9, 3, 7, 6, 3);
    private static final List<Direction> HORIZONTAL = List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);
    private static final int MAX_ROOT_LENGTH = 15;
    private static final int MAX_ROOT_WIDTH = 8;

    private final Level level;
    private final RandomGenerator random;
    private final Map<Vector3i, Block> originals = new LinkedHashMap<>();
    private final Map<Vector3i, BlockState> changes = new LinkedHashMap<>();
    private final Set<Vector3i> leaves = new LinkedHashSet<>();
    private boolean unavailable;

    private MangroveTreeGenerator(Level level, RandomGenerator random) {
        this.level = level;
        this.random = random;
    }

    public static @Nullable Plan generate(Block propagule, RandomGenerator random) {
        MangroveTreeGenerator generator = new MangroveTreeGenerator(propagule.getLevel(), random);
        Profile profile = random.nextFloat() < 0.85f ? TALL : NORMAL;
        return generator.plan(propagule.getPosition(), profile);
    }

    private @Nullable Plan plan(Vector3i origin, Profile profile) {
        int height = profile.baseHeight() + this.random.nextInt(2) + this.random.nextInt(profile.randomHeight() + 1);
        Vector3i trunk = origin.add(0, this.random.nextInt(profile.minimumRootHeight(), profile.maximumRootHeight() + 1), 0);
        if (origin.getY() <= this.level.getMinHeight() || trunk.getY() + height + 1 >= this.level.getMaxHeight()) {
            return null;
        }

        for (int y = 0; y <= height + 1; y++) {
            int radius = y < profile.clearanceHeight() ? 0 : 2;
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockState state = this.read(trunk.add(x, y, z));
                    if (state == null || this.cannotPlaceLog(state) && !state.is(BlockTags.LOG)) {
                        return null;
                    }
                }
            }
        }

        this.put(origin, BlockStates.AIR);
        if (!this.placeRoots(origin, trunk)) {
            return null;
        }

        List<Vector3i> foliage = this.placeTrunk(trunk, height, profile);
        for (Vector3i attachment : foliage) {
            for (int i = 0; i < 70; i++) {
                Vector3i position = attachment.add(this.random.nextInt(3) - this.random.nextInt(3),
                        this.random.nextInt(2) - this.random.nextInt(2), this.random.nextInt(3) - this.random.nextInt(3));
                BlockState state = this.read(position);
                if (state != null && this.replaceable(state)) {
                    this.put(position, BlockTypes.MANGROVE_LEAVES.getDefaultState().withTrait(BlockTraits.IS_PERSISTENT, false));
                    this.leaves.add(position);
                }
            }
        }

        if (this.leaves.isEmpty()) {
            return null;
        }

        this.decorateLeaves();
        if (this.unavailable) {
            return null;
        }

        List<BlockChange> blocks = new ArrayList<>();
        this.changes.forEach((position, state) -> {
            Block original = this.originals.get(position);
            if (original.getState() != state) {
                blocks.add(new BlockChange(original, state));
            }
        });

        return new Plan(profile.type(), List.copyOf(blocks));
    }

    private boolean placeRoots(Vector3i origin, Vector3i trunk) {
        for (int y = origin.getY(); y < trunk.getY(); y++) {
            if (!this.canPlaceRoot(Vector3i.from(origin.getX(), y, origin.getZ()))) {
                return false;
            }
        }

        List<Vector3i> roots = new ArrayList<>();
        roots.add(trunk.add(0, -1, 0));
        for (Direction direction : HORIZONTAL) {
            Vector3i start = direction.relative(trunk);
            List<Vector3i> branch = new ArrayList<>();
            if (this.rootPathFails(start, direction, trunk, branch, 0)) {
                return false;
            }

            roots.addAll(branch);
            roots.add(start);
        }

        for (Vector3i root : roots) {
            BlockState current = this.read(root);
            if (current == null || !this.canPlaceRoot(root)) {
                continue;
            }

            boolean muddy = current.getType() == BlockTypes.MUD || current.getType() == BlockTypes.MUDDY_MANGROVE_ROOTS;
            this.put(root, (muddy ? BlockTypes.MUDDY_MANGROVE_ROOTS : BlockTypes.MANGROVE_ROOTS).getDefaultState());
            Vector3i above = root.add(0, 1, 0);
            if (!muddy && this.random.nextFloat() < 0.5f && this.read(above) == BlockStates.AIR) {
                this.put(above, BlockTypes.MOSS_CARPET.getDefaultState());
            }
        }

        return !this.unavailable;
    }

    private boolean rootPathFails(Vector3i position, Direction direction, Vector3i origin, List<Vector3i> roots, int depth) {
        if (depth == MAX_ROOT_LENGTH || roots.size() > MAX_ROOT_LENGTH) {
            return true;
        }

        Vector3i below = position.add(0, -1, 0);
        Vector3i forward = direction.relative(position);
        int width = Math.abs(position.getX() - origin.getX()) + Math.abs(position.getY() - origin.getY()) + Math.abs(position.getZ() - origin.getZ());
        List<Vector3i> candidates;
        if (width > MAX_ROOT_WIDTH - 3 && width <= MAX_ROOT_WIDTH) {
            candidates = this.random.nextFloat() < 0.2f ? List.of(below, forward.add(0, -1, 0)) : List.of(below);
        } else if (width > MAX_ROOT_WIDTH || this.random.nextFloat() < 0.2f) {
            candidates = List.of(below);
        } else {
            candidates = List.of(this.random.nextBoolean() ? forward : below);
        }

        for (Vector3i candidate : candidates) {
            if (this.canPlaceRoot(candidate)) {
                roots.add(candidate);
                if (this.rootPathFails(candidate, direction, origin, roots, depth + 1)) {
                    return true;
                }
            }
        }

        return this.unavailable;
    }

    private List<Vector3i> placeTrunk(Vector3i trunk, int height, Profile profile) {
        List<Vector3i> foliage = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            Vector3i position = trunk.add(0, y, 0);
            if (this.placeLog(position) && y < height - 1 && this.random.nextFloat() < 0.5f) {
                Direction direction = HORIZONTAL.get(this.random.nextInt(HORIZONTAL.size()));
                int steps = this.random.nextInt(1, profile.maximumBranchSteps() + 1);
                int lastY = position.getY();
                int x = position.getX();
                int z = position.getZ();

                for (int step = 1; step < height && step < steps; step++) {
                    Vector3i next = direction.relative(Vector3i.from(x, position.getY() + step, z));
                    x = next.getX();
                    z = next.getZ();
                    lastY = next.getY() + (this.placeLog(next) ? 1 : 0);
                    foliage.add(next);
                }

                if (lastY - position.getY() > 1) {
                    foliage.add(Vector3i.from(x, lastY, z));
                    foliage.add(Vector3i.from(x, lastY - 2, z));
                }
            }
        }

        foliage.add(trunk.add(0, height, 0));
        return foliage;
    }

    private boolean placeLog(Vector3i position) {
        BlockState state = this.read(position);
        if (state == null || this.cannotPlaceLog(state)) {
            return false;
        }

        this.put(position, BlockTypes.MANGROVE_LOG.getDefaultState());
        return true;
    }

    private void decorateLeaves() {
        List<Vector3i> shuffled = new ArrayList<>(this.leaves);
        for (int i = shuffled.size() - 1; i > 0; i--) {
            Collections.swap(shuffled, i, this.random.nextInt(i + 1));
        }

        Set<Vector3i> excluded = new HashSet<>();
        for (Vector3i leaf : shuffled) {
            Vector3i below = leaf.add(0, -1, 0);
            if (!excluded.contains(below) && this.random.nextFloat() < 0.14f && this.read(below) == BlockStates.AIR && this.read(below.add(0, -1, 0)) == BlockStates.AIR) {
                this.put(below, BlockStates.MANGROVE_PROPAGULE.withTrait(BlockTraits.IS_HANGING, true).withTrait(BlockTraits.PROPAGULE_STAGE, this.random.nextInt(5)));
                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        excluded.add(below.add(x, 0, z));
                    }
                }
            }

            for (Direction direction : HORIZONTAL) {
                if (this.random.nextFloat() >= 0.125f) {
                    continue;
                }

                Vector3i vine = direction.relative(leaf);
                int bit = switch (direction) {
                    case WEST -> 8;
                    case EAST -> 2;
                    case NORTH -> 1;
                    case SOUTH -> 4;
                    default -> throw new IllegalStateException("Non-horizontal vine direction");
                };

                for (int length = 0; length < 5 && this.read(vine) == BlockStates.AIR; length++) {
                    this.put(vine, BlockStates.VINE.withTrait(BlockTraits.VINE_DIRECTION_BITS, bit));
                    vine = vine.add(0, -1, 0);
                }
            }
        }
    }

    private boolean replaceable(BlockState state) {
        return state == BlockStates.AIR || state.is(BlockTags.REPLACEABLE_BY_TREES);
    }

    private boolean cannotPlaceLog(BlockState state) {
        return !this.replaceable(state) && !state.is(BlockTags.MANGROVE_LOGS_CAN_GROW_THROUGH);
    }

    private boolean canPlaceRoot(Vector3i position) {
        BlockState state = this.read(position);
        return state != null && (this.replaceable(state) || state.is(BlockTags.MANGROVE_ROOTS_CAN_GROW_THROUGH));
    }

    private @Nullable BlockState read(Vector3i position) {
        if (position.getY() < this.level.getMinHeight() || position.getY() >= this.level.getMaxHeight()) {
            this.unavailable = true;
            return null;
        }

        Block block = this.originals.computeIfAbsent(position, this.level::getLoadedBlock);
        if (block == null) {
            this.unavailable = true;
            return null;
        }

        return this.changes.getOrDefault(position, block.getState());
    }

    private void put(Vector3i position, BlockState state) {
        if (this.read(position) != null) {
            this.changes.put(position, state);
        }
    }

    public record Plan(TreeType type, List<BlockChange> blocks) {
    }

    private record Profile(TreeType type, int baseHeight, int randomHeight, int minimumRootHeight,
                           int maximumRootHeight, int maximumBranchSteps, int clearanceHeight) {
    }
}
