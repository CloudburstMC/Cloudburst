package org.cloudburstmc.server.level;

import co.aikar.timings.Timing;
import co.aikar.timings.TimingsHistory;
import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.collect.ImmutableSet;
import com.nukkitx.math.vector.Vector2i;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.math.vector.Vector4i;
import com.nukkitx.protocol.bedrock.BedrockPacket;
import com.nukkitx.protocol.bedrock.data.LevelEventType;
import com.nukkitx.protocol.bedrock.data.SoundEvent;
import com.nukkitx.protocol.bedrock.packet.*;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.longs.*;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.shorts.ShortOpenHashSet;
import it.unimi.dsi.fastutil.shorts.ShortSet;
import lombok.Synchronized;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.cloudburstmc.server.Server;
import org.cloudburstmc.server.block.*;
import org.cloudburstmc.server.block.behavior.BlockBehavior;
import org.cloudburstmc.server.block.behavior.BlockBehaviorLiquid;
import org.cloudburstmc.server.block.behavior.BlockBehaviorRedstoneDiode;
import org.cloudburstmc.server.block.behavior.BlockBehaviorSlab;
import org.cloudburstmc.server.block.util.BlockUtils;
import org.cloudburstmc.server.blockentity.BlockEntity;
import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.entity.EntityTypes;
import org.cloudburstmc.server.entity.impl.BaseEntity;
import org.cloudburstmc.server.entity.impl.projectile.EntityArrow;
import org.cloudburstmc.server.entity.misc.DroppedItem;
import org.cloudburstmc.server.entity.misc.ExperienceOrb;
import org.cloudburstmc.server.entity.misc.LightningBolt;
import org.cloudburstmc.server.event.block.BlockBreakEvent;
import org.cloudburstmc.server.event.block.BlockPlaceEvent;
import org.cloudburstmc.server.event.block.BlockUpdateEvent;
import org.cloudburstmc.server.event.entity.ItemSpawnEvent;
import org.cloudburstmc.server.event.level.*;
import org.cloudburstmc.server.event.player.PlayerInteractEvent;
import org.cloudburstmc.server.event.weather.LightningStrikeEvent;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemBucket;
import org.cloudburstmc.server.item.ItemIds;
import org.cloudburstmc.server.item.enchantment.Enchantment;
import org.cloudburstmc.server.level.chunk.Chunk;
import org.cloudburstmc.server.level.chunk.ChunkSection;
import org.cloudburstmc.server.level.gamerule.GameRuleMap;
import org.cloudburstmc.server.level.gamerule.GameRules;
import org.cloudburstmc.server.level.generator.Generator;
import org.cloudburstmc.server.level.manager.LevelChunkManager;
import org.cloudburstmc.server.level.particle.DestroyBlockParticle;
import org.cloudburstmc.server.level.particle.Particle;
import org.cloudburstmc.server.level.provider.LevelProvider;
import org.cloudburstmc.server.math.*;
import org.cloudburstmc.server.metadata.BlockMetadataStore;
import org.cloudburstmc.server.metadata.MetadataValue;
import org.cloudburstmc.server.metadata.Metadatable;
import org.cloudburstmc.server.player.GameMode;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.plugin.Plugin;
import org.cloudburstmc.server.potion.Effect;
import org.cloudburstmc.server.registry.BlockRegistry;
import org.cloudburstmc.server.registry.EntityRegistry;
import org.cloudburstmc.server.registry.GeneratorRegistry;
import org.cloudburstmc.server.registry.RegistryException;
import org.cloudburstmc.server.scheduler.BlockUpdateScheduler;
import org.cloudburstmc.server.timings.LevelTimings;
import org.cloudburstmc.server.utils.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * author: MagicDroidX Nukkit Project
 */
@Log4j2
public class Level implements ChunkManager, Metadatable {

    private static final int levelIdCounter = 1;
    private static final int chunkLoaderCounter = 1;
    public static int COMPRESSION_LEVEL = 8;

    public static final int BLOCK_UPDATE_NORMAL = 1;
    public static final int BLOCK_UPDATE_RANDOM = 2;
    public static final int BLOCK_UPDATE_SCHEDULED = 3;
    public static final int BLOCK_UPDATE_WEAK = 4;
    public static final int BLOCK_UPDATE_TOUCH = 5;
    public static final int BLOCK_UPDATE_REDSTONE = 6;
    public static final int BLOCK_UPDATE_TICK = 7;

    public static final int TIME_DAY = 0;
    public static final int TIME_NOON = 6000;
    public static final int TIME_SUNSET = 12000;
    public static final int TIME_NIGHT = 14000;
    public static final int TIME_MIDNIGHT = 18000;
    public static final int TIME_SUNRISE = 23000;

    public static final int TIME_FULL = 24000;

    public static final int DIMENSION_OVERWORLD = 0;
    public static final int DIMENSION_NETHER = 1;
    public static final int DIMENSION_THE_END = 2;

    // Lower values use less memory
    public static final int MAX_BLOCK_CACHE = 512;

    // The blocks that can randomly tick
    private static final Set<Identifier> randomTickBlocks = Collections.newSetFromMap(new IdentityHashMap<>());

    static {
        randomTickBlocks.add(BlockTypes.GRASS);
        randomTickBlocks.add(BlockTypes.FARMLAND);
        randomTickBlocks.add(BlockTypes.MYCELIUM);
        randomTickBlocks.add(BlockTypes.SAPLING);
        randomTickBlocks.add(BlockTypes.LEAVES);
        randomTickBlocks.add(BlockTypes.LEAVES2);
        randomTickBlocks.add(BlockTypes.SNOW_LAYER);
        randomTickBlocks.add(BlockTypes.ICE);
        randomTickBlocks.add(BlockTypes.FLOWING_LAVA);
        randomTickBlocks.add(BlockTypes.LAVA);
        randomTickBlocks.add(BlockTypes.CACTUS);
        randomTickBlocks.add(BlockTypes.BEETROOT);
        randomTickBlocks.add(BlockTypes.CARROTS);
        randomTickBlocks.add(BlockTypes.POTATOES);
        randomTickBlocks.add(BlockTypes.MELON_STEM);
        randomTickBlocks.add(BlockTypes.PUMPKIN_STEM);
        randomTickBlocks.add(BlockTypes.WHEAT);
        randomTickBlocks.add(BlockTypes.REEDS);
        randomTickBlocks.add(BlockTypes.RED_MUSHROOM);
        randomTickBlocks.add(BlockTypes.BROWN_MUSHROOM);
        randomTickBlocks.add(BlockTypes.NETHER_WART_BLOCK);
        randomTickBlocks.add(BlockTypes.FIRE);
        randomTickBlocks.add(BlockTypes.LIT_REDSTONE_ORE);
        randomTickBlocks.add(BlockTypes.COCOA);
    }

    private final Set<BlockEntity> blockEntities = Collections.newSetFromMap(new IdentityHashMap<>());

    private final Long2ObjectOpenHashMap<Player> players = new Long2ObjectOpenHashMap<>();

    private final Long2ObjectOpenHashMap<Entity> entities = new Long2ObjectOpenHashMap<>();
    private static final RemovalListener<Long, ByteBuf> cacheRemover = notification -> notification.getValue().release();

    private final ConcurrentLinkedQueue<BlockEntity> updateBlockEntities = new ConcurrentLinkedQueue<>();

    private final Server server;
    public final LevelTimings timings;

    private LevelProvider provider;

    private final Int2ObjectOpenHashMap<ChunkLoader> loaders = new Int2ObjectOpenHashMap<>();

    private final Int2IntMap loaderCounter = new Int2IntOpenHashMap();
    private final Set<Entity> updateEntities = ConcurrentHashMap.newKeySet();

    private final Long2ObjectOpenHashMap<Deque<BedrockPacket>> chunkPackets = new Long2ObjectOpenHashMap<>();

    public float skyLightSubtracted;
    // Avoid OOM, gc'd references result in whole chunk being sent (possibly higher cpu)
    private final Cache<Long, IntSet> changedBlocks = CacheBuilder.newBuilder().softValues().build();
    //    private final Long2ObjectOpenHashMap<SoftReference<Map<Character, Object>>> changedBlocks = new Long2ObjectOpenHashMap<>();
    // Storing the vector is redundant
    private final Object changeBlocksPresent = new Object();
    private final Long2ObjectMap<ShortSet> lightQueue = new Long2ObjectOpenHashMap<>();
    // Storing extra blocks past 512 is redundant
    private final Map<Character, Object> changeBlocksFullMap = new HashMap<Character, Object>() {
        @Override
        public int size() {
            return Character.MAX_VALUE;
        }
    };


    private final BlockUpdateScheduler updateQueue;
    private final Queue<Block> normalUpdateQueue = new ConcurrentLinkedDeque<>();
//    private final TreeSet<BlockUpdateEntry> updateQueue = new TreeSet<>();
//    private final List<BlockUpdateEntry> nextTickUpdates = Lists.newArrayList();
    //private final Map<BlockVector3, Integer> updateQueueIndex = new HashMap<>();

    private boolean autoSave;

    private BlockMetadataStore blockMetadata;

    public int sleepTicks = 0;

    private final int chunkTickRadius;
    private final Long2IntMap chunkTickList = new Long2IntOpenHashMap();
    private final int chunksPerTicks;
    private final boolean clearChunksOnTick;

    private int updateLCG = ThreadLocalRandom.current().nextInt();

    private static final int LCG_CONSTANT = 1013904223;
    private final String id;

    private int tickRate;
    public int tickRateTime = 0;
    public int tickRateCounter = 0;

    private final Long2ObjectOpenHashMap<Set<Player>> chunkPlayers = new Long2ObjectOpenHashMap<>();
    private final Cache<Long, ByteBuf> chunkCache = CacheBuilder.newBuilder()
            .softValues()
            .removalListener(cacheRemover)
            .build();
    private final LevelChunkManager chunkManager;
    private final LevelData levelData;

    private Generator generator;

    Level(Server server, String id, LevelProvider levelProvider, LevelData levelData) {
        this.id = id;
        this.blockMetadata = new BlockMetadataStore(this);
        this.server = server;
        this.autoSave = server.getAutoSave();
        this.provider = levelProvider;
        this.levelData = levelData;
        this.timings = new LevelTimings(this);

//        try {
//            if (fullConvert) {
//                String newPath = new File(path).getParent() + "/" + name + ".old/";
//                new File(path).renameTo(new File(newPath));
//                this.chunkProvider = chunkProvider.getConstructor(Level.class, String.class).newInstance(this, newPath);
//            } else {
//                this.chunkProvider = chunkProvider.getConstructor(Level.class, String.class).newInstance(this, path);
//            }
//        } catch (Exception e) {
//            throw new LevelException("Caused by " + Utils.getExceptionMessage(e));
//        }
//
//        this.timings = new LevelTimings(this);
//
//        if (fullConvert) {
//            this.server.getLogger().info(this.server.getLanguage().translate("nukkit.level.updating",
//                    TextFormat.GREEN + this.chunkProvider.getName() + TextFormat.WHITE));
//            LevelChunkProvider old = this.chunkProvider;
//            try {
//                this.chunkProvider = new LevelProviderConverter(this, path)
//                        .from(old)
//                        .to(AnvilChunkProvider.class)
//                        .perform();
//                old.close();
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//        }

        log.info(this.server.getLanguage().translate("nukkit.level.preparing",
                TextFormat.GREEN + getId() + TextFormat.WHITE));

        this.generator = GeneratorRegistry.get().getGeneratorFactory(this.levelData.getGenerator()).create(this.getSeed(), this.levelData.getGeneratorOptions());

        if (this.levelData.getRainTime() <= 0) {
            setRainTime(ThreadLocalRandom.current().nextInt(168000) + 12000);
        }

        if (this.levelData.getLightningTime() <= 0) {
            setThunderTime(ThreadLocalRandom.current().nextInt(168000) + 12000);
        }

        this.updateQueue = new BlockUpdateScheduler(this, this.levelData.getCurrentTick());

        this.chunkTickRadius = Math.min(this.server.getViewDistance(),
                Math.max(1, this.server.getConfig("chunk-ticking.tick-radius", 4)));
        this.chunksPerTicks = this.server.getConfig("chunk-ticking.per-tick", 40);
        this.chunkTickList.clear();
        this.clearChunksOnTick = this.server.getConfig("chunk-ticking.clear-tick-list", true);
        this.tickRate = 1;
        this.chunkManager = new LevelChunkManager(this);

        this.skyLightSubtracted = this.calculateSkylightSubtracted(1);
    }

