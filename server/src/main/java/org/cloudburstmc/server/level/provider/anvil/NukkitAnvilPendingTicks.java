package org.cloudburstmc.server.level.provider.anvil;

import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtList;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.block.BlockStateCodec;
import org.cloudburstmc.server.level.chunk.CloudChunk;
import org.cloudburstmc.server.level.provider.migration.NukkitBlockStateConverter;

import java.io.IOException;
import java.util.*;

/**
 * Collects Anvil scheduled ticks by destination chunk before terrain conversion.
 */
public class NukkitAnvilPendingTicks {

    private final NukkitBlockStateConverter blocks;
    private final Map<Long, Map<TickKey, SavedTick>> chunks = new HashMap<>();

    public NukkitAnvilPendingTicks(NukkitBlockStateConverter blocks) {
        this.blocks = Objects.requireNonNull(blocks, "blocks");
    }

    /**
     * Collects a source chunk's ticks, preferring destination-owned entries when duplicates overlap.
     * Duplicates share a position and numeric block ID. Equal ownership favors the shorter delay,
     * then the lower priority value.
     */
    public void add(int sourceX, int sourceZ, NbtMap chunk) throws IOException {
        if (!chunk.containsKey("TileTicks")) {
            return;
        }

        if (!(chunk.get("TileTicks") instanceof NbtList<?> entries) || entries.getType() != NbtType.COMPOUND) {
            throw new IOException("Nukkit Anvil TileTicks must be a compound list");
        }

        for (Object entry : entries) {
            NbtMap tag = (NbtMap) entry;
            if (!tag.containsKey("x", NbtType.INT) || !tag.containsKey("y", NbtType.INT)
                    || !tag.containsKey("z", NbtType.INT) || !tag.containsKey("i", NbtType.STRING)
                    || !tag.containsKey("t", NbtType.INT)
                    || tag.containsKey("p") && !tag.containsKey("p", NbtType.INT)) {
                throw new IOException("Invalid Nukkit Anvil scheduled tick: " + tag);
            }

            Vector3i position = Vector3i.from(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
            long minX = (long) sourceX << 4;
            long minZ = (long) sourceZ << 4;
            if (position.getX() < minX - 2 || position.getX() >= minX + 16 || position.getZ() < minZ - 2 || position.getZ() >= minZ + 16) {
                throw new IOException("Scheduled tick is outside Nukkit's saved chunk border: " + position);
            }

            int id;
            try {
                id = this.blocks.readSaveId(tag.getString("i"));
            } catch (IllegalArgumentException failure) {
                throw new IOException("Invalid Nukkit Anvil scheduled tick at " + position, failure);
            }

            int x = position.getX() >> 4;
            int z = position.getZ() >> 4;
            SavedTick tick = new SavedTick(Math.max(0, tag.getInt("t")), tag.getInt("p"), x == sourceX && z == sourceZ);
            this.chunks.computeIfAbsent(CloudChunk.key(x, z), ignored -> new LinkedHashMap<>())
                    .merge(new TickKey(position, id), tick, NukkitAnvilPendingTicks::selectTick);
        }
    }

    /**
     * Removes and converts this destination chunk's ticks, omitting ticks for blocks no longer present.
     * The destination height range includes {@code minY} and excludes {@code maxY}.
     */
    public NbtMap take(int x, int z, int minY, int maxY, NbtMap chunk) throws IOException {
        Map<TickKey, SavedTick> ticks = this.chunks.remove(CloudChunk.key(x, z));
        if (ticks == null) {
            return NbtMap.EMPTY;
        }

        List<NbtMap> entries = new ArrayList<>(ticks.size());
        for (Map.Entry<TickKey, SavedTick> entry : ticks.entrySet()) {
            TickKey key = entry.getKey();
            Vector3i position = key.position();
            if (position.getY() < minY || position.getY() >= maxY) {
                throw new IOException("Scheduled tick is outside the destination height: " + position);
            }

            int metadata = matchingMetadata(chunk, position, key.id());
            if (metadata < 0) {
                continue;
            }

            SavedTick tick = entry.getValue();
            entries.add(NbtMap.builder()
                    .putInt("x", position.getX()).putInt("y", position.getY()).putInt("z", position.getZ())
                    .putCompound("blockState", BlockStateCodec.write(this.blocks.read(key.id(), metadata)))
                    .putLong("time", tick.delay()).putInt("p", tick.priority()).build());
        }

        return entries.isEmpty() ? NbtMap.EMPTY
                : NbtMap.builder().putInt("currentTick", 0).putList("tickList", NbtType.COMPOUND, entries).build();
    }

    /**
     * Fails if collected ticks still refer to chunks that were not imported.
     */
    public void requireConsumed() throws IOException {
        if (!this.chunks.isEmpty()) {
            throw new IOException("Nukkit Anvil scheduled ticks reference " + this.chunks.size() + " missing destination chunks");
        }
    }

    private static int matchingMetadata(NbtMap chunk, Vector3i position, int expectedId) {
        int version = chunk.getByte("V") & 0xff;
        for (NbtMap section : chunk.getList("Sections", NbtType.COMPOUND)) {
            if (section.getByte("Y") != position.getY() >> 4) {
                continue;
            }

            int x = position.getX() & 15;
            int y = position.getY() & 15;
            int z = position.getZ() & 15;
            if (version >= 2) {
                int value = section.getIntArray("layer1")[x << 8 | z << 4 | y];
                return value >>> 6 == expectedId ? value & 0x3f : -1;
            }

            int index = y << 8 | z << 4 | x;
            if ((section.getByteArray("Blocks")[index] & 0xff) != expectedId) {
                return -1;
            }

            return section.getByteArray("Data")[index >> 1] >>> ((index & 1) * 4) & 0xf;
        }

        return expectedId == 0 ? 0 : -1;
    }

    private static SavedTick selectTick(SavedTick first, SavedTick second) {
        if (first.owned() != second.owned()) {
            return first.owned() ? first : second;
        }

        int order = Integer.compare(first.delay(), second.delay());
        if (order == 0) {
            order = Integer.compare(first.priority(), second.priority());
        }

        return order <= 0 ? first : second;
    }

    private record TickKey(Vector3i position, int id) {}

    private record SavedTick(int delay, int priority, boolean owned) {}
}
