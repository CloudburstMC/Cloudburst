package org.cloudburstmc.server.diagnostics.spark;

import me.lucko.spark.common.platform.world.ChunkInfo;
import me.lucko.spark.common.platform.world.CountMap;
import me.lucko.spark.common.platform.world.WorldInfoProvider;
import org.cloudburstmc.api.level.gamerule.GameRule;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.level.chunk.CloudChunk;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.*;

/**
 * Reports loaded world counts, chunks and game rules to Spark, keyed by level ID.
 *
 * <p>Polling must run on the server thread, as required by {@link WorldInfoProvider#mustCallSync()}.</p>
 */
public class CloudSparkWorldInfoProvider implements WorldInfoProvider {

    private final CloudServer server;

    public CloudSparkWorldInfoProvider(CloudServer server) {
        this.server = Objects.requireNonNull(server, "server");
    }

    @Override
    public CountsResult pollCounts() {
        int entities = 0;
        int blockEntities = 0;
        int chunks = 0;

        for (CloudLevel level : this.server.getLevels()) {
            entities += level.getEntities().size();
            blockEntities += level.getBlockEntities().size();
            chunks += level.getChunks().size();
        }

        return new CountsResult(this.server.getOnlinePlayers().size(), entities, blockEntities, chunks);
    }

    @Override
    public ChunksResult<? extends ChunkInfo<?>> pollChunks() {
        ChunksResult<CloudChunkInfo> result = new ChunksResult<>();
        for (CloudLevel level : this.server.getLevels()) {
            List<CloudChunkInfo> chunks = new ArrayList<>();
            for (CloudChunk chunk : level.getChunks()) {
                CountMap<String> entityCounts = new CountMap.Simple<>(new HashMap<>());
                for (CloudEntity entity : chunk.getEntities()) {
                    entityCounts.increment(entity.getType().getId().toString());
                }

                for (CloudPlayer player : chunk.getPlayers()) {
                    entityCounts.increment(player.getType().getId().toString());
                }

                chunks.add(new CloudChunkInfo(chunk.getX(), chunk.getZ(), entityCounts));
            }

            result.put(level.getId(), chunks);
        }

        return result;
    }

    @Override
    public GameRulesResult pollGameRules() {
        GameRulesResult result = new GameRulesResult();
        for (GameRule<?> rule : this.server.getGameRuleRegistry().getRules()) {
            this.pollGameRule(result, rule);
        }
        return result;
    }

    /**
     * Reports data-pack information as unavailable.
     *
     * @return {@code null} to report unsupported data-pack polling
     */
    @Override
    public Collection<DataPackInfo> pollDataPacks() {
        return null;
    }

    private <T extends Comparable<T>> void pollGameRule(GameRulesResult result, GameRule<T> rule) {
        result.putDefault(rule.getName(), rule.serialize(rule.getDefaultValue()));
        for (CloudLevel level : this.server.getLevels()) {
            level.getGameRules().find(rule).ifPresent(value -> result.put(rule.getName(), level.getId(), rule.serialize(value)));
        }
    }

    private record CloudChunkInfo(int x, int z, CountMap<String> entityCounts) implements ChunkInfo<String> {
        @Override
        public int getX() {
            return this.x;
        }

        @Override
        public int getZ() {
            return this.z;
        }

        @Override
        public CountMap<String> getEntityCounts() {
            return this.entityCounts;
        }

        @Override
        public String entityTypeName(String type) {
            return type;
        }
    }
}