    public void reloadGenerator() {
        this.generator = GeneratorRegistry.get().getGeneratorFactory(this.levelData.getGenerator()).create(this.getSeed(), this.levelData.getGeneratorOptions());
    }

    public int getTickRate() {
        return tickRate;
    }

    public int getTickRateTime() {
        return tickRateTime;
    }

    public void setTickRate(int tickRate) {
        this.tickRate = tickRate;
    }

    public void init() {
    }

    public BlockMetadataStore getBlockMetadata() {
        return this.blockMetadata;
    }

    public Server getServer() {
        return server;
    }

    final public LevelProvider getProvider() {
        return this.provider;
    }

    public final String getId() {
        return this.id;
    }

    public void close() {
        if (this.getAutoSave()) {
            this.save(true, true);
        }

        try {
            this.provider.close();
        } catch (IOException e) {
            throw new LevelException("Error occurred whilst closing level", e);
        }
        this.provider = null;
        this.blockMetadata = null;
    }

    private Vector3f mutableBlock;

    public void addSound(Vector3i pos, Sound sound) {
        this.addSound(Vector3f.from(pos.getX() + 0.5f, pos.getY() + 0.5f, pos.getZ() + 0.5f), sound);
    }

    public void addSound(Vector3f pos, Sound sound) {
        this.addSound(pos, sound, 1, 1, (Player[]) null);
    }

    public void addSound(Vector3i pos, Sound sound, float volume, float pitch) {
        this.addSound(Vector3f.from(pos.getX() + 0.5f, pos.getY() + 0.5f, pos.getZ() + 0.5f), sound, volume, pitch);
    }

    public void addSound(Vector3f pos, Sound sound, float volume, float pitch) {
        this.addSound(pos, sound, volume, pitch, (Player[]) null);
    }

    public void addSound(Vector3f pos, Sound sound, float volume, float pitch, Collection<Player> players) {
        this.addSound(pos, sound, volume, pitch, players.toArray(new Player[0]));
    }

    public void addSound(Vector3f pos, Sound sound, float volume, float pitch, Player... players) {
        Preconditions.checkArgument(volume >= 0 && volume <= 1, "Sound volume must be between 0 and 1");
        Preconditions.checkArgument(pitch >= 0, "Sound pitch must be higher than 0");

        PlaySoundPacket packet = new PlaySoundPacket();
        packet.setSound(sound.getSound());
        packet.setVolume(volume);
        packet.setPitch(pitch);
        packet.setPosition(pos);

        if (players == null || players.length == 0) {
            addChunkPacket(pos, packet);
        } else {
            Server.broadcastPacket(players, packet);
        }
    }

    public void addLevelSoundEvent(Vector3f pos, SoundEvent event, int data, EntityType<?> type) {
        addLevelSoundEvent(pos, event, data, type, false, false);
    }

    public void addLevelSoundEvent(Vector3f pos, SoundEvent event, int data, EntityType<?> type, boolean isBaby, boolean isGlobal) {
        addLevelSoundEvent(pos, event, data, type.getIdentifier(), isBaby, isGlobal);
    }

    public void addLevelSoundEvent(Vector3i pos, SoundEvent event) {
        this.addLevelSoundEvent(pos.toFloat().add(0.5, 0.5, 0.5), event);
    }

    public void addLevelSoundEvent(Vector3f pos, SoundEvent event) {
        this.addLevelSoundEvent(pos, event, -1);
    }

    public void addLevelSoundEvent(Vector3i pos, SoundEvent event, int data) {
        this.addLevelSoundEvent(Vector3f.from(pos.getX() + 0.5f, pos.getY() + 0.5f, pos.getZ() + 0.5f), event, data,
                Identifier.EMPTY, false, false);
    }

    /**
     * Broadcasts sound to players
     *
     * @param pos   position where sound should be played
     * @param event ID of the sound from {@link SoundEvent}
     * @param data  generic data that can affect sound
     */
    public void addLevelSoundEvent(Vector3f pos, SoundEvent event, int data) {
        this.addLevelSoundEvent(pos, event, data, Identifier.EMPTY, false, false);
    }

    public void addParticle(Particle particle) {
        this.addParticle(particle, (Player[]) null);
    }

    public void addParticle(Particle particle, Player player) {
        this.addParticle(particle, new Player[]{player});
    }

    public void addParticle(Particle particle, Player[] players) {
        BedrockPacket[] packets = particle.encode();

        if (players == null) {
            if (packets != null) {
                for (BedrockPacket packet : packets) {
                    this.addChunkPacket(particle.getPosition(), packet);
                }
            }
        } else {
            if (packets != null) {
                Server.broadcastPackets(players, packets);
            }
        }
    }

    public void addParticle(Particle particle, Collection<Player> players) {
        this.addParticle(particle, players.toArray(new Player[0]));
    }

    public void addParticleEffect(Vector3f pos, Identifier identifier) {
        this.addParticleEffect(pos, identifier, -1, this.levelData.getDimension(), (Player[]) null);
    }

    public void addParticleEffect(Vector3f pos, Identifier identifier, long uniqueEntityId) {
        this.addParticleEffect(pos, identifier, uniqueEntityId, this.levelData.getDimension(), (Player[]) null);
    }

    public void addParticleEffect(Vector3f pos, Identifier identifier, long uniqueEntityId, int dimensionId) {
        this.addParticleEffect(pos, identifier, uniqueEntityId, dimensionId, (Player[]) null);
    }

    public void addParticleEffect(Vector3f pos, Identifier identifier, long uniqueEntityId, int dimensionId, Collection<Player> players) {
        this.addParticleEffect(pos, identifier, uniqueEntityId, dimensionId, players.toArray(new Player[0]));
    }

    public void addParticleEffect(Vector3f pos, Identifier identifier, long uniqueEntityId, int dimensionId, Player... players) {
        SpawnParticleEffectPacket packet = new SpawnParticleEffectPacket();
        packet.setIdentifier(identifier.toString());
        packet.setUniqueEntityId(uniqueEntityId);
        packet.setDimensionId(dimensionId);
        packet.setPosition(pos);

        if (players == null || players.length == 0) {
            addChunkPacket(pos.getFloorX() >> 4, pos.getFloorZ() >> 4, packet);
        } else {
            Server.broadcastPacket(players, packet);
        }
    }

    public boolean getAutoSave() {
        return this.autoSave;
    }

    public void setAutoSave(boolean autoSave) {
        this.autoSave = autoSave;
    }

    public boolean unload() {
        return this.unload(false);
    }

    public boolean unload(boolean force) {
        LevelUnloadEvent ev = new LevelUnloadEvent(this);

        if (this == this.server.getDefaultLevel() && !force) {
            ev.setCancelled();
        }

        this.server.getPluginManager().callEvent(ev);

        if (!force && ev.isCancelled()) {
            return false;
        }

        log.info(this.server.getLanguage().translate("nukkit.level.unloading",
                TextFormat.GREEN + this.getName() + TextFormat.WHITE));
        Level defaultLevel = this.server.getDefaultLevel();

        for (Player player : new ArrayList<>(this.getPlayers().values())) {
            if (this == defaultLevel || defaultLevel == null) {
                player.close(player.getLeaveMessage(), "Forced default level unload");
            } else {
                player.teleport(this.server.getDefaultLevel().getSafeSpawn());
            }
        }

        if (this == defaultLevel) {
            this.server.setDefaultLevel(null);
        }

        this.close();

        return true;
    }

    public Set<Player> getChunkPlayers(int chunkX, int chunkZ) {
        Chunk chunk = this.getLoadedChunk(chunkX, chunkZ);
        return chunk == null ? ImmutableSet.of() : chunk.getPlayerLoaders();
    }

    public Set<ChunkLoader> getChunkLoaders(int chunkX, int chunkZ) {
        Chunk chunk = this.getLoadedChunk(chunkX, chunkZ);
        return chunk == null ? ImmutableSet.of() : chunk.getLoaders();
    }

    public void addLevelSoundEvent(Vector3f pos, SoundEvent event, int data, Identifier identifier, boolean isBaby, boolean isGlobal) {
        LevelSoundEventPacket packet = new LevelSoundEventPacket();
        packet.setSound(event);
        packet.setExtraData(data);
        packet.setIdentifier(identifier.toString());
        packet.setPosition(pos);
        packet.setRelativeVolumeDisabled(isGlobal);
        packet.setBabySound(isBaby);

        this.addChunkPacket(pos, packet);
    }

    public void checkTime() {
        this.levelData.checkTime(this.tickRate);
    }

    private boolean doDaylightCycle() {
        return this.getGameRules().get(GameRules.DO_DAYLIGHT_CYCLE);
    }

    private boolean doWeatherCycle() {
        return this.getGameRules().get(GameRules.DO_WEATHER_CYCLE);
    }

    public void sendTime(Player... players) {
        /*if (this.stopTime) { //TODO
            SetTimePacket pk0 = new SetTimePacket();
            pk0.time = (int) this.time;
            player.dataPacket(pk0);
        }*/

        SetTimePacket pk = new SetTimePacket();
        pk.setTime(this.getTime());

        Server.broadcastPacket(players, pk);
    }

    public void sendTime() {
        sendTime(this.players.values().toArray(new Player[0]));
    }

    public GameRuleMap getGameRules() {
        return this.levelData.getGameRules();
    }

    public void addChunkPacket(Vector3i pos, BedrockPacket packet) {
        addChunkPacket(pos.getX() >> 4, pos.getZ() >> 4, packet);
    }

    public void addChunkPacket(Vector3f pos, BedrockPacket packet) {
        addChunkPacket(pos.getFloorX() >> 4, pos.getFloorZ() >> 4, packet);
    }

    public void addChunkPacket(int chunkX, int chunkZ, BedrockPacket packet) {
        long index = Chunk.key(chunkX, chunkZ);
        synchronized (chunkPackets) {
            Deque<BedrockPacket> packets = chunkPackets.computeIfAbsent(index, i -> new ArrayDeque<>());
            packets.add(packet);
        }
    }

    public void doTick(int currentTick) {
        try (Timing ignored = this.timings.doTick.startTiming()) {
            synchronized (lightQueue) {
                updateBlockLight(lightQueue);
            }
            this.checkTime();

            if (currentTick % 600 == 0 && doDaylightCycle()) {
                this.sendTime();
            }

            // Tick Weather
            if (this.doWeatherCycle()) {
                this.levelData.setRainTime(getRainTime() - 1);
                if (this.levelData.getRainTime() <= 0) {
                    if (!this.setRaining(this.levelData.getRainLevel() <= 0)) {
                        if (this.levelData.getRainLevel() > 0) {
                            setRainTime(ThreadLocalRandom.current().nextInt(12000) + 12000);
                        } else {
                            setRainTime(ThreadLocalRandom.current().nextInt(168000) + 12000);
                        }
                    }
                }

                this.levelData.setLightningTime(this.levelData.getLightningTime() - 1);
                if (this.levelData.getLightningTime() <= 0) {
                    if (!this.setThundering(this.levelData.getLightningLevel() <= 0)) {
                        if (this.levelData.getLightningLevel() > 0) {
                            setThunderTime(ThreadLocalRandom.current().nextInt(12000) + 3600);
                        } else {
                            setThunderTime(ThreadLocalRandom.current().nextInt(168000) + 12000);
                        }
                    }
                }

                if (this.isThundering()) {
                    for (Chunk chunk : this.getChunks()) {
                        this.performThunder(chunk);
                    }
                }
            }

            this.skyLightSubtracted = this.calculateSkylightSubtracted(1);

            this.levelData.tick();

            int polled = 0;

            try (Timing ignored2 = timings.doTickPending.startTiming()) {
                this.updateQueue.tick(this.getCurrentTick());
            }

            Block block;
            while ((block = this.normalUpdateQueue.poll()) != null) {
                block.getState().getBehavior().onUpdate(block, BLOCK_UPDATE_NORMAL);
            }

            TimingsHistory.entityTicks += this.updateEntities.size();

            try (Timing ignored2 = this.timings.entityTick.startTiming()) {
                if (!this.updateEntities.isEmpty()) {
                    this.updateEntities.removeIf(entity -> entity.isClosed() || !entity.onUpdate(currentTick));
                }
            }

            TimingsHistory.tileEntityTicks += this.updateBlockEntities.size();
            try (Timing ignored2 = this.timings.blockEntityTick.startTiming()) {
                this.updateBlockEntities.removeIf(blockEntity -> blockEntity.isClosed() || !blockEntity.onUpdate());
            }

            try (Timing ignored2 = this.timings.tickChunks.startTiming()) {
                try (Timing ignored3 = this.timings.tickChunks.startTiming()) {
                    this.tickChunks();
                }

                synchronized (changedBlocks) {
                    ConcurrentMap<Long, IntSet> changedBlocks = this.changedBlocks.asMap();
                    if (!changedBlocks.isEmpty()) {
                        if (!this.players.isEmpty()) {
                            Iterator<Map.Entry<Long, IntSet>> iter = changedBlocks.entrySet().iterator();
                            while (iter.hasNext()) {
                                Map.Entry<Long, IntSet> entry = iter.next();
                                long chunkKey = entry.getKey();
                                IntSet blocks = entry.getValue();
                                int chunkX = Chunk.fromKeyX(chunkKey);
                                int chunkZ = Chunk.fromKeyZ(chunkKey);
                                if (blocks.size() > MAX_BLOCK_CACHE) {
                                    Chunk chunk = this.getLoadedChunk(chunkX, chunkZ);
                                    if (chunk != null) {
                                        for (Player p : this.getChunkPlayers(chunkX, chunkZ)) {
                                            p.onChunkChanged(chunk);
                                        }
                                    }
                                } else {
                                    Collection<Player> toSend = this.getChunkPlayers(chunkX, chunkZ);
                                    Player[] playerArray = toSend.toArray(new Player[0]);
                                    Block[] blocksArray = new Block[blocks.size()];
                                    int i = 0;
                                    for (int blockKey : blocks) {
                                        blocksArray[i++] = this.getBlock(Chunk.fromKey(chunkKey, blockKey).toVector3()); //TODO: send layers separately
                                    }
                                    this.sendBlocks(playerArray, blocksArray, UpdateBlockPacket.FLAG_ALL);
                                }
                                iter.remove();
                            }
                        }
                    }
                }

                //this.processChunkRequest();

                if (this.sleepTicks > 0 && --this.sleepTicks <= 0) {
                    this.checkSleep();
                }

                synchronized (chunkPackets) {
                    for (long index : this.chunkPackets.keySet()) {
                        int chunkX = Chunk.fromKeyX(index);
                        int chunkZ = Chunk.fromKeyZ(index);
                        Chunk chunk = this.getLoadedChunk(chunkX, chunkZ);

                        Set<Player> playerLoaders;
                        if (chunk == null || (playerLoaders = chunk.getPlayerLoaders()).isEmpty()) {
                            // Chunk is unloaded.
                            continue;
                        }

                        for (BedrockPacket packet : this.chunkPackets.get(index)) {
                            Server.broadcastPacket(playerLoaders, packet);
                        }
                    }
                    this.chunkPackets.clear();
                }

                if (this.levelData.getGameRules().isDirty()) {
                    GameRulesChangedPacket packet = new GameRulesChangedPacket();
                    this.levelData.getGameRules().toNetwork(packet.getGameRules());
                    Server.broadcastPacket(players.values().toArray(new Player[0]), packet);
                    this.levelData.getGameRules().refresh();
                }
            }
        }
    }

    private void performThunder(Chunk chunk) {
        if (areNeighboringChunksLoaded(Chunk.key(chunk.getX(), chunk.getZ()))) return;
        if (ThreadLocalRandom.current().nextInt(10000) == 0) {
            int LCG = this.getUpdateLCG() >> 2;

            int chunkX = chunk.getX() * 16;
            int chunkZ = chunk.getZ() * 16;
            Vector3f vector = this.adjustPosToNearbyEntity(Vector3f.from(chunkX + (LCG & 0xf), 0, chunkZ + (LCG >> 8 & 0xf)));

            Identifier blockType = chunk.getBlock(vector.getFloorX() & 0xf, vector.getFloorY(), vector.getFloorZ() & 0xf).getType();
            if (blockType != BlockTypes.TALL_GRASS && blockType != BlockTypes.FLOWING_WATER)
                vector = vector.add(0, 1, 0);

            Location location = Location.from(vector, this);
            LightningBolt bolt = EntityRegistry.get().newEntity(EntityTypes.LIGHTNING_BOLT, location);
            bolt.setPosition(vector);
            LightningStrikeEvent ev = new LightningStrikeEvent(this, bolt);
            getServer().getPluginManager().callEvent(ev);
            if (!ev.isCancelled()) {
                bolt.spawnToAll();
            } else {
                bolt.setEffect(false);
            }
        }
    }

    public void checkSleep() {
        if (this.players.isEmpty()) {
            return;
        }

        boolean resetTime = true;
        for (Player p : this.getPlayers().values()) {
            if (!p.isSleeping()) {
                resetTime = false;
                break;
            }
        }

        if (resetTime) {
            int time = this.getTime() % Level.TIME_FULL;

            if (time >= Level.TIME_NIGHT && time < Level.TIME_SUNRISE) {
                this.setTime(this.getTime() + Level.TIME_FULL - time);

                for (Player p : this.getPlayers().values()) {
                    p.stopSleep();
                }
            }
        }
    }

    public Vector3f adjustPosToNearbyEntity(Vector3f pos) {
        pos = Vector3f.from(pos.getX(), this.getHighestBlockAt(pos.getFloorX(), pos.getFloorZ()), pos.getZ());
        AxisAlignedBB axisalignedbb = new SimpleAxisAlignedBB(pos, Vector3f.from(pos.getX(), 255, pos.getZ())).expand(3, 3, 3);
        List<Entity> list = new ArrayList<>();

        for (Entity entity : this.getCollidingEntities(axisalignedbb)) {
            if (entity.isAlive() && canBlockSeeSky(entity.getPosition())) {
                list.add(entity);
            }
        }

        if (!list.isEmpty()) {
            return list.get(ThreadLocalRandom.current().nextInt(list.size())).getPosition();
        } else {
            if (pos.getY() == -1) {
                pos = pos.add(0, 2, 0);
            }

            return pos;
        }
    }

    public void sendBlocks(Player[] target, Block[] blocks) {
        this.sendBlocks(target, blocks, Collections.emptySet());
    }

    public void sendBlocks(Player[] target, Block[] blocks, Set<UpdateBlockPacket.Flag> flags) {
        this.sendBlocks(target, blocks, flags, false);
    }

    public void sendBlocks(Player[] target, Block[] blocks, Set<UpdateBlockPacket.Flag> flags, boolean optimizeRebuilds) {
        for (Block block : blocks) {
            if (block == null) throw new NullPointerException("Null block is update array");
        }
        UpdateBlockPacket[] packets = new UpdateBlockPacket[blocks.length * 2];
        LongSet chunks = null;
        if (optimizeRebuilds) {
            chunks = new LongOpenHashSet();
        }
        for (int i = 0; i < packets.length; i += 2) {
            boolean first = !optimizeRebuilds;

            Block block = blocks[i];
            int chunkX = block.getChunk().getX();
            int chunkZ = block.getChunk().getZ();

            if (optimizeRebuilds) {
                long index = Chunk.key(chunkX, chunkZ);
                if (!chunks.contains(index)) {
                    chunks.add(index);
                    first = true;
                }
            }

            UpdateBlockPacket updateBlockPacket = new UpdateBlockPacket();
            updateBlockPacket.setBlockPosition(block.getPosition());
            updateBlockPacket.setDataLayer(0);
            updateBlockPacket.getFlags().addAll(flags);

            UpdateBlockPacket updateBlockPacket2 = new UpdateBlockPacket();
            updateBlockPacket2.setBlockPosition(block.getPosition());
            updateBlockPacket2.setDataLayer(1);
            updateBlockPacket2.getFlags().addAll(flags);

            try {
                updateBlockPacket.setRuntimeId(BlockPalette.INSTANCE.getRuntimeId(block.getState())); //TODO: send layers separately
                updateBlockPacket2.setRuntimeId(BlockPalette.INSTANCE.getRuntimeId(block.getExtra()));
            } catch (RegistryException e) {
                throw new IllegalStateException("Unable to create BlockUpdatePacket at " +
                        block.getPosition() + " in " + getName(), e);
            }
            packets[i] = updateBlockPacket;
            packets[i + 1] = updateBlockPacket2;
        }
        Server.broadcastPackets(target, packets);
    }

    public boolean save() {
        return this.save(false);
    }

    public boolean save(boolean force) {
        return this.save(force, false);
    }

    private boolean save(boolean force, boolean sync) {
        if (!this.getAutoSave() && !force) {
            return false;
        }

        this.server.getPluginManager().callEvent(new LevelSaveEvent(this));

        CompletableFuture<Void> chunksFuture = this.saveChunks();
        CompletableFuture<Void> dataFuture = this.provider.saveLevelData(this.levelData);

        if (sync) {
            chunksFuture.join();
            dataFuture.join();
        }

        return true;
    }

    public CompletableFuture<Void> saveChunks() {
        return this.chunkManager.saveChunks();
    }

    private void tickChunks() {
        if (this.chunksPerTicks <= 0 || this.players.isEmpty()) {
            this.chunkTickList.clear();
            return;
        }

        int chunksPerLoader = Math.min(200, Math.max(1, (int) (((double) (this.chunksPerTicks - this.players.size()) / this.players.size() + 0.5))));
        int randRange = 3 + chunksPerLoader / 30;
        randRange = Math.min(randRange, this.chunkTickRadius);

        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (!this.loaders.isEmpty()) {
            for (ChunkLoader loader : this.loaders.values()) {
                int chunkX = (int) loader.getX() >> 4;
                int chunkZ = (int) loader.getZ() >> 4;

                long index = Chunk.key(chunkX, chunkZ);
                int existingLoaders = Math.max(0, this.chunkTickList.getOrDefault(index, 0));
                this.chunkTickList.put(index, existingLoaders + 1);
                for (int chunk = 0; chunk < chunksPerLoader; ++chunk) {
                    int dx = random.nextInt(2 * randRange) - randRange;
                    int dz = random.nextInt(2 * randRange) - randRange;
                    long hash = Chunk.key(dx + chunkX, dz + chunkZ);
                    if (!this.chunkTickList.containsKey(hash) && this.chunkManager.isChunkLoaded(hash)) {
                        this.chunkTickList.put(hash, -1);
                    }
                }
            }
        }

        int blockTest = 0;

        if (!chunkTickList.isEmpty()) {
            ObjectIterator<Long2IntMap.Entry> iter = chunkTickList.long2IntEntrySet().iterator();
            while (iter.hasNext()) {
                Long2IntMap.Entry entry = iter.next();
                long index = entry.getLongKey();
                if (!areNeighboringChunksLoaded(index)) {
                    iter.remove();
                    continue;
                }

                int loaders = entry.getIntValue();

                int chunkX = Chunk.fromKeyX(index);
                int chunkZ = Chunk.fromKeyZ(index);

                Chunk chunk;
                if ((chunk = this.getLoadedChunk(chunkX, chunkZ)) == null) {
                    iter.remove();
                    continue;
                } else if (loaders <= 0) {
                    iter.remove();
                }

                chunk.getEntities().forEach(this::scheduleEntityUpdate);

                int tickSpeed = getGameRules().get(GameRules.RANDOM_TICK_SPEED);

                if (tickSpeed > 0) {
                    ChunkSection[] sections = chunk.getSections();
                    for (int sectionY = 0; sectionY < sections.length; sectionY++) {
                        ChunkSection section = sections[sectionY];
                        if (section != null) {
                            for (int i = 0; i < tickSpeed; ++i) {
                                int lcg = this.getUpdateLCG();
                                int x = lcg & 0x0f;
                                int y = lcg >>> 8 & 0x0f;
                                int z = lcg >>> 16 & 0x0f;

                                val state = section.getBlock(x, y, z, 0);
                                if (randomTickBlocks.contains(state.getType())) {
                                    Block block = new CloudBlock(this, Vector3i.from(x, y, z), new BlockState[]{
                                            state,
                                            section.getBlock(x, y, z, 1)
                                    });

                                    state.getBehavior().onUpdate(block, BLOCK_UPDATE_RANDOM);
                                }
                            }
                        }
                    }
                }
            }
        }

        if (this.clearChunksOnTick) {
            this.chunkTickList.clear();
        }
    }

    public void updateAroundRedstone(Vector3i pos, Direction face) {
        for (Direction side : Direction.values()) {
            if (face != null && side == face) {
                continue;
            }

            Block block = this.getBlock(side.getOffset(pos));
            block.getState().getBehavior().onUpdate(block, BLOCK_UPDATE_REDSTONE);
        }
    }

    public void updateComparatorOutputLevel(Vector3i v) {
        for (Direction face : Direction.Plane.HORIZONTAL) {
            Vector3i pos = face.getOffset(v);

            if (this.isChunkLoaded(pos)) {
                Block block = this.getBlock(pos);

                BlockBehavior behavior = block.getState().getBehavior();
                if (BlockBehaviorRedstoneDiode.isDiode(behavior)) {
                    behavior.onUpdate(block, BLOCK_UPDATE_REDSTONE);
                } else if (behavior.isNormalBlock(block)) {
                    pos = face.getOffset(pos);
                    block = this.getBlock(pos);

                    if (BlockBehaviorRedstoneDiode.isDiode(behavior)) {
                        behavior.onUpdate(block, BLOCK_UPDATE_REDSTONE);
                    }
                }
            }
        }
    }

    public void updateAround(int posX, int posY, int posZ) {
        BlockUpdateEvent ev;
        Block block;

        for (int x = posX - 1; x <= posX + 1; x++) {
            for (int y = posY - 1; y <= posY + 1; y++) {
                for (int z = posZ - 1; z <= posZ + 1; z++) {
                    if (x == posX && y == posY && z == posZ) continue;
                    block = this.getBlock(x, y, z);
                    if (block.getState().getType() != BlockTypes.AIR) {
                        this.getServer().getPluginManager().callEvent(
                                ev = new BlockUpdateEvent(block));
                        if (!ev.isCancelled()) {
                            normalUpdateQueue.add(block);
                        }
                    }
                }
            }
        }
    }

    public void scheduleUpdate(Vector3i pos, int delay) {
        scheduleUpdate(getBlock(pos), delay);
    }

    public void scheduleUpdate(Block block, int delay) {
        this.scheduleUpdate(block, block.getPosition(), delay, 0, true);
    }

    public void updateAround(Vector3i pos) {
        updateAround(pos.getX(), pos.getY(), pos.getZ());
    }

    public void scheduleUpdate(Block block, Vector3i pos, int delay) {
        this.scheduleUpdate(block, pos, delay, 0, true);
    }

    public void scheduleUpdate(BlockUpdate blockUpdate) {
        this.scheduleUpdate(blockUpdate.getBlock(), blockUpdate.getPos(), blockUpdate.getDelay(),
                blockUpdate.getPriority(), blockUpdate.shouldCheckArea());
    }

    public void scheduleUpdate(Block block, Vector3i pos, int delay, int priority) {
        this.scheduleUpdate(block, pos, delay, priority, true);
    }

    public void scheduleUpdate(Block block, Vector3i pos, int delay, int priority, boolean checkArea) {
        if (block.getState().getType() == BlockTypes.AIR || (checkArea && !this.isChunkLoaded(pos))) {
            return;
        }

        BlockUpdateEntry entry = new BlockUpdateEntry(pos, block, ((long) delay) + getCurrentTick(), priority);

        if (!this.updateQueue.contains(entry)) {
            this.updateQueue.add(entry);
        }
    }

    public boolean cancelSheduledUpdate(Vector3i pos, Block block) {
        return this.updateQueue.remove(new BlockUpdateEntry(pos, block));
    }

    public boolean isUpdateScheduled(Vector3i pos, Block block) {
        return this.updateQueue.contains(new BlockUpdateEntry(pos, block));
    }

    public Set<BlockUpdateEntry> getPendingBlockUpdates(Chunk chunk) {
        int minX = (chunk.getX() << 4) - 2;
        int maxX = minX + 16 + 2;
        int minZ = (chunk.getZ() << 4) - 2;
        int maxZ = minZ + 16 + 2;

        return this.getPendingBlockUpdates(new SimpleAxisAlignedBB(minX, 0, minZ, maxX, 256, maxZ));
    }

    public Set<BlockUpdateEntry> getPendingBlockUpdates(AxisAlignedBB boundingBox) {
        return updateQueue.getPendingBlockUpdates(boundingBox);
    }

    public Block[] getCollisionBlocks(AxisAlignedBB bb) {
        return this.getCollisionBlocks(bb, false);
    }

    public Block[] getCollisionBlocks(AxisAlignedBB bb, boolean targetFirst) {
        int minX = NukkitMath.floorDouble(bb.getMinX());
        int minY = NukkitMath.floorDouble(bb.getMinY());
        int minZ = NukkitMath.floorDouble(bb.getMinZ());
        int maxX = NukkitMath.ceilDouble(bb.getMaxX());
        int maxY = NukkitMath.ceilDouble(bb.getMaxY());
        int maxZ = NukkitMath.ceilDouble(bb.getMaxZ());

        List<Block> collides = new ArrayList<>();

        if (targetFirst) {
            for (int z = minZ; z <= maxZ; ++z) {
                for (int x = minX; x <= maxX; ++x) {
                    for (int y = minY; y <= maxY; ++y) {
                        Block block = this.getLoadedBlock(x, y, z);
                        if (block != null && block.getState() != BlockStates.AIR && block.getState().getBehavior().collidesWithBB(block, bb)) {
                            return new Block[]{block};
                        }
                    }
                }
            }
        } else {
            for (int z = minZ; z <= maxZ; ++z) {
                for (int x = minX; x <= maxX; ++x) {
                    for (int y = minY; y <= maxY; ++y) {
                        Block block = this.getLoadedBlock(x, y, z);
                        if (block != null && block.getState() != BlockStates.AIR && block.getState().getBehavior().collidesWithBB(block, bb)) {
                            collides.add(block);
                        }
                    }
                }
            }
        }

        return collides.toArray(new Block[0]);
    }

    public boolean isBlockTickPending(Vector3i pos, Block block) {
        return this.updateQueue.isBlockTickPending(pos, block);
    }

    public AxisAlignedBB[] getCollisionCubes(BaseEntity entity, AxisAlignedBB bb) {
        return this.getCollisionCubes(entity, bb, true);
    }

    public AxisAlignedBB[] getCollisionCubes(BaseEntity entity, AxisAlignedBB bb, boolean entities) {
        return getCollisionCubes(entity, bb, entities, false);
    }

    public AxisAlignedBB[] getCollisionCubes(BaseEntity entity, AxisAlignedBB bb, boolean entities, boolean solidEntities) {
        int minX = NukkitMath.floorDouble(bb.getMinX());
        int minY = NukkitMath.floorDouble(bb.getMinY());
        int minZ = NukkitMath.floorDouble(bb.getMinZ());
        int maxX = NukkitMath.ceilDouble(bb.getMaxX());
        int maxY = NukkitMath.ceilDouble(bb.getMaxY());
        int maxZ = NukkitMath.ceilDouble(bb.getMaxZ());

        List<AxisAlignedBB> collides = new ArrayList<>();

        for (int z = minZ; z <= maxZ; ++z) {
            for (int x = minX; x <= maxX; ++x) {
                for (int y = minY; y <= maxY; ++y) {
                    Block block = this.getBlock(x, y, z); //TODO: check loaded block
                    BlockBehavior behavior = block.getState().getBehavior();
                    if (!behavior.canPassThrough() && behavior.collidesWithBB(block, bb)) {
                        collides.add(behavior.getBoundingBox());
                    }
                }
            }
        }

        if (entities || solidEntities) {
            for (Entity ent : this.getCollidingEntities(bb.grow(0.25f, 0.25f, 0.25f), entity)) {
                if (solidEntities && !ent.canPassThrough()) {
                    collides.add(ent.getBoundingBox().clone());
                }
            }
        }

        return collides.toArray(new AxisAlignedBB[0]);
    }

    public boolean isFullBlock(BlockState state) {
        BlockBehavior behavior = state.getBehavior();
        if (behavior.isSolid()) {
            return true;
        }
        AxisAlignedBB bb = behavior.getBoundingBox();

        return bb != null && bb.getAverageEdgeLength() >= 1;
    }

    public boolean hasCollision(BaseEntity entity, AxisAlignedBB bb, boolean entities) {
        int minX = NukkitMath.floorDouble(bb.getMinX());
        int minY = NukkitMath.floorDouble(bb.getMinY());
        int minZ = NukkitMath.floorDouble(bb.getMinZ());
        int maxX = NukkitMath.ceilDouble(bb.getMaxX());
        int maxY = NukkitMath.ceilDouble(bb.getMaxY());
        int maxZ = NukkitMath.ceilDouble(bb.getMaxZ());

        for (int z = minZ; z <= maxZ; ++z) {
            for (int x = minX; x <= maxX; ++x) {
                for (int y = minY; y <= maxY; ++y) {
                    Block block = this.getLoadedBlock(Vector3i.from(x, y, z));
                    if (block == null) return true; // Shouldn't walk into unloaded chunks.
                    BlockBehavior behavior = block.getState().getBehavior();
                    if (!behavior.canPassThrough() && behavior.collidesWithBB(block, bb)) {
                        return true;
                    }
                }
            }
        }

        if (entities) {
            return !this.getCollidingEntities(bb.grow(0.25f, 0.25f, 0.25f), entity).isEmpty();
        }
        return false;
    }

    public int calculateSkylightSubtracted(float tickDiff) {
        float angle = this.calculateCelestialAngle(getTime(), tickDiff);
        float light = 1 - (MathHelper.cos(angle * ((float) Math.PI * 2F)) * 2 + 0.5f);
        light = light < 0 ? 0 : light > 1 ? 1 : light;
        light = 1 - light;
        light = (float) ((double) light * ((isRaining() ? 1 : 0) - (double) 5f / 16d));
        light = (float) ((double) light * ((isThundering() ? 1 : 0) - (double) 5f / 16d));
        light = 1 - light;
        return (int) (light * 11f);
    }

    public float calculateCelestialAngle(int time, float tickDiff) {
        float angle = ((float) time + tickDiff) / 24000f - 0.25f;

        if (angle < 0) {
            ++angle;
        }

        if (angle > 1) {
            --angle;
        }

        float i = 1 - (float) ((Math.cos((double) angle * Math.PI) + 1) / 2d);
        angle = angle + (i - angle) / 3;
        return angle;
    }

    public int getMoonPhase(long worldTime) {
        return (int) (worldTime / 24000 % 8 + 8) % 8;
    }

    public int getFullLight(Vector3i pos) {
        Chunk chunk = this.getChunk(pos);

        int level = chunk.getSkyLight(pos.getX() & 0x0f, pos.getY() & 0xff, pos.getZ() & 0x0f);
        level -= this.skyLightSubtracted;

        if (level < 15) {
            level = Math.max(chunk.getBlockLight(pos.getX() & 0x0f, pos.getY() & 0xff, pos.getZ() & 0x0f),
                    level);
        }

        return level;
    }

    @Nullable
    public Block getLoadedBlock(int x, int y, int z) {
        int chunkX = x >> 4;
        int chunkZ = z >> 4;

        Chunk chunk = this.getLoadedChunk(chunkX, chunkZ);

        if (y < 0 || y > 255) {
            return new CloudBlock(this, Vector3i.from(x, y, z), CloudBlock.EMPTY);
        }

        if (chunk == null) {
            return null;
        }

        return new CloudBlock(this, Vector3i.from(x, y, z),
                new BlockState[]{
                        chunk.getBlock(x & 0xf, y, z & 0xf, 0),
                        chunk.getBlock(x & 0xf, y, z & 0xf, 1)
                }
        );
    }

    @Nullable
    public Block getLoadedBlock(Vector3f pos) {
        return this.getLoadedBlock(pos.getFloorX(), pos.getFloorY(), pos.getFloorZ());
    }

    @Nullable
    public Block getLoadedBlock(Vector3i pos) {
        return this.getLoadedBlock(pos.getX(), pos.getY(), pos.getZ());
    }

    @Nonnull
    public Block getBlock(Vector3f vector) {
        return this.getBlock(vector.getFloorX(), vector.getFloorY(), vector.getFloorZ());
    }

    @Nonnull
    public Block getBlock(Vector3i vector) {
        return this.getBlock(vector.getX(), vector.getY(), vector.getZ());
    }

    @Nonnull
    public Block getBlock(int x, int y, int z) {
        int chunkX = x >> 4;
        int chunkZ = z >> 4;

        Chunk chunk = this.getChunk(chunkX, chunkZ);

        if (y < 0 || y > 255) {
            return new CloudBlock(this, Vector3i.from(x, y, z), BlockStates.EMPTY_STATES);
        }

        return new CloudBlock(this, Vector3i.from(x, y, z), new BlockState[]{
                chunk.getBlock(x & 0xf, y, z & 0xf, 0),
                chunk.getBlock(x & 0xf, y, z & 0xf, 1)
        });
    }

    public void updateBlockSkyLight(int x, int y, int z) {
        // todo
    }

    public void updateAllLight(Vector3f pos) {
        this.updateBlockSkyLight(pos.getFloorX(), pos.getFloorY(), pos.getFloorZ());
        this.addLightUpdate(pos.getFloorX(), pos.getFloorY(), pos.getFloorZ());
    }

    public void updateBlockLight(Long2ObjectMap<ShortSet> map) {
        if (map.isEmpty()) {
            return;
        }
        LongPriorityQueue lightPropagationQueue = new LongArrayFIFOQueue();
        Long2ByteMap lightRemovalQueue = new Long2ByteOpenHashMap();
        LongSet visited = new LongOpenHashSet();
        LongSet removalVisited = new LongOpenHashSet();

        for (Long2ObjectMap.Entry<ShortSet> entry : map.long2ObjectEntrySet()) {
            long chunkKey = entry.getLongKey();
            ShortSet blocks = entry.getValue();
            int chunkX = Chunk.fromKeyX(chunkKey);
            int chunkZ = Chunk.fromKeyZ(chunkKey);
            for (short blockKey : blocks) {
                Vector3i position = Chunk.fromKey(chunkKey, blockKey);
                Chunk chunk = this.getLoadedChunk(chunkX, chunkZ);
                if (chunk != null) {
                    int lcx = position.getX() & 0xF;
                    int lcz = position.getZ() & 0xF;
                    int oldLevel = chunk.getBlockLight(lcx, position.getY(), lcz);
                    Block block = new CloudBlock(this, Vector3i.from(lcx, position.getY(), lcz), new BlockState[]{
                            chunk.getBlock(lcx, position.getY(), lcz, 0),
                            chunk.getBlock(lcx, position.getY(), lcz, 1)
                    });
                    int newLevel = block.getState().getBehavior().getLightLevel(block);
                    if (oldLevel != newLevel) {
                        this.setBlockLightAt(position.getX(), position.getY(), position.getZ(), newLevel);
                        if (newLevel < oldLevel) {
                            removalVisited.add(BlockUtils.key(position));
                            lightRemovalQueue.put(BlockUtils.key(position), (byte) oldLevel);
                        } else {
                            visited.add(BlockUtils.key(position));
                            lightPropagationQueue.enqueue(BlockUtils.key(position));
                        }
                    }
                }
            }
        }
        map.clear();

        for (Long2ByteMap.Entry entry : lightRemovalQueue.long2ByteEntrySet()) {
            long node = entry.getLongKey();
            int x = Hash.hashBlockX(node);
            int y = Hash.hashBlockY(node);
            int z = Hash.hashBlockZ(node);

            int lightLevel = entry.getByteValue();

            this.computeRemoveBlockLight(x - 1, y, z, lightLevel, lightRemovalQueue, lightPropagationQueue,
                    removalVisited, visited);
            this.computeRemoveBlockLight(x + 1, y, z, lightLevel, lightRemovalQueue, lightPropagationQueue,
                    removalVisited, visited);
            this.computeRemoveBlockLight(x, y - 1, z, lightLevel, lightRemovalQueue, lightPropagationQueue,
                    removalVisited, visited);
            this.computeRemoveBlockLight(x, y + 1, z, lightLevel, lightRemovalQueue, lightPropagationQueue,
                    removalVisited, visited);
            this.computeRemoveBlockLight(x, y, z - 1, lightLevel, lightRemovalQueue, lightPropagationQueue,
                    removalVisited, visited);
            this.computeRemoveBlockLight(x, y, z + 1, lightLevel, lightRemovalQueue, lightPropagationQueue,
                    removalVisited, visited);
        }

        while (!lightPropagationQueue.isEmpty()) {
            long node = lightPropagationQueue.dequeueLong();

            int x = Hash.hashBlockX(node);
            int y = Hash.hashBlockY(node);
            int z = Hash.hashBlockZ(node);

            Block block = this.getBlock(x, y, z);

            int lightLevel = this.getBlockLightAt(x, y, z) - block.getState().getBehavior().getFilterLevel();

            if (lightLevel >= 1) {
                this.computeSpreadBlockLight(x - 1, y, z, lightLevel, lightPropagationQueue, visited);
                this.computeSpreadBlockLight(x + 1, y, z, lightLevel, lightPropagationQueue, visited);
                this.computeSpreadBlockLight(x, y - 1, z, lightLevel, lightPropagationQueue, visited);
                this.computeSpreadBlockLight(x, y + 1, z, lightLevel, lightPropagationQueue, visited);
                this.computeSpreadBlockLight(x, y, z - 1, lightLevel, lightPropagationQueue, visited);
                this.computeSpreadBlockLight(x, y, z + 1, lightLevel, lightPropagationQueue, visited);
            }
        }
    }

    private void computeRemoveBlockLight(int x, int y, int z, int currentLight, Long2ByteMap queue,
                                         LongPriorityQueue spreadQueue, LongSet visited, LongSet spreadVisited) {
        int current = this.getBlockLightAt(x, y, z);
        long index = Hash.hashBlock(x, y, z);
        if (current != 0 && current < currentLight) {
            this.setBlockLightAt(x, y, z, 0);
            if (current > 1) {
                if (visited.add(index)) {
                    queue.put(Hash.hashBlock(x, y, z), (byte) current);
                }
            }
        } else if (current >= currentLight) {
            if (spreadVisited.add(index)) {
                spreadQueue.enqueue(Hash.hashBlock(x, y, z));
            }
        }
    }

    private void computeSpreadBlockLight(int x, int y, int z, int currentLight, LongPriorityQueue queue, LongSet visited) {
        int current = this.getBlockLightAt(x, y, z);
        long index = Hash.hashBlock(x, y, z);

        if (current < currentLight - 1) {
            this.setBlockLightAt(x, y, z, currentLight);

            if (visited.add(index)) {
                if (currentLight > 1) {
                    queue.enqueue(Hash.hashBlock(x, y, z));
                }
            }
        }
    }

    @Synchronized("lightQueue")
    public void addLightUpdate(int x, int y, int z) {
        long index = Chunk.key(x >> 4, z >> 4);
        this.lightQueue.computeIfAbsent(index, aLong -> new ShortOpenHashSet())
                .add(Chunk.blockKey(x, y, z));
    }

    public boolean setBlock(Vector3i pos, BlockState blockState) {
        return this.setBlock(pos, blockState, false);
    }

    public boolean setBlock(Vector3i pos, int layer, BlockState blockState) {
        return this.setBlock(pos.getX(), pos.getY(), pos.getZ(), layer, blockState, false, true);
    }

    public boolean setBlock(Vector3i pos, BlockState blockState, boolean direct) {
        return this.setBlock(pos, blockState, direct, true);
    }

    public boolean setBlock(Vector3i pos, int layer, BlockState blockState, boolean direct, boolean update) {
        return setBlock(pos.getX(), pos.getY(), pos.getZ(), layer, blockState, direct, update);
    }

    public boolean setBlock(Vector3i pos, BlockState blockState, boolean direct, boolean update) {
        return setBlock(pos.getX(), pos.getY(), pos.getZ(), 0, blockState, direct, update);
    }

    public boolean setBlock(Vector4i pos, BlockState blockState) {
        return this.setBlock(pos, blockState, false);
    }

    public boolean setBlock(Vector4i pos, BlockState blockState, boolean direct) {
        return this.setBlock(pos, blockState, direct, true);
    }

    public boolean setBlock(Vector4i pos, BlockState blockState, boolean direct, boolean update) {
        return setBlock(pos.getX(), pos.getY(), pos.getZ(), pos.getW(), blockState, direct, update);
    }

    public boolean setBlock(int x, int y, int z, int layer, BlockState state, boolean direct, boolean update) {
        if (y < 0 || y >= 256) {
            return false;
        }
        Chunk chunk = this.getChunk(x >> 4, z >> 4);
        BlockState previousState = chunk.getAndSetBlock(x & 0xF, y, z & 0xF, layer, state);
        if (previousState == state) {
            return false;
        }
        int cx = x >> 4;
        int cz = z >> 4;
        long index = Chunk.key(cx, cz);

        Block block = new CloudBlock(this, Vector3i.from(x, y, z), new BlockState[]{
                layer == 0 ? state : chunk.getBlock(x & 0xf, y, z & 0xf),
                layer == 1 ? state : chunk.getBlock(x & 0xf, y, z & 0xf, 1)
        });
        if (direct) {
            this.sendBlocks(this.getChunkPlayers(cx, cz).toArray(new Player[0]), new Block[]{block}, UpdateBlockPacket.FLAG_ALL_PRIORITY);
        } else {
            addBlockChange(index, x, y, z, layer);
        }

        if (update) {
            BlockBehavior behavior = state.getBehavior();
            BlockBehavior previousBehavior = previousState.getBehavior();
            if (previousBehavior.isTransparent() != behavior.isTransparent() ||
                    previousBehavior.getLightLevel(block) != behavior.getLightLevel(block)) {
                addLightUpdate(x, y, z);
            }
            BlockUpdateEvent ev = new BlockUpdateEvent(block);
            this.server.getPluginManager().callEvent(ev);
            if (!ev.isCancelled()) {
                for (Entity entity : this.getNearbyEntities(new SimpleAxisAlignedBB(x - 1, y - 1, z - 1, x + 1, y + 1, z + 1))) {
                    this.scheduleEntityUpdate(entity);
                }
                behavior.onUpdate(block, BLOCK_UPDATE_NORMAL);
                this.updateAround(x, y, z);
            }
        }
        return true;
    }

    private void addBlockChange(int x, int y, int z) {
        addBlockChange(x, y, z, 0);
    }

    private void addBlockChange(int x, int y, int z, int layer) {
        long index = Chunk.key(x >> 4, z >> 4);
        addBlockChange(index, x, y, z, layer);
    }

    private void addBlockChange(long index, int x, int y, int z, int layer) {
        IntSet current;
        try {
            current = this.changedBlocks.get(index, IntOpenHashSet::new);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Unable to get block changes", e);
        }
        synchronized (changedBlocks) {
            current.add(Chunk.blockKey(x, y, z, layer));
        }
    }

    @Nonnull
    public DroppedItem dropItem(Vector3i source, Item item) {
        return this.dropItem(source.toFloat().add(0.5, 0.5, 0.5), item);
    }

    @Nonnull
    public DroppedItem dropItem(Vector3f source, Item item) {
        return this.dropItem(source, item, null);
    }

    @Nonnull
    public DroppedItem dropItem(Vector3f source, Item item, Vector3f motion) {
        return this.dropItem(source, item, motion, 10);
    }

    @Nonnull
    public DroppedItem dropItem(Vector3f source, Item item, Vector3f motion, int delay) {
        return this.dropItem(source, item, motion, false, delay);
    }

    @Nonnull
    public DroppedItem dropItem(Vector3f source, Item item, Vector3f motion, boolean dropAround, int delay) {
        checkNotNull(source, "source");
        checkNotNull(item, "item");
        checkArgument(!item.isNull(), "invalid item");

        if (motion == null) {
            if (dropAround) {
                float f = ThreadLocalRandom.current().nextFloat() * 0.5f;
                float f1 = ThreadLocalRandom.current().nextFloat() * ((float) Math.PI * 2);

                motion = Vector3f.from(-MathHelper.sin(f1) * f, 0.2, MathHelper.cos(f1) * f);
            } else {
                motion = Vector3f.from(new java.util.Random().nextDouble() * 0.2 - 0.1, 0.2,
                        new java.util.Random().nextDouble() * 0.2 - 0.1);
            }
        }

        DroppedItem droppedItem = EntityRegistry.get().newEntity(EntityTypes.ITEM, Location.from(source, this));
        droppedItem.setPosition(source);
        droppedItem.setMotion(motion);
        droppedItem.setHealth(5);
        droppedItem.setItem(item);
        droppedItem.setPickupDelay(delay);

        droppedItem.spawnToAll();

        this.server.getPluginManager().callEvent(new ItemSpawnEvent(droppedItem));

        return droppedItem;
    }

    public Item useBreakOn(Vector3i pos) {
        return this.useBreakOn(pos, null);
    }

    public Item useBreakOn(Vector3i pos, Item item) {
        return this.useBreakOn(pos, item, null);
    }

    public Item useBreakOn(Vector3i pos, Item item, Player player) {
        return this.useBreakOn(pos, item, player, false);
    }

    public Item useBreakOn(Vector3i pos, Item item, Player player, boolean createParticles) {
        return useBreakOn(pos, null, item, player, createParticles);
    }

    public Item useBreakOn(Vector3i pos, Direction face, Item item, Player player, boolean createParticles) {
        if (player != null && player.getGamemode() == GameMode.SPECTATOR) {
            return null;
        }
        Block target = this.getBlock(pos);
        Item[] drops;
        BlockBehavior targetBehavior = target.getState().getBehavior();
        int dropExp = targetBehavior.getDropExp();

        if (item == null) {
            item = Item.get(BlockTypes.AIR, 0, 0);
        }

        boolean isSilkTouch = item.getEnchantment(Enchantment.ID_SILK_TOUCH) != null;

        if (player != null) {
            if (player.getGamemode() == GameMode.ADVENTURE && !item.canDestroy(target.getState().getType())) {
                return null;
            }

            double breakTime = targetBehavior.getBreakTime(target.getState(), item, player);
            // this in
            // block
            // class

            if (player.isCreative() && breakTime > 0.15) {
                breakTime = 0.15;
            }

            if (player.hasEffect(Effect.HASTE)) {
                breakTime *= 1 - (0.2 * (player.getEffect(Effect.HASTE).getAmplifier() + 1));
            }

            if (player.hasEffect(Effect.MINING_FATIGUE)) {
                breakTime *= 1 - (0.3 * (player.getEffect(Effect.MINING_FATIGUE).getAmplifier() + 1));
            }

            Enchantment eff = item.getEnchantment(Enchantment.ID_EFFICIENCY);

            if (eff != null && eff.getLevel() > 0) {
                breakTime *= 1 - (0.3 * eff.getLevel());
            }

            breakTime -= 0.15;

            Item[] eventDrops;
            if (!player.isSurvival()) {
                eventDrops = new Item[0];
            } else if (isSilkTouch && targetBehavior.canSilkTouch()) {
                eventDrops = new Item[]{targetBehavior.toItem(target)};
            } else {
                eventDrops = targetBehavior.getDrops(target, item);
            }

            BlockBreakEvent ev = new BlockBreakEvent(player, target, face, item, eventDrops, dropExp, player.isCreative(),
                    (player.lastBreak + breakTime * 1000) > System.currentTimeMillis());

            if (player.isSurvival() && !targetBehavior.isBreakable(item)) {
                ev.setCancelled();
            } else if (!player.isOp() && isInSpawnRadius(target.getPosition())) {
                ev.setCancelled();
            }

            this.server.getPluginManager().callEvent(ev);
            if (ev.isCancelled()) {
                return null;
            }

            if (!ev.getInstaBreak() && ev.isFastBreak()) {
                return null;
            }

            player.lastBreak = System.currentTimeMillis();

            drops = ev.getDrops();
            dropExp = ev.getDropExp();
        } else if (!targetBehavior.isBreakable(item)) {
            return null;
        } else if (item.getEnchantment(Enchantment.ID_SILK_TOUCH) != null) {
            drops = new Item[]{targetBehavior.toItem(target)};
        } else {
            drops = targetBehavior.getDrops(target, item);
        }

        Block above = this.getLoadedBlock(target.getPosition().add(0, 1, 0));
        if (above != null) {
            if (above.getState().getType() == BlockTypes.FIRE) {
                this.setBlock(above.getPosition(), BlockStates.AIR, true);
            }
        }

        if (createParticles) {
            Chunk chunk = this.getLoadedChunk(target.getPosition());
            if (chunk != null) {
                this.addParticle(new DestroyBlockParticle(target.getPosition().toFloat().add(0.5, 0.5, 0.5), target.getState()), chunk.getPlayerLoaders());
            }
        }

        // Close BlockEntity before we check onBreak
        BlockEntity blockEntity = this.getLoadedBlockEntity(target.getPosition());
        if (blockEntity != null) {
            blockEntity.onBreak();
            blockEntity.close();

            this.updateComparatorOutputLevel(target.getPosition());
        }

        targetBehavior.onBreak(target, item, player);

        item.useOn(target);
        if (item.isTool() && item.getMeta() >= item.getMaxDurability()) {
            item = Item.get(BlockTypes.AIR, 0, 0);
        }

        if (this.getGameRules().get(GameRules.DO_TILE_DROPS)) {
            if (!isSilkTouch && player != null && player.isSurvival() && dropExp > 0 && drops.length != 0) {
                this.dropExpOrb(pos, dropExp);
            }

            if (player == null || player.isSurvival()) {
                for (Item drop : drops) {
                    if (drop.getCount() > 0) {
                        this.dropItem(pos.add(0.5, 0.5, 0.5), drop);
                    }
                }
            }
        }

        return item;
    }

    public void dropExpOrb(Vector3i source, int exp) {
        dropExpOrb(source.toFloat().add(0.5f, 0.5f, 0.5f), exp, null);
    }

    public void dropExpOrb(Vector3f source, int exp) {
        dropExpOrb(source, exp, null);
    }

    public void dropExpOrb(Vector3f source, int exp, Vector3f motion) {
        dropExpOrb(source, exp, motion, 10);
    }

    public void dropExpOrb(Vector3f source, int exp, Vector3f motion, int delay) {
        Random rand = ThreadLocalRandom.current();
        for (int split : ExperienceOrb.splitIntoOrbSizes(exp)) {
            ExperienceOrb experienceOrb = EntityRegistry.get().newEntity(EntityTypes.XP_ORB, Location.from(source, this));
            experienceOrb.setPickupDelay(delay);
            experienceOrb.setExperience(split);
            experienceOrb.setRotation(rand.nextFloat() * 360f, 0);
            experienceOrb.setMotion(motion == null ? Vector3f.from(
                    (rand.nextDouble() * 0.2 - 0.1) * 2,
                    rand.nextDouble() * 0.4,
                    (rand.nextDouble() * 0.2 - 0.1) * 2) : motion);
            experienceOrb.spawnToAll();
        }
    }

    public Item useItemOn(Vector3i vector, Item item, Direction face, Vector3f clickPos) {
        return this.useItemOn(vector, item, face, clickPos, null);
    }

    public Item useItemOn(Vector3i vector, Item item, Direction face, Vector3f clickPos, Player player) {
        return this.useItemOn(vector, item, face, clickPos, player, true);
    }


    public Item useItemOn(Vector3i vector, Item item, Direction face, Vector3f clickPos, Player player, boolean playSound) {
        Block target = this.getBlock(vector);
        BlockBehavior targetBehavior = target.getState().getBehavior();
        Block block = target.getSide(face);
        BlockBehavior behavior = block.getState().getBehavior();
        Vector3i blockPos = block.getPosition();

        if (blockPos.getY() > 255 || blockPos.getY() < 0) {
            return null;
        }

        if (target.getState().getType() == BlockTypes.AIR) {
            return null;
        }

        if (player != null) {
            PlayerInteractEvent ev = new PlayerInteractEvent(player, item, target, face, PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK);

            if (player.getGamemode() == GameMode.SPECTATOR) {
                ev.setCancelled();
            }

            if (!player.isOp() && isInSpawnRadius(target.getPosition())) {
                ev.setCancelled();
            }

            this.server.getPluginManager().callEvent(ev);
            if (!ev.isCancelled()) {
                targetBehavior.onUpdate(target, BLOCK_UPDATE_TOUCH);
                if ((!player.isSneaking() || player.getInventory().getItemInHand().isNull()) && targetBehavior.canBeActivated(target) && targetBehavior.onActivate(target, item, player)) {
                    if (item.isTool() && item.getMeta() >= item.getMaxDurability()) {
                        item = Item.get(BlockTypes.AIR, 0, 0);
                    }
                    return item;
                }

                if (item.canBeActivated() && item.onActivate(this, player, block, target, face, clickPos)) {
                    if (item.getCount() <= 0) {
                        item = Item.get(BlockTypes.AIR, 0, 0);
                        return item;
                    }
                }
            } else {
                if (item.getId() == ItemIds.BUCKET && ItemBucket.getBlockIdFromDamage(item.getMeta()) == BlockTypes.FLOWING_WATER) {
                    player.getLevel().sendBlocks(new Player[]{player}, new Block[]{new CloudBlock(this, block.getPosition(), new BlockState[]{BlockStates.AIR, BlockStates.AIR})}, UpdateBlockPacket.FLAG_ALL_PRIORITY);
                }
                return null;
            }
        } else if (targetBehavior.canBeActivated(target) && targetBehavior.onActivate(target, item)) {
            if (item.isTool() && item.getMeta() >= item.getMaxDurability()) {
                item = Item.get(BlockTypes.AIR, 0, 0);
            }
            return item;
        }
        BlockState hand;
        if (item.canBePlaced()) {
            hand = item.getBlock();
        } else {
            return null;
        }

        if (!(behavior.canBeReplaced(block)
                || (hand instanceof BlockBehaviorSlab && (block instanceof BlockBehaviorSlab || target instanceof BlockBehaviorSlab)))) {
            return null;
        }

        if (targetBehavior.canBeReplaced(target)) {
            block = target;
            behavior = targetBehavior;
        }

        BlockBehavior handBehavior = BlockRegistry.get().getBehavior(hand.getType());

        if (!handBehavior.canPassThrough() && handBehavior.getBoundingBox() != null) {
            Set<Entity> entities = this.getCollidingEntities(handBehavior.getBoundingBox());
            int realCount = 0;
            for (Entity e : entities) {
                if (e instanceof EntityArrow || e instanceof DroppedItem || (e instanceof Player && ((Player) e).isSpectator())) {
                    continue;
                }
                ++realCount;
            }

            if (player != null) {
                Vector3f diff = player.getNextPosition().sub(player.getPosition());
                if (diff.lengthSquared() > 0.00001) {
                    AxisAlignedBB bb = player.getBoundingBox().getOffsetBoundingBox(diff);
                    if (handBehavior.getBoundingBox().intersectsWith(bb)) {
                        ++realCount;
                    }
                }
            }

            if (realCount > 0) {
                return null; // Entity in block
            }
        }

        if (player != null) {
            BlockPlaceEvent event = new BlockPlaceEvent(player, hand, block, target, item);
            if (player.getGamemode() == GameMode.ADVENTURE && item.canPlaceOn(target.getState().getType())) {
                event.setCancelled();
            }
            if (!player.isOp() && isInSpawnRadius(target.getPosition())) {
                event.setCancelled();
            }
            this.server.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return null;
            }
        }

        BlockBehavior liquidBehavior = block.getState().getBehavior();
        BlockState air = block.getExtra();

        Vector3i pos = null;
        if (air == BlockStates.AIR && (liquidBehavior instanceof BlockBehaviorLiquid) && ((BlockBehaviorLiquid) liquidBehavior).usesWaterLogging()
            && (block.getState().ensureTrait(BlockTraits.FLUID_LEVEL) == 0) // Remove this line when MCPE-33345 is resolved
        ) {
            pos = block.getPosition();

            block.set(block.getState(), 1, false, false);
            block.set(air, false, false);

            this.scheduleUpdate(block, 1);
        }

        try {
            if (!handBehavior.place(item, block, target, face, clickPos, player)) {
                if (pos != null) {
                    this.setBlock(pos, 0, block.getState(), false, false);
                    this.setBlock(pos, 1, air, false, false);
                }
                return null;
            }
        } catch (Exception e) {
            if (pos != null) {
                this.setBlock(pos, 0, block.getState(), false, false);
                this.setBlock(pos, 1, air, false, false);
            }
            throw e;
        }

        if (player != null) {
            if (!player.isCreative()) {
                item.setCount(item.getCount() - 1);
            }
        }

        if (playSound) {
            this.addLevelSoundEvent(block.getPosition().toFloat(), SoundEvent.PLACE, BlockRegistry.get().getRuntimeId(hand));
        }

        if (item.getCount() <= 0) {
            item = Item.get(BlockTypes.AIR, 0, 0);
        }
        return item;
    }

    public boolean isInSpawnRadius(Vector3i vector3) {
        int distance = this.server.getSpawnRadius();
        if (distance > -1) {

            Vector2i t = vector3.toVector2(true);
            Vector2i s = this.getSpawnLocation().getPosition().toInt().toVector2(true);
            return t.distance(s) <= distance;
        }
        return false;
    }

    public Entity getEntity(long entityId) {
        synchronized (entities) {
            return this.entities.containsKey(entityId) ? this.entities.get(entityId) : null;
        }
    }

    public Entity[] getEntities() {
        synchronized (entities) {
            return entities.values().toArray(new Entity[0]);
        }
    }

    public Set<Entity> getCollidingEntities(AxisAlignedBB bb) {
        return this.getCollidingEntities(bb, null);
    }

    public Set<Entity> getCollidingEntities(AxisAlignedBB bb, Entity entity) {
        ImmutableSet.Builder<Entity> entities = null;

        if (entity == null || entity.canCollide()) {
            int minX = NukkitMath.floorDouble((bb.getMinX() - 2) / 16);
            int maxX = NukkitMath.ceilDouble((bb.getMaxX() + 2) / 16);
            int minZ = NukkitMath.floorDouble((bb.getMinZ() - 2) / 16);
            int maxZ = NukkitMath.ceilDouble((bb.getMaxZ() + 2) / 16);

            for (int x = minX; x <= maxX; ++x) {
                for (int z = minZ; z <= maxZ; ++z) {
                    Set<Entity> colliding = this.getLoadedChunkEntities(x, z);
                    for (Entity ent : colliding) {
                        if ((entity == null || (ent != entity && entity.canCollideWith(ent)))
                                && ent.getBoundingBox().intersectsWith(bb)) {
                            if (entities == null) {
                                entities = ImmutableSet.builder();
                            }
                            entities.add(ent);
                        }
                    }
                }
            }
        }

        return entities == null ? ImmutableSet.of() : entities.build();
    }

    public Set<Entity> getNearbyEntities(AxisAlignedBB bb) {
        return this.getNearbyEntities(bb, null);
    }

    public Set<Entity> getNearbyEntities(AxisAlignedBB bb, Entity entity) {
        return getNearbyEntities(bb, entity, false);
    }

    public Set<Entity> getNearbyEntities(AxisAlignedBB bb, Entity entity, boolean loadChunks) {
        int minX = NukkitMath.floorDouble((bb.getMinX() - 2) * 0.0625);
        int maxX = NukkitMath.ceilDouble((bb.getMaxX() + 2) * 0.0625);
        int minZ = NukkitMath.floorDouble((bb.getMinZ() - 2) * 0.0625);
        int maxZ = NukkitMath.ceilDouble((bb.getMaxZ() + 2) * 0.0625);

        ImmutableSet.Builder<Entity> entities = null;

        for (int x = minX; x <= maxX; ++x) {
            for (int z = minZ; z <= maxZ; ++z) {
                Set<Entity> entitiesInRange = loadChunks ? this.getChunkEntities(x, z) : this.getLoadedChunkEntities(x, z);
                for (Entity entityInRange : entitiesInRange) {
                    if (entityInRange != entity && entityInRange.getBoundingBox().intersectsWith(bb)) {
                        if (entities == null) {
                            entities = ImmutableSet.builder();
                        }
                        entities.add(entityInRange);
                    }
                }
            }
        }

        return entities == null ? ImmutableSet.of() : entities.build();
    }

    public Set<BlockEntity> getBlockEntities() {
        return blockEntities;
    }

    public Map<Long, Player> getPlayers() {
        return players;
    }

    public Map<Integer, ChunkLoader> getLoaders() {
        return loaders;
    }


    public BlockEntity getBlockEntity(Vector3i pos) {
        Chunk chunk = this.getChunk(pos);
        return chunk.getBlockEntity(pos.getX() & 0x0f, pos.getY() & 0xff, pos.getZ() & 0x0f);
    }

    @Nullable
    public BlockEntity getLoadedBlockEntity(Vector3i pos) {
        Chunk chunk = this.getLoadedChunk(pos);
        return chunk == null ? null : chunk.getBlockEntity(pos.getX() & 0x0f, pos.getY() & 0xff, pos.getZ() & 0x0f);
    }

    @Nonnull
    public Set<Entity> getChunkEntities(int chunkX, int chunkZ) {
        return this.getChunk(chunkX, chunkZ).getEntities();
    }

    @Nonnull
    public Set<Entity> getLoadedChunkEntities(int chunkX, int chunkZ) {
        Chunk chunk = this.getLoadedChunk(chunkX, chunkZ);
        if (chunk != null) {
            return ImmutableSet.<Entity>builder()
                    .addAll(chunk.getEntities())
                    .addAll(chunk.getPlayers())
                    .build();
        }
        return Collections.emptySet();
    }


    @Nonnull
    public Collection<BlockEntity> getChunkBlockEntities(int chunkX, int chunkZ) {
        return this.getChunk(chunkX, chunkZ).getBlockEntities();
    }

    @Nonnull
    public Collection<BlockEntity> getLoadedBlockEntities(int chunkX, int chunkZ) {
        Chunk chunk = this.getLoadedChunk(chunkX, chunkZ);
        return chunk == null ? Collections.emptyList() : chunk.getBlockEntities();
    }

    @Override
    public BlockState getBlockAt(int x, int y, int z, int layer) {
        Chunk chunk = this.getChunk(x >> 4, z >> 4);
        return chunk.getBlock(x & 0x0f, y & 0xff, z & 0x0f, layer);
    }

    @Override
    public void setBlockAt(int x, int y, int z, int layer, BlockState blockState) {
        Chunk chunk = this.getChunk(x >> 4, z >> 4);
        chunk.setBlock(x & 0x0f, y & 0xff, z & 0x0f, layer, blockState);
        addBlockChange(x, y, z, layer);
    }

    public int getBiomeId(int x, int z) {
        return this.getChunk(x >> 4, z >> 4).getBiome(x & 0xF, z & 0xF);
    }

    public void setBiomeId(int x, int z, byte biomeId) {
        this.getChunk(x >> 4, z >> 4).setBiome(x & 0xF, z & 0xF, biomeId);
    }

    public int getSkyLightAt(int x, int y, int z) {
        return this.getChunk(x >> 4, z >> 4).getBlockLight(x & 0xF, y, z & 0xF);
    }

    public void setSkyLightAt(int x, int y, int z, int level) {
        this.getChunk(x >> 4, z >> 4).setBlockLight(x & 0xF, y, z & 0xF, level);
    }

    public int getBlockLightAt(int x, int y, int z) {
        return this.getChunk(x >> 4, z >> 4).getBlockLight(x & 0xF, y, z & 0xF);
    }

    public void setBlockLightAt(int x, int y, int z, int level) {
        this.getChunk(x >> 4, z >> 4).setBlockLight(x & 0xF, y, z & 0xF, level);
    }

    public int getHighestBlock(int x, int z) {
        return this.getChunk(x >> 4, z >> 4).getHighestBlock(x & 0xF, z & 0xF);
    }

    public Chunk getLoadedChunk(Vector3f pos) {
        return this.getLoadedChunk(pos.getFloorX() >> 4, pos.getFloorZ() >> 4);
    }

    public Chunk getLoadedChunk(Vector3i pos) {
        return this.getLoadedChunk(pos.getX() >> 4, pos.getZ() >> 4);
    }

    @Nullable
    public Chunk getLoadedChunk(long chunkKey) {
        return this.chunkManager.getLoadedChunk(chunkKey);
    }

    @Nullable
    public Chunk getLoadedChunk(int chunkX, int chunkZ) {
        return this.chunkManager.getLoadedChunk(chunkX, chunkZ);
    }

    @Nonnull
    public Set<Chunk> getChunks() {
        return this.chunkManager.getLoadedChunks();
    }

    public int getChunkCount() {
        return this.chunkManager.getLoadedCount();
    }

    @Nonnull
    public Chunk getChunk(long chunkKey) {
        return this.chunkManager.getChunk(Chunk.fromKeyX(chunkKey), Chunk.fromKeyZ(chunkKey));
    }

    @Nonnull
    public Chunk getChunk(Vector3f pos) {
        return this.getChunk(pos.getFloorX() >> 4, pos.getFloorZ() >> 4);
    }

    @Nonnull
    public Chunk getChunk(Vector3i pos) {
        return this.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
    }

    @Nonnull
    public Chunk getChunk(int chunkX, int chunkZ) {
        return this.chunkManager.getChunk(chunkX, chunkZ);
    }

    @Nonnull
    public CompletableFuture<Chunk> getChunkFuture(int chunkX, int chunkZ) {
        return this.chunkManager.getChunkFuture(chunkX, chunkZ);
    }

    public int getHighestBlockAt(int x, int z) {
        return this.getChunk(x >> 4, z >> 4).getHighestBlock(x & 0x0f, z & 0x0f);
    }

    public BlockColor getMapColorAt(int x, int z) {
        Chunk chunk = this.getChunk(x >> 4, z >> 4);
        int y = chunk.getHighestBlock(x & 0x0f, z & 0x0f);
        while (y > 1) {
            Block block = getBlock(Vector3i.from(x, y, z));
            BlockColor blockColor = block.getState().getBehavior().getColor(block);
            if (blockColor.getAlpha() == 0x00) {
                y--;
            } else {
                return blockColor;
            }
        }
        return BlockColor.VOID_BLOCK_COLOR;
    }

    public boolean isChunkLoaded(Vector4i pos) {
        return isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4);
    }

    public boolean isChunkLoaded(Vector3i pos) {
        return isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4);
    }

    public boolean isChunkLoaded(int x, int z) {
        return this.chunkManager.isChunkLoaded(x, z);
    }

    private boolean areNeighboringChunksLoaded(long hash) {
        return this.chunkManager.isChunkLoaded(hash + 1) &&
                this.chunkManager.isChunkLoaded(hash - 1) &&
                this.chunkManager.isChunkLoaded(hash + (1L << 32)) &&
                this.chunkManager.isChunkLoaded(hash - (1L << 32));
    }

    public Location getSpawnLocation() {
        return Location.from(this.levelData.getSpawn().toFloat().add(0.5f, 0f, 0.5f), this);
    }

    public void setSpawnLocation(Vector3f pos) {
        Location previousSpawn = this.getSpawnLocation();
        Vector3i blockPos = pos.toInt();
        this.levelData.setSpawn(blockPos);
        this.server.getPluginManager().callEvent(new SpawnChangeEvent(this, previousSpawn.getPosition()));

        SetSpawnPositionPacket packet = new SetSpawnPositionPacket();
        packet.setSpawnType(SetSpawnPositionPacket.Type.WORLD_SPAWN);
        packet.setBlockPosition(blockPos);
        Server.broadcastPacket(this.players.values(), packet);
    }

    public void scheduleEntityUpdate(Entity entity) {
        checkNotNull(entity, "entity");
        this.updateEntities.add(entity);
    }

    public void removeEntity(Entity entity) {
        if (entity.getLevel() != this) {
            throw new LevelException("Invalid Entity level");
        }

        if (entity instanceof Player) {
            this.players.remove(entity.getUniqueId());
            this.checkSleep();
        } else {
            entity.close();
        }

        synchronized (entities) {
            this.entities.remove(entity.getUniqueId());
        }
        this.updateEntities.remove(entity);
    }

    public void addEntity(Entity entity) {
        if (entity.getLevel() != this) {
            throw new LevelException("Invalid Entity level");
        }

        if (entity instanceof Player) {
            this.players.put(entity.getUniqueId(), (Player) entity);
        }
        synchronized (entities) {
            this.entities.put(entity.getUniqueId(), entity);
        }
    }

    public void addBlockEntity(BlockEntity blockEntity) {
        if (blockEntity.getLevel() != this) {
            throw new LevelException("Invalid Block Entity level");
        }
        blockEntities.add(blockEntity);
    }

    public void scheduleBlockEntityUpdate(BlockEntity entity) {
        checkNotNull(entity, "entity");
        Preconditions.checkArgument(entity.getLevel() == this, "BlockEntity is not in this level");
        if (!updateBlockEntities.contains(entity)) {
            updateBlockEntities.add(entity);
        }
    }

    public void removeBlockEntity(BlockEntity entity) {
        checkNotNull(entity, "entity");
        Preconditions.checkArgument(entity.getLevel() == this, "BlockEntity is not in this level");
        blockEntities.remove(entity);
        updateBlockEntities.remove(entity);
    }

    public boolean isSpawnChunk(int x, int z) {
        Vector3i spawn = this.levelData.getSpawn();
        return Math.abs(x - (spawn.getX() >> 4)) <= 1 && Math.abs(z - (spawn.getX() >> 4)) <= 1;
    }

    public Location getSafeSpawn() {
        return this.getSafeSpawn(null);
    }

    public Location getSafeSpawn(Location spawn) {
        if (spawn == null || spawn.getY() < 1) {
            spawn = this.getSpawnLocation();
        }

        if (spawn != null) {
            Vector3f v = spawn.getPosition();
            Chunk chunk = this.getLoadedChunk(v);
            int x = v.getFloorX() & 0x0f;
            int z = v.getFloorZ() & 0x0f;
            if (chunk != null) {
                int y = NukkitMath.clamp(v.getFloorY(), 0, 254);
                boolean wasAir = !this.isFullBlock(chunk.getBlock(x, y + 1, z));
                for (; y > 0; --y) {
                    BlockState blockState = chunk.getBlock(x, y, z);
                    if (this.isFullBlock(blockState)) {
                        if (wasAir) {
                            y++;
                            break;
                        }
                    } else {
                        wasAir = true;
                    }
                }

                for (; y >= 0 && y < 255; y++) {
                    BlockState blockState = chunk.getBlock(x, (y + 1), z);
                    if (!this.isFullBlock(blockState)) {
                        blockState = chunk.getBlock(x, y, z);
                        if (!this.isFullBlock(blockState)) {
                            return Location.from(spawn.getX(), y, spawn.getZ(), spawn.getYaw(), spawn.getPitch(), this);
                        }
                    }
                }

                v = Vector3f.from(spawn.getX(), y, spawn.getZ());
            }

            return Location.from(v.getX(), v.getY(), v.getZ(), spawn.getYaw(), spawn.getPitch(), this);
        }

        return null;
    }

    public int getTime() {
        return (int) this.levelData.getTime();
    }

    public boolean isDaytime() {
        return this.skyLightSubtracted < 4;
    }

    public void setTime(int time) {
        this.levelData.setTime(time);
        this.sendTime();
    }

    public long getCurrentTick() {
        return this.levelData.getCurrentTick();
    }

    public String getName() {
        return this.levelData.getName();
    }

    public void stopTime() {
        this.getGameRules().put(GameRules.DO_DAYLIGHT_CYCLE, false);
        this.sendTime();
    }

    public void startTime() {
        this.getGameRules().put(GameRules.DO_DAYLIGHT_CYCLE, true);
        this.sendTime();
    }

    public long getSeed() {
        return this.levelData.getRandomSeed();
    }

    public void setSeed(long seed) {
        this.levelData.setRandomSeed(seed);
    }

    public void doChunkGarbageCollection() {
        this.chunkManager.tick();
    }

    @Override
    public void setMetadata(String metadataKey, MetadataValue newMetadataValue) throws Exception {
        this.server.getLevelMetadata().setMetadata(this, metadataKey, newMetadataValue);
    }

    @Override
    public List<MetadataValue> getMetadata(String metadataKey) throws Exception {
        return this.server.getLevelMetadata().getMetadata(this, metadataKey);
    }

    @Override
    public boolean hasMetadata(String metadataKey) throws Exception {
        return this.server.getLevelMetadata().hasMetadata(this, metadataKey);
    }

    @Override
    public void removeMetadata(String metadataKey, Plugin owningPlugin) throws Exception {
        this.server.getLevelMetadata().removeMetadata(this, metadataKey, owningPlugin);
    }

    public void addEntityMovement(BaseEntity entity, double x, double y, double z, double yaw, double pitch, double headYaw) {
        MoveEntityAbsolutePacket packet = new MoveEntityAbsolutePacket();
        packet.setRuntimeEntityId(entity.getRuntimeId());
        packet.setPosition(Vector3f.from(x, y, z));
        packet.setRotation(Vector3f.from(pitch, yaw, headYaw));

        Server.broadcastPacket(entity.getViewers(), packet);
    }

    public boolean isRaining() {
        return this.levelData.getRainLevel() > 0;
    }

    public boolean setRaining(boolean raining) {
        WeatherChangeEvent ev = new WeatherChangeEvent(this, raining);
        this.getServer().getPluginManager().callEvent(ev);

        if (ev.isCancelled()) {
            return false;
        }

        this.levelData.setRainLevel(raining ? 1 : 0);

        LevelEventPacket packet = new LevelEventPacket();
        // These numbers are from Minecraft

        if (raining) {
            packet.setType(LevelEventType.START_RAINING);
            packet.setData(ThreadLocalRandom.current().nextInt(50000) + 10000);
            setRainTime(ThreadLocalRandom.current().nextInt(12000) + 12000);
        } else {
            packet.setType(LevelEventType.STOP_RAINING);
            setRainTime(ThreadLocalRandom.current().nextInt(168000) + 12000);
        }
        packet.setPosition(Vector3f.ZERO);

        Server.broadcastPacket(this.getPlayers().values(), packet);

        return true;
    }

    public int getRainTime() {
        return this.levelData.getRainTime();
    }

    public void setRainTime(int rainTime) {
        this.levelData.setRainTime(rainTime);
    }

    public boolean isThundering() {
        return isRaining() && this.levelData.getLightningLevel() > 0;
    }

    public boolean setThundering(boolean thundering) {
        ThunderChangeEvent ev = new ThunderChangeEvent(this, thundering);
        this.getServer().getPluginManager().callEvent(ev);

        if (ev.isCancelled()) {
            return false;
        }

        if (thundering && !isRaining()) {
            setRaining(true);
        }

        this.levelData.setLightningLevel(thundering ? 1 : 0);

        LevelEventPacket packet = new LevelEventPacket();
        // These numbers are from Minecraft
        if (thundering) {
            packet.setType(LevelEventType.START_THUNDERSTORM);
            packet.setData(ThreadLocalRandom.current().nextInt(50000) + 10000);
            setThunderTime(ThreadLocalRandom.current().nextInt(12000) + 3600);
        } else {
            packet.setType(LevelEventType.STOP_THUNDERSTORM);
            setThunderTime(ThreadLocalRandom.current().nextInt(168000) + 12000);
        }
        packet.setPosition(Vector3f.ZERO);

        Server.broadcastPacket(this.getPlayers().values(), packet);

        return true;
    }

    public int getThunderTime() {
        return this.levelData.getLightningTime();
    }

    public void setThunderTime(int thunderTime) {
        this.levelData.setLightningTime(thunderTime);
    }

    public void sendWeather(Player[] players) {
        if (players == null) {
            players = this.getPlayers().values().toArray(new Player[0]);
        }

        LevelEventPacket rainEvent = new LevelEventPacket();
        if (this.isRaining()) {
            rainEvent.setType(LevelEventType.START_RAINING);
            rainEvent.setData(ThreadLocalRandom.current().nextInt(50000) + 10000);
        } else {
            rainEvent.setType(LevelEventType.STOP_RAINING);
        }
        rainEvent.setPosition(Vector3f.ZERO);
        Server.broadcastPacket(players, rainEvent);

        LevelEventPacket thunderEvent = new LevelEventPacket();
        if (this.isThundering()) {
            thunderEvent.setType(LevelEventType.START_THUNDERSTORM);
            thunderEvent.setData(ThreadLocalRandom.current().nextInt(50000) + 10000);
        } else {
            thunderEvent.setType(LevelEventType.STOP_THUNDERSTORM);
        }
        thunderEvent.setPosition(Vector3f.ZERO);
        Server.broadcastPacket(players, thunderEvent);
    }

    public void sendWeather(Player player) {
        if (player != null) {
            this.sendWeather(new Player[]{player});
        }
    }

    public void sendWeather(Collection<Player> players) {
        if (players == null) {
            players = this.getPlayers().values();
        }
        this.sendWeather(players.toArray(new Player[0]));
    }

    public int getDimension() {
        return this.levelData.getDimension();
    }

    public boolean canBlockSeeSky(Vector3f pos) {
        return canBlockSeeSky(pos.getFloorX(), pos.getFloorY(), pos.getFloorZ());
    }

    public boolean canBlockSeeSky(Vector3i pos) {
        return canBlockSeeSky(pos.getX(), pos.getY(), pos.getZ());
    }

    public boolean canBlockSeeSky(int x, int y, int z) {
        return this.getHighestBlockAt(x, z) < y;
    }

    public int getStrongPower(Vector3i pos, Direction direction) {
        Block block = this.getBlock(pos);
        return block.getState().getBehavior().getStrongPower(block, direction);
    }

    public int getStrongPower(Vector3i pos) {
        int i = 0;

        for (Direction face : Direction.values()) {
            i = Math.max(i, this.getStrongPower(face.getOffset(pos), face));

            if (i >= 15) {
                return i;
            }
        }

        return i;
//        i = Math.max(i, this.getStrongPower(pos.down(), BlockFace.DOWN));
//
//        if (i >= 15) {
//            return i;
//        } else {
//            i = Math.max(i, this.getStrongPower(pos.up(), BlockFace.UP));
//
//            if (i >= 15) {
//                return i;
//            } else {
//                i = Math.max(i, this.getStrongPower(pos.north(), BlockFace.NORTH));
//
//                if (i >= 15) {
//                    return i;
//                } else {
//                    i = Math.max(i, this.getStrongPower(pos.south(), BlockFace.SOUTH));
//
//                    if (i >= 15) {
//                        return i;
//                    } else {
//                        i = Math.max(i, this.getStrongPower(pos.west(), BlockFace.WEST));
//
//                        if (i >= 15) {
//                            return i;
//                        } else {
//                            i = Math.max(i, this.getStrongPower(pos.east(), BlockFace.EAST));
//                            return i >= 15 ? i : i;
//                        }
//                    }
//                }
//            }
//        }
    }

    public boolean isSidePowered(Vector3i pos, Direction face) {
        return this.getRedstonePower(pos, face) > 0;
    }

    public int getRedstonePower(Vector3i pos, Direction face) {
        Block block = this.getBlock(pos);
        val behavior = block.getState().getBehavior();
        return behavior.isNormalBlock(block) ? this.getStrongPower(pos) : behavior.getWeakPower(block, face);
    }

    public boolean isBlockPowered(Vector3i pos) {
        for (Direction face : Direction.values()) {
            if (this.getRedstonePower(face.getOffset(pos), face) > 0) {
                return true;
            }
        }

        return false;
    }

    public int isBlockIndirectlyGettingPowered(Vector3i pos) {
        int power = 0;

        for (Direction face : Direction.values()) {
            int blockPower = this.getRedstonePower(face.getOffset(pos), face);

            if (blockPower >= 15) {
                return 15;
            }

            if (blockPower > power) {
                power = blockPower;
            }
        }

        return power;
    }

    public boolean isAreaLoaded(AxisAlignedBB bb) {
        if (bb.getMaxY() < 0 || bb.getMinY() >= 256) {
            return false;
        }
        int minX = NukkitMath.floorDouble(bb.getMinX()) >> 4;
        int minZ = NukkitMath.floorDouble(bb.getMinZ()) >> 4;
        int maxX = NukkitMath.floorDouble(bb.getMaxX()) >> 4;
        int maxZ = NukkitMath.floorDouble(bb.getMaxZ()) >> 4;

        for (int x = minX; x <= maxX; ++x) {
            for (int z = minZ; z <= maxZ; ++z) {
                if (!this.isChunkLoaded(x, z)) {
                    return false;
                }
            }
        }

        return true;
    }

    public int getUpdateLCG() {
        return (this.updateLCG = (this.updateLCG * 3) ^ LCG_CONSTANT);
    }

    public Generator getGenerator() {
        return this.generator;
    }

    @Override
    public String toString() {
        return "Level(id=" + id + ")";
    }

    //    private static void orderGetRidings(Entity entity, LongSet set) {
//        if (entity.riding != null) {
//            if(!set.add(entity.riding.getId())) {
//                throw new RuntimeException("Circular entity link detected (id = "+entity.riding.getId()+")");
//            }
//            orderGetRidings(entity.riding, set);
//        }
//    }
//
//    public List<Entity> orderChunkEntitiesForSpawn(int chunkX, int chunkZ) {
//        return orderChunkEntitiesForSpawn(getChunk(chunkX, chunkZ, false));
//    }
//
//    public List<Entity> orderChunkEntitiesForSpawn(Chunk chunk) {
//        Comparator<Entity> comparator = (o1, o2) -> {
//            if (o1.riding == null) {
//                if(o2 == null) {
//                    return 0;
//                }
//
//                return -1;
//            }
//
//            if (o2.riding == null) {
//                return 1;
//            }
//
//            LongSet ridings = new LongOpenHashSet();
//            orderGetRidings(o1, ridings);
//
//            if(ridings.contains(o2.getId())) {
//                return 1;
//            }
//
//            ridings.clear();
//            orderGetRidings(o2, ridings);
//
//            if(ridings.contains(o1.getId())) {
//                return -1;
//            }
//
//            return 0;
//        };
//
//        List<Entity> sorted = new ArrayList<>(chunk.getEntities().values());
//        sorted.sort(comparator);
//
//        return sorted;
//    }
}
