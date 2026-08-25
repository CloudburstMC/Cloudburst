package org.cloudburstmc.server.level;

import co.aikar.timings.Timing;
import co.aikar.timings.TimingsHistory;
import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.longs.*;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import lombok.Getter;
import lombok.Synchronized;
import lombok.extern.log4j.Log4j2;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.block.component.BlockShapeContext;
import org.cloudburstmc.api.block.component.TickBlockHandler;
import org.cloudburstmc.api.blockentity.BlockEntity;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.misc.DroppedItem;
import org.cloudburstmc.api.entity.misc.ExperienceOrb;
import org.cloudburstmc.api.entity.misc.LightningBolt;
import org.cloudburstmc.api.event.block.BlockBreakEvent;
import org.cloudburstmc.api.event.block.BlockDropItemEvent;
import org.cloudburstmc.api.event.block.BlockPlaceEvent;
import org.cloudburstmc.api.event.block.BlockUpdateEvent;
import org.cloudburstmc.api.event.level.*;
import org.cloudburstmc.api.event.player.PlayerInteractEvent;
import org.cloudburstmc.api.item.EquipmentSlot;
import org.cloudburstmc.api.item.ItemComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.component.UseHandler;
import org.cloudburstmc.api.item.component.UseOnHandler;
import org.cloudburstmc.api.level.ChunkLoader;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.LevelException;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.api.level.chunk.ChunkSection;
import org.cloudburstmc.api.level.gamerule.GameRuleMap;
import org.cloudburstmc.api.level.gamerule.GameRules;
import org.cloudburstmc.api.player.GameMode;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.registry.RegistryException;
import org.cloudburstmc.api.util.*;
import org.cloudburstmc.api.util.component.ComponentMap;
import org.cloudburstmc.api.util.data.SlabSlot;
import org.cloudburstmc.math.GenericMath;
import org.cloudburstmc.math.vector.Vector2i;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.math.vector.Vector4i;
import org.cloudburstmc.protocol.bedrock.data.GameRuleData;
import org.cloudburstmc.protocol.bedrock.data.LevelEvent;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.protocol.bedrock.packet.*;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.block.BlockPalette;
import org.cloudburstmc.server.block.CloudBlock;
import org.cloudburstmc.server.block.component.LiquidBlockHandlers;
import org.cloudburstmc.server.block.util.BlockUtils;
import org.cloudburstmc.server.blockentity.BaseBlockEntity;
import org.cloudburstmc.server.config.ServerConfig;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.entity.projectile.EntityArrow;
import org.cloudburstmc.server.item.ToolUtils;
import org.cloudburstmc.server.level.chunk.CloudChunk;
import org.cloudburstmc.server.level.chunk.CloudChunkSection;
import org.cloudburstmc.server.level.chunk.SectionTickList;
import org.cloudburstmc.server.level.collision.CloudVoxelShapes;
import org.cloudburstmc.server.level.collision.CollisionEngine;
import org.cloudburstmc.server.level.generator.Generator;
import org.cloudburstmc.server.level.manager.LevelChunkManager;
import org.cloudburstmc.server.level.particle.DestroyBlockParticle;
import org.cloudburstmc.server.level.particle.Particle;
import org.cloudburstmc.server.level.provider.LevelProvider;
import org.cloudburstmc.server.level.weather.PrecipitationHandler;
import org.cloudburstmc.server.math.MathHelper;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.registry.CloudItemRegistry;
import org.cloudburstmc.server.registry.EntityRegistry;
import org.cloudburstmc.server.registry.GeneratorRegistry;
import org.cloudburstmc.server.scheduler.BlockUpdateScheduler;
import org.cloudburstmc.server.timings.LevelTimings;
import org.cloudburstmc.server.utils.BlockUpdateEntry;
import org.cloudburstmc.server.utils.Hash;

import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.LongPredicate;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;

import static com.google.common.base.Preconditions.*;

@Log4j2
public class CloudLevel implements Level {

    public static final int DIMENSION_OVERWORLD = 0;
    public static final int DIMENSION_NETHER = 1;
    public static final int DIMENSION_THE_END = 2;

    // Lower values use less memory
    public static final int MAX_BLOCK_CACHE = 512;
    private static final double MINIMUM_PREDICTED_BREAK_PROGRESS = 0.7D;

    private final Set<BlockEntity> blockEntities = Collections.newSetFromMap(new IdentityHashMap<>());

    private final Long2ObjectOpenHashMap<CloudPlayer> players = new Long2ObjectOpenHashMap<>();

    private final ConcurrentHashMap<Long, Entity> entities = new ConcurrentHashMap<>();
    private static final RemovalListener<Long, ByteBuf> cacheRemover = notification -> notification.getValue().release();

    private final ConcurrentLinkedQueue<BlockEntity> updateBlockEntities = new ConcurrentLinkedQueue<>();

    private final CloudServer server;
    private final int maxLiquidTicks;
    @Getter
    private final int waterOverLavaFlowSpeed;
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
    private final Long2ObjectMap<IntSet> lightQueue = new Long2ObjectOpenHashMap<>();
    // Storing extra blocks past 512 is redundant
    private final Map<Character, Object> changeBlocksFullMap = new HashMap<Character, Object>() {
        @Override
        public int size() {
            return Character.MAX_VALUE;
        }
    };


    @Getter
    private final BlockUpdateScheduler blockUpdateQueue;
    @Getter
    private final BlockUpdateScheduler liquidUpdateQueue;

    private static final ThreadLocal<NeighborUpdateContext> NEIGHBOR_UPDATES = ThreadLocal.withInitial(NeighborUpdateContext::new);

    private boolean autoSave;

    //private BlockMetadataStore blockMetadata;

    public int sleepTicks = 0;

    private final int chunkTickRadius;
    private final Long2IntMap chunkTickList = new Long2IntOpenHashMap();
    private final int chunksPerTicks;
    private final boolean clearChunksOnTick;

    private final RandomGenerator random = RandomGenerator.getDefault();
    private @Nullable List<DroppedItem> capturedBlockDrops;
    private int updateLCG = this.random.nextInt();

    private static final int LCG_CONSTANT = 1013904223;
    private final String id;

    private double lastTickDuration;

    private final Long2ObjectOpenHashMap<Set<Player>> chunkPlayers = new Long2ObjectOpenHashMap<>();
    private final Cache<Long, ByteBuf> chunkCache = CacheBuilder.newBuilder()
            .softValues()
            .removalListener(cacheRemover)
            .build();
    private final LevelChunkManager chunkManager;
    private final LevelData levelData;
    private final CollisionEngine collisionEngine;

    private Generator generator;

    @Inject
    CloudBlockRegistry blockRegistry;

    @Inject
    CloudItemRegistry itemRegistry;

    @Inject
    EntityRegistry entityRegistry;

    @Inject
    GeneratorRegistry generatorRegistry;

    @Inject
    CloudLevel(CloudServer server, String id, LevelProvider levelProvider, LevelData levelData, GeneratorRegistry generatorRegistry) {
        this.id = id;
        //this.blockMetadata = new BlockMetadataStore(this);
        this.server = server;
        ServerConfig.World levelConfig = server.getConfig().getWorlds().get(id);
        ServerConfig.Level defaults = server.getConfig().getLevel();
        this.maxLiquidTicks = levelConfig != null && levelConfig.getMaxLiquidTicks() != null
                ? levelConfig.getMaxLiquidTicks() : defaults.getMaxLiquidTicks();
        this.waterOverLavaFlowSpeed = levelConfig != null && levelConfig.getWaterOverLavaFlowSpeed() != null
                ? levelConfig.getWaterOverLavaFlowSpeed() : defaults.getWaterOverLavaFlowSpeed();
        checkArgument(this.maxLiquidTicks > 0, "max-liquid-ticks must be greater than zero for level %s", id);
        checkArgument(this.waterOverLavaFlowSpeed > 0,
                "water-over-lava-flow-speed must be greater than zero for level %s", id);
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
//            this.server.getLogger().info(this.server.getLanguage().translate("cloudburst.level.updating",
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

        this.generator = generatorRegistry.getGeneratorFactory(this.levelData.getGenerator()).create(this.getSeed(), this.levelData.getGeneratorOptions());

        if (this.levelData.getRainTime() <= 0) {
            setRainTime(this.random.nextInt(168000) + 12000);
        }

        if (this.levelData.getLightningTime() <= 0) {
            setThunderTime(this.random.nextInt(168000) + 12000);
        }

        this.chunkTickRadius = Math.min(this.server.getViewDistance(),
                Math.max(1, this.server.getConfig().getChunkTicking().getTickRadius()));
        this.chunksPerTicks = this.server.getConfig().getChunkTicking().getPerTick();
        this.chunkTickList.clear();
        this.clearChunksOnTick = this.server.getConfig().getChunkTicking().isClearTickList();
        this.chunkManager = new LevelChunkManager(this);
        this.collisionEngine = new CollisionEngine(this);

        LongPredicate loadedChunk = chunkKey -> this.chunkManager.isChunkLoaded(chunkKey);
        this.blockUpdateQueue = new BlockUpdateScheduler(this.levelData.getCurrentTick(), loadedChunk, this::tickBlock);
        this.liquidUpdateQueue = new BlockUpdateScheduler(this.levelData.getCurrentTick(), loadedChunk, this::tickLiquid);

        this.skyLightSubtracted = this.calculateSkylightSubtracted(1);
    }

    public void reloadGenerator() {
        this.generator = this.generatorRegistry.getGeneratorFactory(this.levelData.getGenerator()).create(this.getSeed(), this.levelData.getGeneratorOptions());
    }

    public double getLastTickDuration() {
        return this.lastTickDuration;
    }

    public RandomGenerator getRandom() {
        return this.random;
    }

    public void setLastTickDuration(double lastTickDuration) {
        this.lastTickDuration = lastTickDuration;
    }

    /*public BlockMetadataStore getBlockMetadata() {
        return this.blockMetadata;
    }*/

    public void init() {
    }

    public CloudServer getServer() {
        return server;
    }

    final public LevelProvider getProvider() {
        return this.provider;
    }

    public final String getId() {
        return this.id;
    }

    public void close() {
        this.save(true, true);

        try {
            this.provider.close();
        } catch (IOException e) {
            throw new LevelException("Error occurred whilst closing level", e);
        }
        this.provider = null;
        // this.blockMetadata = null;
    }

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
            CloudServer.broadcastPacket(players, packet);
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
                CloudServer.broadcastPackets(players, packets);
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
        packet.setMolangVariablesJson(Optional.empty());

        if (players == null || players.length == 0) {
            addChunkPacket(pos.getFloorX() >> 4, pos.getFloorZ() >> 4, packet);
        } else {
            CloudServer.broadcastPacket(players, packet);
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

        this.server.getEventManager().fire(ev);

        if (!force && ev.isCancelled()) {
            return false;
        }

        log.info(this.server.getLanguage().translate("cloudburst.level.unloading", "§a" + this.getName() + "§r"));
        CloudLevel defaultLevel = this.server.getDefaultLevel();

        for (Player player : new ArrayList<>(this.getPlayers().values())) {
            if (this == defaultLevel || defaultLevel == null) {
                ((CloudPlayer) player).close(((CloudPlayer) player).leaveMessage(), "Forced default level unload");
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

    @Override
    public Set<CloudPlayer> getChunkPlayers(int chunkX, int chunkZ) {
        ImmutableSet.Builder<CloudPlayer> players = ImmutableSet.builder();
        for (CloudPlayer player : this.players.values()) {
            if (player.isChunkInView(chunkX, chunkZ)) {
                players.add(player);
            }
        }
        return players.build();
    }

    @Override
    public Set<ChunkLoader> getChunkLoaders(int chunkX, int chunkZ) {
        CloudChunk chunk = this.getLoadedChunk(chunkX, chunkZ);
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
        this.levelData.checkTime();
    }

    private boolean doDaylightCycle() {
        return this.getGameRules().get(GameRules.DO_DAYLIGHT_CYCLE);
    }

    private boolean doWeatherCycle() {
        return this.getGameRules().get(GameRules.DO_WEATHER_CYCLE);
    }

    @Override
    public void sendTime(Player... players) {
        /*if (this.stopTime) { //TODO
            SetTimePacket pk0 = new SetTimePacket();
            pk0.time = (int) this.time;
            player.dataPacket(pk0);
        }*/

        SetTimePacket pk = new SetTimePacket();
        pk.setTime(this.getTime());

        CloudServer.broadcastPacket(players, pk);
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
        long index = CloudChunk.key(chunkX, chunkZ);
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
            if (this.getDimension() != CloudLevel.DIMENSION_NETHER && this.getDimension() != CloudLevel.DIMENSION_THE_END && this.doWeatherCycle()) {
                this.levelData.setRainTime(getRainTime() - 1);
                if (this.levelData.getRainTime() <= 0) {
                    if (!this.setRaining(this.levelData.getRainLevel() <= 0)) {
                        if (this.levelData.getRainLevel() > 0) {
                            setRainTime(this.random.nextInt(12000) + 12000);
                        } else {
                            setRainTime(this.random.nextInt(168000) + 12000);
                        }
                    }
                }

                this.levelData.setLightningTime(this.levelData.getLightningTime() - 1);
                if (this.levelData.getLightningTime() <= 0) {
                    if (!this.setThundering(this.levelData.getLightningLevel() <= 0)) {
                        if (this.levelData.getLightningLevel() > 0) {
                            setThunderTime(this.random.nextInt(12000) + 3600);
                        } else {
                            setThunderTime(this.random.nextInt(168000) + 12000);
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

            int maxBlockTicks = this.server.getConfig().getLevel().getMaxBlockTicks();
            int maxLiquidTicks = this.maxLiquidTicks;
            this.chunkManager.promoteReadyChunks();
            try (Timing ignored2 = timings.scheduledBlockTicks.startTiming()) {
                this.blockUpdateQueue.tick(this.getCurrentTick(), maxBlockTicks);
            }

            try (Timing ignored2 = timings.scheduledLiquidTicks.startTiming()) {
                this.liquidUpdateQueue.tick(this.getCurrentTick(), maxLiquidTicks);
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
                this.tickChunks();

                synchronized (changedBlocks) {
                    ConcurrentMap<Long, IntSet> changedBlocks = this.changedBlocks.asMap();
                    if (!changedBlocks.isEmpty()) {
                        if (!this.players.isEmpty()) {
                            Iterator<Map.Entry<Long, IntSet>> iter = changedBlocks.entrySet().iterator();
                            while (iter.hasNext()) {
                                Map.Entry<Long, IntSet> entry = iter.next();
                                long chunkKey = entry.getKey();
                                IntSet blocks = entry.getValue();
                                int chunkX = CloudChunk.fromKeyX(chunkKey);
                                int chunkZ = CloudChunk.fromKeyZ(chunkKey);
                                if (blocks.size() > MAX_BLOCK_CACHE) {
                                    Chunk chunk = this.getLoadedChunk(chunkX, chunkZ);
                                    if (chunk != null) {
                                        for (Player p : this.getChunkPlayers(chunkX, chunkZ)) {
                                            ((CloudPlayer) p).onChunkChanged(chunk);
                                        }
                                    }
                                } else {
                                    Collection<CloudPlayer> toSend = this.getChunkPlayers(chunkX, chunkZ);
                                    Player[] playerArray = toSend.toArray(new Player[0]);
                                    Block[] blocksArray = new Block[blocks.size()];
                                    int i = 0;
                                    for (int blockKey : blocks) {
                                        blocksArray[i++] = this.getBlock(CloudChunk.fromBlockKey(chunkKey, blockKey, this.getMinHeight()));
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
                        CloudChunk chunk = this.getLoadedChunk(index);

                        Set<CloudPlayer> viewers;
                        if (chunk == null || (viewers = chunk.getViewers()).isEmpty()) {
                            // Chunk is unloaded.
                            continue;
                        }

                        for (BedrockPacket packet : this.chunkPackets.get(index)) {
                            CloudServer.broadcastPacket(viewers, packet);
                        }
                    }
                    this.chunkPackets.clear();
                }

                if (this.levelData.getGameRules().isDirty()) {
                    GameRulesChangedPacket packet = new GameRulesChangedPacket();
                    //this.levelData.getGameRules().toNetwork(packet.getGameRules());
                    this.levelData.getGameRules().forEach((gameRule, o) -> {
                        packet.getGameRules().add(new GameRuleData<>(gameRule.getName(), o));
                    });
                    CloudServer.broadcastPacket(players.values().toArray(new CloudPlayer[0]), packet);
                    this.levelData.getGameRules().refresh();
                }
            }
        }
    }

    private void performThunder(Chunk chunk) {
        if (areNeighboringChunksLoaded(CloudChunk.key(chunk.getX(), chunk.getZ()))) return;
        if (this.random.nextInt(10000) == 0) {
            int LCG = this.getUpdateLCG() >> 2;

            int chunkX = chunk.getX() * 16;
            int chunkZ = chunk.getZ() * 16;
            Vector3f vector = this.adjustPosToNearbyEntity(Vector3f.from(chunkX + (LCG & 0xf), 0, chunkZ + (LCG >> 8 & 0xf)));

            BlockType blockType = chunk.getBlock(vector.getFloorX() & 0xf, vector.getFloorY(), vector.getFloorZ() & 0xf).getType();
            if (blockType != BlockTypes.TALL_GRASS && !blockType.isLiquid())
                vector = vector.add(0, 1, 0);

            Location location = Location.from(vector, this);
            LightningBolt bolt = this.entityRegistry.newEntity(EntityTypes.LIGHTNING_BOLT, location);
            bolt.setPosition(vector);
            LightningStrikeEvent ev = new LightningStrikeEvent(this, bolt);
            getServer().getEventManager().fire(ev);
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
            int time = this.getTime() % CloudLevel.TIME_FULL;

            if (time >= CloudLevel.TIME_NIGHT && time < CloudLevel.TIME_SUNRISE) {
                this.setTime(this.getTime() + CloudLevel.TIME_FULL - time);

                for (Player p : this.getPlayers().values()) {
                    p.stopSleep();
                }
            }
        }
    }

    public Vector3f adjustPosToNearbyEntity(Vector3f pos) {
        pos = Vector3f.from(pos.getX(), this.getHighestBlockAt(pos.getFloorX(), pos.getFloorZ()), pos.getZ());
        BoundingBox boundingBox = new BoundingBox(pos, Vector3f.from(pos.getX(), 255, pos.getZ())).inflate(3, 3, 3);
        List<Entity> list = new ArrayList<>();

        for (Entity entity : this.getCollidingEntities(boundingBox)) {
            if (entity.isAlive() && canBlockSeeSky(entity.getPosition())) {
                list.add(entity);
            }
        }

        if (!list.isEmpty()) {
            return list.get(this.random.nextInt(list.size())).getPosition();
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

            Block block = blocks[i >> 1];
            int chunkX = block.getChunk().getX();
            int chunkZ = block.getChunk().getZ();

            if (optimizeRebuilds) {
                long index = CloudChunk.key(chunkX, chunkZ);
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
                updateBlockPacket.setDefinition(BlockPalette.INSTANCE.getDefinition(block.getState()));
                updateBlockPacket2.setDefinition(BlockPalette.INSTANCE.getDefinition(block.getExtra()));
            } catch (RegistryException e) {
                throw new IllegalStateException("Unable to create BlockUpdatePacket at " +
                        block.getPosition() + " in " + getName(), e);
            }
            packets[i] = updateBlockPacket;
            packets[i + 1] = updateBlockPacket2;
        }
        CloudServer.broadcastPackets(target, packets);
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

        this.server.getEventManager().fire(new LevelSaveEvent(this));

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

        RandomGenerator random = this.random;
        if (!this.loaders.isEmpty()) {
            for (ChunkLoader loader : this.loaders.values()) {
                int chunkX = (int) loader.getX() >> 4;
                int chunkZ = (int) loader.getZ() >> 4;

                long index = CloudChunk.key(chunkX, chunkZ);
                int existingLoaders = Math.max(0, this.chunkTickList.getOrDefault(index, 0));
                this.chunkTickList.put(index, existingLoaders + 1);
                for (int chunk = 0; chunk < chunksPerLoader; ++chunk) {
                    int dx = random.nextInt(2 * randRange) - randRange;
                    int dz = random.nextInt(2 * randRange) - randRange;
                    long hash = CloudChunk.key(dx + chunkX, dz + chunkZ);
                    if (!this.chunkTickList.containsKey(hash) && this.chunkManager.isChunkLoaded(hash)) {
                        this.chunkTickList.put(hash, -1);
                    }
                }
            }
        }

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

                int chunkX = CloudChunk.fromKeyX(index);
                int chunkZ = CloudChunk.fromKeyZ(index);

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
                    int minHeight = this.getMinHeight();
                    int baseWorldX = chunkX << 4;
                    int baseWorldZ = chunkZ << 4;
                    RandomGenerator rng = this.random;

                    for (int i = 0; i < tickSpeed; i++) {
                        if (rng.nextInt(48) == 0) {
                            PrecipitationHandler.tickColumn(this, baseWorldX + rng.nextInt(16), baseWorldZ + rng.nextInt(16));
                        }
                    }

                    for (int sectionIdx = 0; sectionIdx < sections.length; sectionIdx++) {
                        ChunkSection section = sections[sectionIdx];
                        if (section == null) {
                            continue;
                        }

                        CloudChunkSection cs = (CloudChunkSection) section;
                        if (!cs.isRandomlyTicking()) {
                            continue;
                        }

                        SectionTickList tickList = cs.getTickingList();
                        int tickingBlocks = tickList.size();

                        int sectionBaseY = (sectionIdx << 4) + minHeight;
                        for (int i = 0; i < tickSpeed; ++i) {
                            if (rng.nextInt(4096) >= tickingBlocks) {
                                continue;
                            }

                            int idx = rng.nextInt(tickingBlocks);

                            BlockState state = tickList.getState(idx);
                            int lx = tickList.getX(idx);
                            int ly = tickList.getY(idx);
                            int lz = tickList.getZ(idx);

                            int worldX = baseWorldX + lx;
                            int worldY = sectionBaseY + ly;
                            int worldZ = baseWorldZ + lz;

                            ComponentMap behaviors = this.blockRegistry.requireComponents(state.getType());
                            TickBlockHandler randomTick = behaviors.require(BlockComponents.ON_RANDOM_TICK);

                            Block block = new CloudBlock(
                                    this,
                                    Vector3i.from(worldX, worldY, worldZ),
                                    new BlockState[]{state, section.getBlock(lx, ly, lz, 1)}
                            );

                            randomTick.execute(block, rng);
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

            Block block = this.getBlock(side.relative(pos));
            block.requireComponent(BlockComponents.ON_REDSTONE_UPDATE).execute(block);
        }
    }

    public void updateComparatorOutputLevel(Vector3i v) {
        for (Direction face : Direction.Plane.HORIZONTAL) {
            Vector3i pos = face.relative(v);

            if (this.isChunkLoaded(pos)) {
                Block block = this.getBlock(pos);

                ComponentMap behaviors = block.getComponents();
                // FIXME: Needs reimplementation
//                if (BlockBehaviorRedstoneDiode.isDiode(behavior)) {
//                    behavior.onUpdate(block, BLOCK_UPDATE_REDSTONE);
//                } else if (behavior.isNormalBlock(block)) {
//                    pos = face.getOffset(pos);
//                    block = this.getBlock(pos);
//
//                    if (BlockBehaviorRedstoneDiode.isDiode(behavior)) {
//                        behavior.onUpdate(block, BLOCK_UPDATE_REDSTONE);
//                    }
//                }
            }
        }
    }

    public void updateAround(int posX, int posY, int posZ) {
        NeighborUpdateContext context = NEIGHBOR_UPDATES.get();
        Vector3i centre = Vector3i.from(posX, posY, posZ);
        int cap = this.server.getConfig().getLevel().getMaxChainedNeighborUpdates();
        if (cap >= 0 && context.count >= cap) {
            if (context.count == cap) {
                log.error("Neighbor-update chain exceeded {} updates near {}. Skipping remaining updates.", cap, centre);
            }

            context.count++;
            if (!context.processing) {
                context.count = 0;
            }

            return;
        }

        NeighborUpdate update = new NeighborUpdate(this, centre, this.getBlock(centre));
        context.count++;
        if (context.processing) {
            context.added.add(update);
            return;
        }

        context.stack.push(update);
        context.processing = true;

        try {
            while (!context.stack.isEmpty() || !context.added.isEmpty()) {
                for (int i = context.added.size() - 1; i >= 0; i--) {
                    context.stack.push(context.added.get(i));
                }
                context.added.clear();

                NeighborUpdate current = context.stack.getFirst();
                while (context.added.isEmpty()) {
                    if (!current.runNext()) {
                        context.stack.pop();
                        break;
                    }
                }
            }
        } finally {
            context.stack.clear();
            context.added.clear();
            context.count = 0;
            context.processing = false;
        }
    }

    private void updateNeighbour(Block block, Block changed) {
        if (block.getState().getType() == BlockTypes.AIR) {
            return;
        }

        BlockUpdateEvent event = new BlockUpdateEvent(block);
        this.getServer().getEventManager().fire(event);
        if (!event.isCancelled()) {
            block.requireComponent(BlockComponents.ON_NEIGHBOUR_CHANGED).execute(block, changed);
        }
    }

    public void scheduleUpdate(Vector3i pos, int delay) {
        scheduleUpdate(getBlock(pos), delay);
    }

    public void registerTickContainers(long chunkKey) {
        this.blockUpdateQueue.registerTickContainer(chunkKey);
        this.liquidUpdateQueue.registerTickContainer(chunkKey);
    }

    public void unregisterTickContainers(long chunkKey) {
        this.blockUpdateQueue.unregisterTickContainer(chunkKey);
        this.liquidUpdateQueue.unregisterTickContainer(chunkKey);
    }

    public void removeTickContainers(long chunkKey) {
        this.blockUpdateQueue.removeTickContainer(chunkKey);
        this.liquidUpdateQueue.removeTickContainer(chunkKey);
    }

    public boolean areTicksDirty(long chunkKey) {
        return this.blockUpdateQueue.isDirty(chunkKey) || this.liquidUpdateQueue.isDirty(chunkKey);
    }

    public void scheduleUpdate(Block block, int delay) {
        this.scheduleUpdate(block, block.getPosition(), delay, true);
    }

    public void updateAround(Vector3i pos) {
        updateAround(pos.getX(), pos.getY(), pos.getZ());
    }

    public void scheduleUpdate(Block block, Vector3i pos, int delay) {
        this.scheduleUpdate(block, pos, delay, true);
    }

    public void scheduleUpdate(BlockUpdate blockUpdate) {
        this.scheduleUpdate(
                blockUpdate.getBlock(),
                blockUpdate.getPos(),
                blockUpdate.getDelay(),
                blockUpdate.shouldCheckArea());
    }

    private void scheduleUpdate(Block block, Vector3i pos, int delay, boolean checkArea) {
        if (block.getState().getType() == BlockTypes.AIR || (checkArea && !this.isChunkLoaded(pos))) {
            return;
        }

        BlockUpdateEntry entry = BlockUpdateEntry.of(pos, block.getState().getType(), (long) delay + getCurrentTick());
        this.blockUpdateQueue.add(entry);
    }

    @Override
    public boolean cancelScheduledUpdate(Vector3i pos) {
        return this.blockUpdateQueue.remove(BlockUpdateEntry.probe(pos, getBlockState(pos).getType()));
    }

    public boolean isUpdateScheduled(Vector3i pos) {
        return this.blockUpdateQueue.contains(BlockUpdateEntry.probe(pos, getBlockState(pos).getType()));
    }

    public Set<BlockUpdateEntry> getPendingBlockUpdates(CloudChunk chunk) {
        int minX = (chunk.getX() << 4) - 2;
        int maxX = minX + 16 + 2;
        int minZ = (chunk.getZ() << 4) - 2;
        int maxZ = minZ + 16 + 2;

        return this.getPendingBlockUpdates(new BoundingBox(minX, getMinHeight(), minZ, maxX, getMaxHeight(), maxZ));
    }

    public Set<BlockUpdateEntry> getPendingBlockUpdates(BoundingBox boundingBox) {
        Set<BlockUpdateEntry> result = new HashSet<>();
        Set<BlockUpdateEntry> blockTicks = this.blockUpdateQueue.getPendingBlockUpdates(boundingBox);
        Set<BlockUpdateEntry> liquidTicks = this.liquidUpdateQueue.getPendingBlockUpdates(boundingBox);

        if (blockTicks != null) {
            result.addAll(blockTicks);
        }

        if (liquidTicks != null) {
            result.addAll(liquidTicks);
        }

        return result;
    }

    public void clearPendingBlockUpdates(BoundingBox boundingBox) {
        this.blockUpdateQueue.clearArea(boundingBox);
        this.liquidUpdateQueue.clearArea(boundingBox);
    }

    public void copyPendingBlockUpdates(BoundingBox boundingBox, Vector3i offset) {
        this.blockUpdateQueue.copyArea(boundingBox, offset);
        this.liquidUpdateQueue.copyArea(boundingBox, offset);
    }

    public boolean isBlockTickPending(Vector3i pos, Block block) {
        return this.blockUpdateQueue.willTickThisTick(pos, block.getState().getType());
    }

    public boolean isFullBlock(Vector3i pos, BlockState state) {
        ComponentMap behaviors = this.blockRegistry.requireComponents(state.getType());
        VoxelShape shape = behaviors.require(BlockComponents.GET_COLLISION_SHAPE)
                .execute(state, BlockShapeContext.at(this, pos), CollisionContext.empty());
        return CloudVoxelShapes.isFullBlock(shape);
    }

    @Override
    public boolean hasCollision(@Nullable Entity entity, BoundingBox boundingBox, boolean includeEntities) {
        return this.collisionEngine.hasCollision(entity, boundingBox, includeEntities);
    }

    @Override
    public boolean hasBlockCollision(@Nullable Entity entity, BoundingBox boundingBox) {
        return this.collisionEngine.hasBlockCollision(entity, boundingBox);
    }

    public boolean hasBlockCollision(@Nullable Entity entity, BlockState state, Vector3i position, BoundingBox boundingBox) {
        return this.collisionEngine.hasBlockCollision(entity, state, position, boundingBox);
    }

    public boolean collidesWithSuffocatingBlock(@Nullable Entity entity, BoundingBox boundingBox) {
        return this.collisionEngine.collidesWithSuffocatingBlock(entity, boundingBox);
    }

    public Iterable<VoxelShape> getBlockCollisions(@Nullable Entity entity, BoundingBox boundingBox) {
        return this.collisionEngine.getBlockCollisions(entity, boundingBox);
    }

    public Optional<Vector3i> findSupportingBlock(Entity entity, BoundingBox boundingBox) {
        return this.collisionEngine.findSupportingBlock(entity, boundingBox);
    }

    public List<VoxelShape> getEntityCollisions(@Nullable Entity entity, BoundingBox boundingBox) {
        return this.collisionEngine.getEntityCollisions(entity, boundingBox);
    }

    public Vector3f collideBoundingBox(@Nullable Entity entity, Vector3f movement, BoundingBox boundingBox) {
        return this.collisionEngine.collideBoundingBox(entity, movement, boundingBox);
    }

    public void forEachLoadedBlockIntersecting(BoundingBox boundingBox, Consumer<Block> consumer) {
        int minX = GenericMath.floor(boundingBox.getMinX());
        int minY = GenericMath.floor(boundingBox.getMinY());
        int minZ = GenericMath.floor(boundingBox.getMinZ());
        int maxX = GenericMath.ceil(boundingBox.getMaxX());
        int maxY = GenericMath.ceil(boundingBox.getMaxY());
        int maxZ = GenericMath.ceil(boundingBox.getMaxZ());

        for (int z = minZ; z <= maxZ; ++z) {
            for (int x = minX; x <= maxX; ++x) {
                for (int y = minY; y <= maxY; ++y) {
                    Block block = this.getLoadedBlock(x, y, z);
                    if (block != null && block.getState() != BlockStates.AIR && intersectsUnitBlock(boundingBox, x, y, z)) {
                        consumer.accept(block);
                    }
                }
            }
        }
    }

    public boolean hasLoadedBlockIntersecting(BoundingBox boundingBox, Predicate<Block> predicate) {
        int minX = GenericMath.floor(boundingBox.getMinX());
        int minY = GenericMath.floor(boundingBox.getMinY());
        int minZ = GenericMath.floor(boundingBox.getMinZ());
        int maxX = GenericMath.ceil(boundingBox.getMaxX());
        int maxY = GenericMath.ceil(boundingBox.getMaxY());
        int maxZ = GenericMath.ceil(boundingBox.getMaxZ());

        for (int z = minZ; z <= maxZ; ++z) {
            for (int x = minX; x <= maxX; ++x) {
                for (int y = minY; y <= maxY; ++y) {
                    Block block = this.getLoadedBlock(x, y, z);
                    if (block != null && block.getState() != BlockStates.AIR
                            && intersectsUnitBlock(boundingBox, x, y, z)
                            && predicate.test(block)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public void forEachBlockCollision(@Nullable Entity entity, BoundingBox boundingBox, Consumer<Block> consumer) {
        CollisionContext context = CollisionContext.of(entity);
        this.forEachLoadedBlockIntersecting(boundingBox, block -> {
            BlockState state = block.getState();
            Vector3i position = block.getPosition();
            VoxelShape collisionShape = block.requireComponent(BlockComponents.GET_COLLISION_SHAPE)
                    .execute(state, BlockShapeContext.at(this, position), context);
            if (!collisionShape.isEmpty() && collisionShape.overlaps(boundingBox, position.getX(), position.getY(), position.getZ())) {
                consumer.accept(block);
            }
        });
    }

    private static boolean intersectsUnitBlock(BoundingBox boundingBox, int x, int y, int z) {
        return boundingBox.getMaxX() > x
                && boundingBox.getMinX() < x + 1
                && boundingBox.getMaxY() > y
                && boundingBox.getMinY() < y + 1
                && boundingBox.getMaxZ() > z
                && boundingBox.getMinZ() < z + 1;
    }

    @Override
    public boolean hasEntityCollision(@Nullable Entity entity, BoundingBox boundingBox) {
        return this.collisionEngine.hasEntityCollision(entity, boundingBox);
    }

    @Override
    public boolean hasEntityCollision(@Nullable Entity entity, VoxelShape shape, Vector3i position) {
        return this.collisionEngine.hasEntityCollision(entity, shape, position);
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

        int level = chunk.getSkyLight(pos.getX() & 0x0f, pos.getY(), pos.getZ() & 0x0f);
        level -= this.skyLightSubtracted;

        if (level < 15) {
            level = Math.max(chunk.getBlockLight(pos.getX() & 0x0f, pos.getY(), pos.getZ() & 0x0f),
                    level);
        }

        return level;
    }

    @Nullable
    public Block getLoadedBlock(int x, int y, int z) {
        int chunkX = x >> 4;
        int chunkZ = z >> 4;

        Chunk chunk = this.getLoadedChunk(chunkX, chunkZ);

        if (this.isOutsideBuildHeight(y)) {
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

    @NonNull
    public Block getBlock(int x, int y, int z) {
        int chunkX = x >> 4;
        int chunkZ = z >> 4;

        Chunk chunk = this.getChunk(chunkX, chunkZ);

        if (this.isOutsideBuildHeight(y)) {
            return new CloudBlock(this, Vector3i.from(x, y, z), BlockStates.EMPTY);
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

    public void updateBlockLight(Long2ObjectMap<IntSet> map) {
        if (map.isEmpty()) {
            return;
        }
        LongPriorityQueue lightPropagationQueue = new LongArrayFIFOQueue();
        Long2ByteMap lightRemovalQueue = new Long2ByteOpenHashMap();
        LongSet visited = new LongOpenHashSet();
        LongSet removalVisited = new LongOpenHashSet();

        for (Long2ObjectMap.Entry<IntSet> entry : map.long2ObjectEntrySet()) {
            long chunkKey = entry.getLongKey();
            IntSet blocks = entry.getValue();
            int chunkX = CloudChunk.fromKeyX(chunkKey);
            int chunkZ = CloudChunk.fromKeyZ(chunkKey);
            for (int blockKey : blocks) {
                Vector3i position = CloudChunk.fromKeyLight(chunkKey, blockKey, this.getMinHeight());
                Chunk chunk = this.getLoadedChunk(chunkX, chunkZ);
                if (chunk != null) {
                    int lcx = position.getX() & 0xF;
                    int lcz = position.getZ() & 0xF;
                    int oldLevel = chunk.getBlockLight(lcx, position.getY(), lcz);
                    Block block = new CloudBlock(this, Vector3i.from(lcx, position.getY(), lcz), new BlockState[]{
                            chunk.getBlock(lcx, position.getY(), lcz, 0),
                            chunk.getBlock(lcx, position.getY(), lcz, 1)
                    });
                    int newLevel = block.getState().getLightEmission();
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

            if (this.isOutsideBuildHeight(y)) continue;

            Block block = this.getBlock(x, y, z);
            BlockState state = block.getState();

            int lightLevel = this.getBlockLightAt(x, y, z) - state.getLightDampening();

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
        if (this.isOutsideBuildHeight(y)) return;
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
        if (this.isOutsideBuildHeight(y)) return;
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
        if (this.isOutsideBuildHeight(y)) return;
        long index = CloudChunk.key(x >> 4, z >> 4);
        this.lightQueue.computeIfAbsent(index, aLong -> new IntOpenHashSet())
                .add(CloudChunk.blockKeyWithLayer(x, y, z, 0, this.getMinHeight()));
    }

    public boolean setBlockState(int x, int y, int z, int layer, BlockState state, boolean direct, boolean update) {
        if (this.isOutsideBuildHeight(y)) {
            return false;
        }

        Chunk chunk = this.getChunk(x >> 4, z >> 4);
        if (layer == 1 && state != BlockStates.AIR) {
            BlockState primary = chunk.getBlock(x & 0xf, y, z & 0xf);
            if (!state.getType().isLiquid() || !canContainLiquid(primary, state)) {
                return false;
            }
        }

        BlockState oldState = chunk.getAndSetBlock(x & 0xF, y, z & 0xF, layer, state);
        if (oldState == state) {
            return false;
        }

        if (layer == 0) {
            BlockState extra = chunk.getBlock(x & 0xf, y, z & 0xf, 1);
            if (extra == BlockStates.AIR && oldState.getType().isLiquid() && LiquidState.of(oldState).isSource()
                    && canContainLiquid(state, oldState)) {
                chunk.getAndSetBlock(x & 0xf, y, z & 0xf, 1, oldState);
                addBlockChange(x, y, z);
            } else if (extra.getType().isLiquid() && state == BlockStates.AIR) {
                if (LiquidState.of(extra).isSource()) {
                    chunk.getAndSetBlock(x & 0xf, y, z & 0xf, 0, extra);
                    state = extra;
                }

                chunk.getAndSetBlock(x & 0xf, y, z & 0xf, 1, BlockStates.AIR);
                addBlockChange(x, y, z);
            } else if (extra.getType().isLiquid() && !canContainLiquid(state, extra)) {
                chunk.getAndSetBlock(x & 0xf, y, z & 0xf, 1, BlockStates.AIR);
                addBlockChange(x, y, z);
            }
        }

        int cx = x >> 4;
        int cz = z >> 4;
        long index = CloudChunk.key(cx, cz);

        Vector3i position = Vector3i.from(x, y, z);
        Block newBlock = new CloudBlock(this, position, new BlockState[]{
                layer == 0 ? state : chunk.getBlock(x & 0xf, y, z & 0xf),
                layer == 1 ? state : chunk.getBlock(x & 0xf, y, z & 0xf, 1)
        });

        if (direct) {
            this.sendBlocks(this.getChunkPlayers(cx, cz).toArray(new Player[0]), new Block[]{newBlock}, UpdateBlockPacket.FLAG_ALL_PRIORITY);
        } else {
            addBlockChange(index, x, y, z);
        }

        if (update) {
            if (oldState.getTranslucency() != state.getTranslucency() || oldState.getLightEmission() != state.getLightEmission()) {
                addLightUpdate(x, y, z);
            }

            BlockUpdateEvent ev = new BlockUpdateEvent(newBlock);
            this.server.getEventManager().fire(ev);
            if (!ev.isCancelled()) {
                for (Entity entity : this.getNearbyEntities(new BoundingBox(x - 1, y - 1, z - 1, x + 1, y + 1, z + 1))) {
                    this.scheduleEntityUpdate(entity);
                }

                this.updateAround(x, y, z);
                this.scheduleLiquidUpdate(position);

                for (Direction direction : Direction.values()) {
                    this.scheduleLiquidUpdate(direction.relative(position));
                }
            }
        }

        return true;
    }

    @Override
    public float getLiquidHeight(Vector3i position) {
        LiquidState liquid = this.getLiquidState(position);
        if (liquid.isEmpty()) {
            return 0;
        }

        LiquidState above = this.getLiquidState(Direction.UP.relative(position));
        return liquid.isSameFamily(above) ? 1 : liquid.getOwnHeight();
    }

    @Override
    public Vector3f getLiquidFlow(Vector3i position) {
        LiquidState liquid = this.getLiquidState(position);
        return liquid.isEmpty() ? Vector3f.ZERO : LiquidBlockHandlers.flow(this, position, liquid);
    }

    @Override
    public boolean canSetLiquidState(Vector3i position, LiquidState liquid) {
        if (liquid.isEmpty()) {
            return false;
        }

        BlockState primary = this.getBlockState(position);
        if (primary.getType().isLiquid() || primary == BlockStates.AIR) {
            return true;
        }

        return LiquidBlockHandlers.canOccupySecondaryLayer(liquid)
                && (liquid.isSource() ? primary.canContainLiquidSource() : primary.canContainFlowingLiquid());
    }

    @Override
    public boolean setLiquidState(Vector3i position, LiquidState liquid) {
        if (!this.canSetLiquidState(position, liquid)) {
            return false;
        }

        BlockState primary = this.getBlockState(position);
        boolean changed;
        if (primary.getType().isLiquid() || primary == BlockStates.AIR) {
            boolean primaryChanged = this.setBlockState(position, LiquidStateAccess.blockState(liquid));
            boolean extraChanged = this.setBlockState(position, 1, BlockStates.AIR);
            changed = primaryChanged || extraChanged;
        } else {
            changed = this.setBlockState(position, 1, LiquidStateAccess.blockState(liquid));
        }

        if (changed) {
            this.scheduleLiquidUpdate(position);
        }

        return changed;
    }

    @Override
    public boolean removeLiquid(Vector3i position) {
        Block block = this.getBlock(position);
        if (block.getState().getType() == BlockTypes.BUBBLE_COLUMN) {
            boolean extraChanged = this.setBlockState(position.getX(), position.getY(), position.getZ(),
                    1, BlockStates.AIR, false, false);
            boolean primaryChanged = this.setBlockState(position, BlockStates.AIR);
            return primaryChanged || extraChanged;
        }

        int layer = block.getLiquidLayer();
        return layer >= 0 && this.setBlockState(position, layer, BlockStates.AIR);
    }

    public boolean scheduleLiquidUpdate(Vector3i position) {
        LiquidState liquid = this.getBlock(position).getLiquid();
        if (liquid.isEmpty() || this.isLiquidUpdateScheduled(position, liquid)) {
            return false;
        }

        int delay = LiquidBlockHandlers.tickDelay(this, position, liquid);
        this.scheduleLiquidUpdate(position, liquid, delay);
        return this.isLiquidUpdateScheduled(position, liquid);
    }

    public void scheduleLiquidUpdate(Vector3i position, LiquidState liquid, int delay) {
        if (liquid.isEmpty()) {
            throw new IllegalArgumentException("Cannot schedule empty liquid");
        }

        if (!this.isChunkLoaded(position)) {
            return;
        }

        if (isLiquidUpdateScheduled(position, liquid)) {
            return;
        }

        this.liquidUpdateQueue.add(BlockUpdateEntry.of(position, LiquidStateAccess.blockState(liquid).getType(),
                (long) delay + getCurrentTick()));
    }

    private boolean isLiquidUpdateScheduled(Vector3i position, LiquidState liquid) {
        return this.liquidUpdateQueue.contains(BlockUpdateEntry.probe(position, LiquidStateAccess.blockState(liquid).getType()));
    }

    private void tickBlock(Vector3i position, BlockType scheduledType) {
        Block block = this.getBlock(position);
        if (block.getState().getType() != scheduledType) {
            return;
        }

        block.requireComponent(BlockComponents.ON_TICK).execute(block, null);
    }

    private void tickLiquid(Vector3i position, BlockType scheduledType) {
        Block block = this.getBlock(position);
        LiquidState liquid = block.getLiquid();
        if (liquid.isEmpty() || LiquidStateAccess.blockState(liquid).getType() != scheduledType) {
            return;
        }

        this.blockRegistry.requireComponent(scheduledType, BlockComponents.ON_TICK).execute(block, null);
    }

    private void addBlockChange(int x, int y, int z) {
        long index = CloudChunk.key(x >> 4, z >> 4);
        addBlockChange(index, x, y, z);
    }

    private void addBlockChange(long index, int x, int y, int z) {
        synchronized (changedBlocks) {
            try {
                this.changedBlocks.get(index, IntOpenHashSet::new).add(CloudChunk.blockKey(x, y, z, this.getMinHeight()));
            } catch (ExecutionException e) {
                throw new IllegalStateException("Unable to get block changes", e);
            }
        }
    }

    @NonNull
    @Override
    public DroppedItem dropItem(Vector3f source, ItemStack item, Vector3f motion, boolean dropAround, int delay) {
        DroppedItem droppedItem = this.createDroppedItem(source, item, motion, dropAround, delay);
        droppedItem.spawnToAll();
        return droppedItem;
    }

    private DroppedItem createDroppedItem(Vector3f source, ItemStack item, @Nullable Vector3f motion, boolean dropAround, int delay) {
        checkNotNull(source, "source");
        checkNotNull(item, "item");
        checkArgument(!item.isEmpty(), "invalid item");

        if (motion == null) {
            if (dropAround) {
                float f = this.random.nextFloat() * 0.5f;
                float f1 = this.random.nextFloat() * ((float) Math.PI * 2);

                motion = Vector3f.from(-MathHelper.sin(f1) * f, 0.2, MathHelper.cos(f1) * f);
            } else {
                motion = Vector3f.from(
                        this.random.nextDouble() * 0.2 - 0.1, 0.2,
                        this.random.nextDouble() * 0.2 - 0.1
                );
            }
        }


        DroppedItem droppedItem = this.entityRegistry.newEntity(EntityTypes.ITEM, Location.from(source, this));
        droppedItem.setPosition(source);
        droppedItem.setMotion(motion);
        droppedItem.setHealth(5);
        droppedItem.setItem(item);
        droppedItem.setPickupDelay(delay);
        return droppedItem;
    }

    public void dropBlockItem(Vector3i position, ItemStack item) {
        checkNotNull(position, "position");
        checkNotNull(item, "item");
        checkArgument(!item.isEmpty(), "item must not be empty");
        float x = position.getX() + 0.5f + this.random.nextFloat(-0.25f, 0.25f);
        float y = position.getY() + 0.375f + this.random.nextFloat(-0.25f, 0.25f);
        float z = position.getZ() + 0.5f + this.random.nextFloat(-0.25f, 0.25f);
        this.dropBlockItem(Vector3f.from(x, y, z), item);
    }

    private void dropBlockItem(Vector3f position, ItemStack item) {
        if (!this.getGameRules().get(GameRules.DO_TILE_DROPS)) {
            return;
        }

        if (this.capturedBlockDrops != null) {
            this.capturedBlockDrops.add(this.createDroppedItem(position, item, null, false, 10));
            return;
        }

        this.dropItem(position, item);
    }

    public ItemStack breakBlock(Vector3i pos) {
        return this.breakBlock(pos, null);
    }

    public ItemStack breakBlock(Vector3i pos, ItemStack item) {
        return this.breakBlock(pos, item, null);
    }

    public ItemStack breakBlock(Vector3i pos, ItemStack item, Player player) {
        return this.breakBlock(pos, item, player, false);
    }

    public ItemStack breakBlock(Vector3i pos, ItemStack item, Player player, boolean createParticles) {
        return breakBlock(pos, item, player, createParticles, false);
    }

    public ItemStack breakBlockPredicted(Vector3i pos, ItemStack item, Player player, boolean createParticles, @Nullable Boolean fastBreakOverride) {
        boolean fastBreak = fastBreakOverride != null ? fastBreakOverride : isBreakingTooFast(pos, item, player);
        return breakBlock(pos, item, player, createParticles, fastBreak);
    }

    private ItemStack breakBlock(Vector3i pos, ItemStack item, Player player, boolean createParticles, boolean fastBreak) {
        if (player != null && player.getGameMode() == GameMode.SPECTATOR) {
            return null;
        }

        if (item == null || item.isEmpty()) {
            item = ItemStack.EMPTY;
        }

        if (player != null && fastBreak) {
            return null;
        }

        Block target = this.getBlock(pos);
        BlockState brokenState = target.getState();
        ComponentMap targetBehaviors = target.getComponents();

        BlockLootContext lootContext = new BlockLootContext(item, player, this.random);
        boolean correctForDrops = ToolUtils.isCorrectForDrops(item, brokenState);

        ItemStack[] drops;
        boolean dropItems = true;
        int dropExp = 0;

        if (player != null) {
            ComponentMap itemBehaviors = item.isEmpty() ? null : this.itemRegistry.requireComponents(item.getType());
            if (player.getGameMode() == GameMode.ADVENTURE && (itemBehaviors == null || !itemBehaviors.require(ItemComponents.CAN_DESTROY).execute(item, target))) {
                return null;
            }

            if (player.isCreative() && !ToolUtils.canDestroyInCreative(item)) {
                return null;
            }

            if (!player.isCreative() && correctForDrops) {
                dropExp = targetBehaviors.require(BlockComponents.GET_EXPERIENCE)
                        .execute(target, lootContext);
            }

            BlockBreakEvent ev = new BlockBreakEvent(target, player);
            ev.setExpToDrop(dropExp);
            if (!player.isCreative() && !targetBehaviors.require(BlockComponents.IS_BREAKABLE).execute(target, item)) {
                ev.setCancelled();
            } else if (!player.isOp() && isInSpawnRadius(target.getPosition())) {
                ev.setCancelled();
            }

            this.server.getEventManager().fire(ev);
            if (ev.isCancelled()) {
                return null;
            }

            ((CloudPlayer) player).lastBreak = System.currentTimeMillis();

            dropItems = ev.isDropItems();
            dropExp = ev.getExpToDrop();

            target = this.getBlock(pos);
            if (target.getState() == BlockStates.AIR) {
                return item;
            }
            targetBehaviors = target.getComponents();
            correctForDrops = ToolUtils.isCorrectForDrops(item, target.getState());
            drops = !player.isCreative() && correctForDrops
                    ? targetBehaviors.require(BlockComponents.GET_LOOT)
                            .execute(target, lootContext).toArray(ItemStack[]::new)
                    : new ItemStack[0];
        } else if (!targetBehaviors.require(BlockComponents.IS_BREAKABLE).execute(target, item)) {
            return null;
        } else if (correctForDrops) {
            drops = targetBehaviors.require(BlockComponents.GET_LOOT)
                    .execute(target, lootContext).toArray(ItemStack[]::new);
        } else {
            drops = new ItemStack[0];
        }

        boolean doBlockDrops = this.getGameRules().get(GameRules.DO_TILE_DROPS);
        List<DroppedItem> capturedDrops = new ArrayList<>();
        if (player != null) {
            checkState(this.capturedBlockDrops == null, "Block drops are already being captured");
            this.capturedBlockDrops = capturedDrops;
        }

        try {
            Block above = this.getLoadedBlock(target.getPosition().add(0, 1, 0));
            if (above != null) {
                if (above.getState().getType() == BlockTypes.FIRE) {
                    this.setBlockState(above.getPosition(), BlockStates.AIR, true);
                }
            }

            if (createParticles) {
                this.addBlockDestroyParticle(target, player);
            }

            BlockEntity blockEntity = this.getLoadedBlockEntity(target.getPosition());
            if (blockEntity != null) {
                blockEntity.onBreak();
                blockEntity.close();
                this.updateComparatorOutputLevel(target.getPosition());
            }

            ComponentMap itemBehaviors = item.isEmpty() ? null : this.itemRegistry.requireComponents(item.getType());
            if (itemBehaviors != null && player != null && !player.isCreative()) {
                item = itemBehaviors.require(ItemComponents.MINE_BLOCK).execute(item, target, player);
            }

            if (doBlockDrops && player != null && !player.isCreative() && dropItems) {
                for (ItemStack drop : drops) {
                    if (drop != null && !drop.isEmpty() && drop.getCount() > 0) {
                        this.dropBlockItem(pos, drop);
                    }
                }
            }

            targetBehaviors.require(BlockComponents.ON_DESTROY).execute(target, player);
        } finally {
            if (player != null) {
                this.capturedBlockDrops = null;
            }
        }

        if (doBlockDrops && player == null) {
            for (ItemStack drop : drops) {
                if (drop != null && !drop.isEmpty() && drop.getCount() > 0) {
                    this.dropBlockItem(pos, drop);
                }
            }
        }

        if (player != null && dropItems) {
            BlockDropItemEvent dropEvent = new BlockDropItemEvent(this.getBlock(pos), brokenState, player,
                    capturedDrops);
            this.server.getEventManager().fire(dropEvent);
            if (!dropEvent.isCancelled()) {
                for (DroppedItem drop : capturedDrops) {
                    if (drop != null && !drop.getItem().isEmpty() && drop.getItem().getCount() > 0) {
                        drop.spawnToAll();
                    }
                }
            } else {
                capturedDrops.forEach(Entity::close);
            }
        }

        if (doBlockDrops && player != null && !player.isCreative() && dropExp > 0) {
            this.dropExpOrb(pos, dropExp);
        }

        return item;
    }

    private boolean isBreakingTooFast(Vector3i position, ItemStack item, Player player) {
        if (player.isCreative()) {
            return false;
        }

        ItemStack tool = item == null || item.isEmpty() ? ItemStack.EMPTY : item;
        BlockState state = this.getBlockState(position);
        long breakTimeMillis = Math.round(ToolUtils.getBreakTicks(player, tool, state)
                * 50L * MINIMUM_PREDICTED_BREAK_PROGRESS);
        return ((CloudPlayer) player).lastBreak + breakTimeMillis > System.currentTimeMillis();
    }

    private void addBlockDestroyParticle(Block target, @Nullable Player player) {
        Vector3f position = target.getPosition().toFloat().add(0.5f, 0.5f, 0.5f);
        Particle particle = new DestroyBlockParticle(position, target.getState());
        List<Player> viewers = new ArrayList<>(this.getChunkPlayers(position.getFloorX() >> 4, position.getFloorZ() >> 4));

        if (player != null && !viewers.contains(player)) {
            viewers.add(player);
        }

        if (!viewers.isEmpty()) {
            this.addParticle(particle, viewers);
        }
    }

    public void dropExpOrb(Vector3i source, int exp) {
        dropExpOrb(source.toFloat().add(0.5f, 0.5f, 0.5f), exp, null);
    }

    public void dropExpOrb(Vector3f source, int exp) {
        dropExpOrb(source, exp, null);
    }

    public void dropExpOrb(Vector3f source, int exp, @Nullable Vector3f motion) {
        dropExpOrb(source, exp, motion, 10);
    }

    public void dropExpOrb(Vector3f source, int exp, @Nullable Vector3f motion, int delay) {
        for (int split : ExperienceOrb.splitIntoOrbSizes(exp)) {
            this.spawnExperienceOrb(source, split, motion, delay);
        }
    }

    public void spawnExperienceOrb(Vector3f source, int experience, @Nullable Vector3f motion, int pickupDelay) {
        Preconditions.checkArgument(experience > 0, "Experience must be greater than zero");
        Preconditions.checkArgument(pickupDelay >= 0, "Pickup delay cannot be negative");

        RandomGenerator random = this.random;
        ExperienceOrb orb = this.entityRegistry.newEntity(EntityTypes.XP_ORB, Location.from(source, this));
        orb.setPickupDelay(pickupDelay);
        orb.setExperience(experience);
        orb.setRotation(random.nextFloat() * 360, 0);
        orb.setMotion(motion == null ? Vector3f.from(
                (random.nextDouble() * 0.2 - 0.1) * 2,
                random.nextDouble() * 0.4,
                (random.nextDouble() * 0.2 - 0.1) * 2) : motion);
        orb.spawnToAll();
    }

    /**
     * Attempts to interact with the target block.
     * Does not require an item in hand.
     *
     * @return true if the block consumed the interaction
     */
    public boolean tryUseBlock(Block target, Block side, Direction face, ItemStack item, Player player) {
        ComponentMap targetBehaviors = target.getComponents();

        if (player != null) {
            PlayerInteractEvent ev = new PlayerInteractEvent(player, item, target, face, PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK);
            if (player.getGameMode() == GameMode.SPECTATOR) {
                ev.setCancelled();
            }

            if (!player.isOp() && isInSpawnRadius(target.getPosition())) {
                ev.setCancelled();
            }
            this.server.getEventManager().fire(ev);

            if (ev.isCancelled()) {
                return false;
            }


            if (player.isSneaking() && !item.isEmpty()) {
                return false;
            }

            boolean canUse = targetBehaviors.require(BlockComponents.CAN_BE_USED).execute(target, player);
            return canUse && targetBehaviors.require(BlockComponents.USE).execute(target, player, face, item);
        } else {
            return targetBehaviors.require(BlockComponents.CAN_BE_USED).execute(target, null) && targetBehaviors.require(BlockComponents.USE).execute(target, null, face, ItemStack.EMPTY);
        }
    }

    /**
     * Attempts to activate the item's USE_ON handler against the target block.
     * Requires a non-empty item.
     *
     * @return the updated ItemStack if the item was consumed/used, or null if not handled
     */
    public ItemStack tryUseItem(Block target, Direction face, Vector3f clickPos, ItemStack item, Player player) {
        ComponentMap itemBehaviors = this.itemRegistry.requireComponents(item.getType());

        UseOnHandler useOnHandler = itemBehaviors.get(ItemComponents.USE_ON);
        if (useOnHandler == null) {
            return null;
        }

        ItemStack result = useOnHandler.execute(item, player, target.getPosition(), face, clickPos);
        return Objects.requireNonNullElse(result, item);
    }

    /**
     * Attempts to activate an item used in the air.
     * Requires the item to have a {@code USE} component; items that only have {@code USE_ON} are not applicable here.
     *
     * @return the updated {@link ItemStack} if the item was consumed/activated, or {@code null} if not handled
     */
    public ItemStack tryActivateItem(ItemStack item, Player player) {
        ComponentMap itemBehaviors = this.itemRegistry.requireComponents(item.getType());

        UseHandler useHandler = itemBehaviors.get(ItemComponents.USE);
        if (useHandler == null) {
            return null;
        }

        ItemStack result = useHandler.execute(item, player);
        return Objects.requireNonNullElse(result, item);
    }

    /**
     * Attempts to place a block from the held item against the target block.
     * Requires a non-empty item.
     *
     * @return the updated ItemStack after placement, or null if placement was rejected
     */
    public ItemStack tryPlaceBlock(Block target, Block side, Direction face, Vector3f clickPos, ItemStack item, Player player, boolean playSound) {
        ComponentMap itemBehaviors = this.itemRegistry.requireComponents(item.getType());

        @SuppressWarnings("unchecked")
        BlockState hand = ((Optional<BlockState>) itemBehaviors.require(ItemComponents.GET_BLOCK).execute(item)).orElse(null);
        if (hand == null) {
            return null;
        }

        boolean isSlab = hand.is(BlockTags.SLAB);
        boolean targetReplaceable = canReplace(target, hand, player, face, clickPos);
        boolean sideReplaceable = canReplace(side, hand, player, face, clickPos);

        Block block = isSlab
                ? resolveSlabTarget(hand, target, side, face, clickPos, targetReplaceable, sideReplaceable)
                : resolveNormalTarget(target, side, targetReplaceable, sideReplaceable);
        if (block == null) {
            return null;
        }

        ComponentMap handBehaviors = this.blockRegistry.requireComponents(hand.getType());
        hand = handBehaviors.require(BlockComponents.RESOLVE_PLACEMENT_STATE)
                .execute(hand, block, player, face, clickPos);
        Vector3i blockPos = block.getPosition();
        Block prospectiveBlock = new CloudBlock(this, blockPos, new BlockState[]{hand, block.getExtra()});
        if (!handBehaviors.require(BlockComponents.CAN_SURVIVE).execute(prospectiveBlock)) {
            return null;
        }

        VoxelShape handShape = handBehaviors.require(BlockComponents.GET_COLLISION_SHAPE)
                .execute(hand, BlockShapeContext.at(this, blockPos), CollisionContext.of(player))
                .move(blockPos.getX(), blockPos.getY(), blockPos.getZ());

        if (!handShape.isEmpty()) {
            BoundingBox handBB = handShape.bounds();

            Set<Entity> entities = this.getCollidingEntities(player instanceof CloudPlayer cp ? cp : null, handBB);
            int realCount = 0;
            for (Entity e : entities) {
                if (e instanceof EntityArrow || e instanceof DroppedItem || (e instanceof CloudPlayer && ((CloudPlayer) e).isSpectator())) {
                    continue;
                }
                ++realCount;
            }

            if (player != null) {
                BoundingBox shrunkPlayer = player.getBoundingBox().deflate(1e-4f, 1e-4f, 1e-4f);
                if (handShape.overlaps(shrunkPlayer)) {
                    ++realCount;
                }

                Vector3f diff = ((CloudPlayer) player).getNextPosition().sub(player.getPosition());
                if (diff.lengthSquared() > 0.00001) {
                    BoundingBox movedPlayer = shrunkPlayer.move(diff.getX(), diff.getY(), diff.getZ());
                    if (handShape.overlaps(movedPlayer)) {
                        ++realCount;
                    }
                }
            }

            if (realCount > 0) {
                return null;
            }
        }

        if (player != null) {
            boolean canBuild = (player.getGameMode() != GameMode.ADVENTURE
                    || itemRegistry.requireComponent(item.getType(), ItemComponents.CAN_BE_PLACED_ON)
                    .execute(item, target))
                    && (player.isOp() || !isInSpawnRadius(block.getPosition()));
            BlockPlaceEvent event = new BlockPlaceEvent(prospectiveBlock, block.getState(), target, item, player,
                    canBuild, EquipmentSlot.MAIN_HAND);

            this.server.getEventManager().fire(event);
            if (event.isCancelled() || !event.canBuild()) {
                return null;
            }
        }

        if (!handBehaviors.require(BlockComponents.ON_PLACE).execute(hand, player, block.getPosition(), face, clickPos)) {
            return null;
        }

        if (player != null && !player.isCreative()) {
            item = item.decreaseCount();
        }

        if (playSound) {
            this.addLevelSoundEvent(block.getPosition(), SoundEvent.PLACE, CloudBlockRegistry.REGISTRY.getRuntimeId(hand));
        }

        return item.getCount() <= 0 ? ItemStack.EMPTY : item;
    }

    private static boolean canContainLiquid(BlockState container, BlockState liquid) {
        if (container == BlockStates.AIR || container.getType().isLiquid()) {
            return false;
        }

        LiquidState state = LiquidState.of(liquid);
        return LiquidBlockHandlers.canOccupySecondaryLayer(state)
                && (state.isSource() ? container.canContainLiquidSource() : container.canContainFlowingLiquid());
    }

    private @Nullable Block resolveSlabTarget(BlockState hand, Block target, Block side, Direction face,
                                              Vector3f clickPos, boolean targetReplaceable, boolean sideReplaceable) {
        BlockState targetState = target.getState();
        if (targetState.is(BlockTags.SLAB) && !targetState.is(BlockTags.DOUBLE_SLAB) && targetState.getType() == hand.getType()) {
            boolean above = clickPos.getY() > 0.5f;
            SlabSlot existing = targetState.ensureTrait(BlockTraits.SLAB_SLOT);
            boolean canMerge = face == Direction.UP ? existing == SlabSlot.BOTTOM
                    : face == Direction.DOWN ? existing == SlabSlot.TOP
                      : (existing == SlabSlot.BOTTOM) == above;
            if (canMerge) {
                return target;
            }
            return sideReplaceable ? side : null;
        }

        if (side.getState().is(BlockTags.SLAB) && !side.getState().is(BlockTags.DOUBLE_SLAB) && side.getState().getType() == hand.getType()) {
            return side;
        }

        return resolveNormalTarget(target, side, targetReplaceable, sideReplaceable);
    }

    private static @Nullable Block resolveNormalTarget(Block target, Block side, boolean targetReplaceable, boolean sideReplaceable) {
        if (targetReplaceable) {
            return target;
        }

        return sideReplaceable ? side : null;
    }

    private boolean canReplace(Block block, BlockState replacement, @Nullable Player player, Direction face, Vector3f clickPosition) {
        return this.blockRegistry.requireComponent(block.getState().getType(), BlockComponents.CAN_BE_REPLACED)
                .execute(block, replacement, player, face, clickPosition);
    }

    private static final class NeighborUpdate {
        private static final Direction[] DIRECTIONS = Direction.values();

        private final CloudLevel level;
        private final Vector3i position;
        private final Block changed;
        private int directionIndex;

        private NeighborUpdate(CloudLevel level, Vector3i position, Block changed) {
            this.level = level;
            this.position = position;
            this.changed = changed;
        }

        private boolean runNext() {
            Direction direction = DIRECTIONS[this.directionIndex++];
            this.level.updateNeighbour(this.level.getBlock(direction.relative(this.position)), this.changed);
            return this.directionIndex < DIRECTIONS.length;
        }
    }

    private static final class NeighborUpdateContext {
        private final Deque<NeighborUpdate> stack = new ArrayDeque<>();
        private final List<NeighborUpdate> added = new ArrayList<>();
        private int count;
        private boolean processing;
    }

    public boolean isInSpawnRadius(Vector3i vector3) {
        int distance = this.server.getSpawnRadius();
        if (distance > -1) {
            Vector2i t = vector3.toVector2(true);
            Vector2i s = this.getSpawnLocation().toInt().toVector2(true);
            return t.distance(s) <= distance;
        }
        return false;
    }

    public Entity getEntity(long entityId) {
        return this.entities.get(entityId);
    }

    public Entity[] getEntities() {
        return entities.values().toArray(new Entity[0]);
    }

    public Set<Entity> getCollidingEntities(BoundingBox boundingBox) {
        return this.getCollidingEntities(null, boundingBox);
    }

    public Set<Entity> getCollidingEntities(@Nullable Entity except, BoundingBox boundingBox) {
        ImmutableSet.Builder<Entity> entities = null;

        if (except == null || except.canCollide()) {
            int minX = GenericMath.floor((boundingBox.getMinX() - 2) / 16);
            int maxX = GenericMath.ceil((boundingBox.getMaxX() + 2) / 16);
            int minZ = GenericMath.floor((boundingBox.getMinZ() - 2) / 16);
            int maxZ = GenericMath.ceil((boundingBox.getMaxZ() + 2) / 16);

            for (int x = minX; x <= maxX; ++x) {
                for (int z = minZ; z <= maxZ; ++z) {
                    Set<CloudEntity> colliding = this.getLoadedChunkEntities(x, z);
                    for (CloudEntity ent : colliding) {
                        if ((except == null || (ent != except && except.canCollideWith(ent)))
                                && ent.getBoundingBox().intersects(boundingBox)) {
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

    @Override
    public Set<Entity> getNearbyEntities(BoundingBox boundingBox) {
        return this.getNearbyEntities(boundingBox, null);
    }

    @Override
    public Set<Entity> getNearbyEntities(BoundingBox boundingBox, @Nullable Predicate<? super Entity> filter) {
        return this.getNearbyEntities(boundingBox, filter, false);
    }

    public Set<Entity> getNearbyEntities(@Nullable Entity except, BoundingBox boundingBox) {
        return this.getNearbyEntities(except, boundingBox, false);
    }

    public Set<Entity> getNearbyEntities(@Nullable Entity except, BoundingBox boundingBox, boolean loadChunks) {
        return this.getNearbyEntities(boundingBox, candidate -> candidate != except, loadChunks);
    }

    private Set<Entity> getNearbyEntities(BoundingBox boundingBox, @Nullable Predicate<? super Entity> filter, boolean loadChunks) {
        int minX = GenericMath.floor((boundingBox.getMinX() - 2) * 0.0625);
        int maxX = GenericMath.ceil((boundingBox.getMaxX() + 2) * 0.0625);
        int minZ = GenericMath.floor((boundingBox.getMinZ() - 2) * 0.0625);
        int maxZ = GenericMath.ceil((boundingBox.getMaxZ() + 2) * 0.0625);

        ImmutableSet.Builder<Entity> entities = null;

        for (int x = minX; x <= maxX; ++x) {
            for (int z = minZ; z <= maxZ; ++z) {
                Set<CloudEntity> entitiesInRange = loadChunks ? this.getChunkEntities(x, z) : this.getLoadedChunkEntities(x, z);
                for (CloudEntity entityInRange : entitiesInRange) {
                    if (entityInRange.getBoundingBox().intersects(boundingBox) && (filter == null || filter.test(entityInRange))) {
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

    @Override
    public int getMinHeight() {
        // TODO: Support custom world heights.
        return this.getDimension() == DIMENSION_OVERWORLD ? -64 : 0;
    }

    @Override
    public int getMaxHeight() {
        if (this.getDimension() == DIMENSION_NETHER) return 128;
        if (this.getDimension() == DIMENSION_THE_END) return 256;
        return 320; // Overworld
    }

    public Set<BlockEntity> getBlockEntities() {
        return blockEntities;
    }

    @Override
    public Map<Long, CloudPlayer> getPlayers() {
        return players;
    }

    public Map<Integer, ChunkLoader> getLoaders() {
        return loaders;
    }


    public BlockEntity getBlockEntity(Vector3i pos) {
        Chunk chunk = this.getChunk(pos);
        return chunk.getBlockEntity(pos.getX() & 0x0f, pos.getY(), pos.getZ() & 0x0f);
    }

    @Nullable
    public BlockEntity getLoadedBlockEntity(Vector3i pos) {
        Chunk chunk = this.getLoadedChunk(pos);
        return chunk == null ? null : chunk.getBlockEntity(pos.getX() & 0x0f, pos.getY(), pos.getZ() & 0x0f);
    }

    @NonNull
    public Set<CloudEntity> getChunkEntities(int chunkX, int chunkZ) {
        return this.getChunk(chunkX, chunkZ).getEntities();
    }

    @NonNull
    public Set<CloudEntity> getLoadedChunkEntities(int chunkX, int chunkZ) {
        CloudChunk chunk = this.getLoadedChunk(chunkX, chunkZ);
        if (chunk != null) {
            return ImmutableSet.<CloudEntity>builder()
                    .addAll(chunk.getEntities())
                    .addAll(chunk.getPlayers())
                    .build();
        }
        return Collections.emptySet();
    }


    @NonNull
    public Collection<BaseBlockEntity> getChunkBlockEntities(int chunkX, int chunkZ) {
        return this.getChunk(chunkX, chunkZ).getBlockEntities();
    }

    @NonNull
    public Collection<BaseBlockEntity> getLoadedBlockEntities(int chunkX, int chunkZ) {
        CloudChunk chunk = this.getLoadedChunk(chunkX, chunkZ);
        return chunk == null ? Collections.emptyList() : chunk.getBlockEntities();
    }

    @Override
    public BlockState getBlockState(int x, int y, int z, int layer) {
        Chunk chunk = this.getChunk(x >> 4, z >> 4);
        return chunk.getBlock(x & 0x0f, y, z & 0x0f, layer);
    }

    public int getBiomeId(int x, int y, int z) {
        return this.getChunk(x >> 4, z >> 4).getBiome(x & 0xF, y, z & 0xF);
    }

    public void setBiomeId(int x, int y, int z, int biomeId) {
        this.getChunk(x >> 4, z >> 4).setBiome(x & 0xF, y, z & 0xF, biomeId);
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

    @Override
    public CloudChunk getLoadedChunk(long chunkKey) {
        return this.chunkManager.getLoadedChunk(chunkKey);
    }

    @Override
    public CloudChunk getLoadedChunk(int chunkX, int chunkZ) {
        return this.chunkManager.getLoadedChunk(chunkX, chunkZ);
    }

    @Override
    public CloudChunk getLoadedChunk(Vector3i pos) {
        return this.getLoadedChunk(pos.getX(), pos.getZ());
    }

    @Override
    public CloudChunk getLoadedChunk(Vector3f pos) {
        return this.getLoadedChunk(pos.toInt());
    }

    @NonNull
    public Set<CloudChunk> getChunks() {
        return this.chunkManager.getLoadedChunks();
    }

    public int getChunkCount() {
        return this.chunkManager.getLoadedCount();
    }

    @Override
    public CloudChunk getChunk(Vector3i pos) {
        return getChunk(pos.getX() >> 4, pos.getZ() >> 4);
    }

    @Override
    public CloudChunk getChunk(Vector3f pos) {
        return getChunk(pos.getFloorX() >> 4, pos.getFloorZ() >> 4);
    }

    @Override
    public CloudChunk getChunk(long chunkKey) {
        return getChunk(CloudChunk.fromKeyX(chunkKey), CloudChunk.fromKeyZ(chunkKey));
    }

    @Override
    public CloudChunk getChunk(int chunkX, int chunkZ) {
        return this.chunkManager.getChunk(chunkX, chunkZ);
    }

    @Override
    public CompletableFuture<CloudChunk> getChunkFuture(int chunkX, int chunkZ) {
        return this.chunkManager.getChunkFuture(chunkX, chunkZ);
    }

    public void addPlayerViewChunkTicket(long chunkKey, Object identifier) {
        this.chunkManager.addPlayerViewTicket(chunkKey, identifier);
    }

    public void removePlayerViewChunkTicket(long chunkKey, Object identifier) {
        this.chunkManager.removePlayerViewTicket(chunkKey, identifier);
    }

    public int getHighestBlockAt(int x, int z) {
        return this.getChunk(x >> 4, z >> 4).getHighestBlock(x & 0x0f, z & 0x0f);
    }

    public Color getMapColorAt(int x, int z) {
        Chunk chunk = this.getChunk(x >> 4, z >> 4);
        int y = chunk.getHighestBlock(x & 0x0f, z & 0x0f);
        while (y > 1) {
            Block block = getBlock(Vector3i.from(x, y, z));
            Color mapColor = block.requireComponent(BlockComponents.GET_MAP_COLOR).execute(block);
            if (mapColor.getAlpha() == 0x00) {
                y--;
            } else {
                return mapColor;
            }
        }
        return Color.BLACK;
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

    public Vector3f getSpawnLocation() {
        return this.levelData.getSpawn().toFloat().add(0.5f, 0f, 0.5f);
    }

    public void setSpawnLocation(Vector3f pos) {
        Vector3f previousSpawn = this.getSpawnLocation();
        Vector3i blockPos = pos.toInt();
        this.levelData.setSpawn(blockPos);
        this.server.getEventManager().fire(new SpawnChangeEvent(this, previousSpawn));

        SetSpawnPositionPacket packet = new SetSpawnPositionPacket();
        packet.setSpawnType(SetSpawnPositionPacket.Type.WORLD_SPAWN);
        packet.setBlockPosition(blockPos);
        CloudServer.broadcastPacket(this.players.values().toArray(new CloudPlayer[0]), packet);
    }

    public void scheduleEntityUpdate(Entity entity) {
        checkNotNull(entity, "entity");
        this.updateEntities.add(entity);
    }

    public void removeEntity(Entity entity) {
        if (entity.getLevel() != this) {
            throw new LevelException("Invalid Entity level");
        }

        if (entity instanceof CloudPlayer) {
            this.players.remove(entity.getUniqueId());
            this.checkSleep();
        } else {
            entity.close();
        }

        this.entities.remove(entity.getUniqueId());
        this.updateEntities.remove(entity);
    }

    public void addEntity(Entity entity) {
        if (entity.getLevel() != this) {
            throw new LevelException("Invalid Entity level");
        }

        if (entity instanceof CloudPlayer) {
            this.players.put(entity.getUniqueId(), (CloudPlayer) entity);
        }
        this.entities.put(entity.getUniqueId(), entity);
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

    public Location getSafeSpawn() {
        return this.getSafeSpawn(null);
    }

    public Location getSafeSpawn(Location pos) {
        if (pos == null) {
            pos = Location.from(this.getSpawnLocation(), this);
        }

        Vector3f v = pos.getPosition();
        int originX = v.getFloorX();
        int originZ = v.getFloorZ();

        int spawnRadius = Math.max(0, this.getGameRules().get(GameRules.SPAWN_RADIUS));

        // Search in expanding rings from the origin outward so the closest dry-land
        // column is preferred. This guarantees every column within the radius is
        // checked exactly once.
        for (int r = 0; r <= spawnRadius; r++) {
            if (r == 0) {
                Integer safeY = getOverworldRespawnY(originX, originZ);
                if (safeY != null) {
                    return Location.from(originX + 0.5f, safeY, originZ + 0.5f, pos.getYaw(), pos.getPitch(), this);
                }
            } else {
                // Walk the perimeter of the ring at distance r.
                for (int dx = -r; dx <= r; dx++) {
                    for (int dz = -r; dz <= r; dz++) {
                        if (Math.abs(dx) != r && Math.abs(dz) != r) {
                            continue; // only the perimeter
                        }

                        int cx = originX + dx;
                        int cz = originZ + dz;
                        Integer safeY = getOverworldRespawnY(cx, cz);
                        if (safeY != null) {
                            return Location.from(cx + 0.5f, safeY, cz + 0.5f, pos.getYaw(), pos.getPitch(), this);
                        }
                    }
                }
            }
        }

        // Fallback: entire search area is liquid. Place on top of the water surface.
        int fallbackY = fixupSpawnHeight(originX, v.getFloorY(), originZ);
        return Location.from(originX + 0.5f, fallbackY, originZ + 0.5f, pos.getYaw(), pos.getPitch(), this);
    }

    /**
     * Returns the Y coordinate the player should stand at in column (x,z), or
     * {@code null} if the column is unsuitable (over liquid, no collision floor found).
     */
    private Integer getOverworldRespawnY(int x, int z) {
        Chunk chunk = this.getChunk(x >> 4, z >> 4);
        if (chunk == null) {
            return null;
        }

        int lx = x & 0x0f;
        int lz = z & 0x0f;

        int motionBlockingY = chunk.getHighestBlock(lx, lz);
        if (motionBlockingY < 0) {
            return null;
        }

        BlockState topBlock = chunk.getBlock(lx, motionBlockingY, lz);
        if (topBlock.is(BlockTags.LIQUID)) {
            return null;
        }

        for (int y = motionBlockingY; y >= getMinHeight(); y--) {
            BlockState state = chunk.getBlock(lx, y, lz);
            if (state.is(BlockTags.LIQUID)) {
                break;
            }

            Vector3i position = Vector3i.from(x, y, z);
            boolean collisionFloor = this.hasBlockCollision(null, state, position, BoundingBox.unit(position));

            if (collisionFloor) {
                int standY = y + 1;
                if (standY + 1 <= 255) {
                    BlockState feet = chunk.getBlock(lx, standY, lz);
                    BlockState head = chunk.getBlock(lx, standY + 1, lz);
                    boolean feetClear = this.isSpawnSpaceClear(feet, x, standY, z);
                    boolean headClear = this.isSpawnSpaceClear(head, x, standY + 1, z);
                    if (feetClear && headClear) {
                        return standY;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Scans up until a clear 2-block gap is found, then walks back down to the floor.
     * Used as a last-resort fallback when the entire search area is liquid.
     */
    private int fixupSpawnHeight(int x, int startY, int z) {
        Chunk chunk = this.getChunk(x >> 4, z >> 4);
        if (chunk == null) {
            return startY;
        }

        int lx = x & 0x0f;
        int lz = z & 0x0f;
        int y = GenericMath.clamp(startY, getMinHeight(), 254);

        while (y < 254) {
            BlockState feet = chunk.getBlock(lx, y, lz);
            BlockState head = chunk.getBlock(lx, y + 1, lz);
            boolean feetClear = this.isSpawnSpaceClear(feet, x, y, z);
            boolean headClear = this.isSpawnSpaceClear(head, x, y + 1, z);
            if (feetClear && headClear) {
                break;
            }
            y++;
        }

        while (y > getMinHeight()) {
            BlockState feet = chunk.getBlock(lx, y - 1, lz);
            BlockState head = chunk.getBlock(lx, y, lz);
            boolean feetClear = this.isSpawnSpaceClear(feet, x, y - 1, z);
            boolean headClear = this.isSpawnSpaceClear(head, x, y, z);
            if (!feetClear || !headClear) {
                break;
            }
            y--;
        }

        return y;
    }

    private boolean isSpawnSpaceClear(BlockState state, int x, int y, int z) {
        Vector3i position = Vector3i.from(x, y, z);
        return !state.is(BlockTags.LIQUID)
                && !this.hasBlockCollision(null, state, position, BoundingBox.unit(position));
    }

    public int getTime() {
        return (int) this.levelData.getTime();
    }

    public void setTime(int time) {
        this.levelData.setTime(time);
        this.sendTime();
    }

    public boolean isDaytime() {
        return this.skyLightSubtracted < 4;
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

/*    @Override
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
    public void removeMetadata(String metadataKey, PluginContainer owningPlugin) throws Exception {
        this.server.getLevelMetadata().removeMetadata(this, metadataKey, owningPlugin);
    }*/

    public void addEntityMovement(Entity entity, double x, double y, double z, double yaw, double pitch, double headYaw) {
        MoveEntityAbsolutePacket packet = new MoveEntityAbsolutePacket();
        packet.setRuntimeEntityId(entity.getRuntimeId());
        packet.setPosition(Vector3f.from(x, y, z));
        packet.setRotation(Vector3f.from(pitch, yaw, headYaw));

        CloudServer.broadcastPacket(((CloudEntity) entity).getViewers(), packet);
    }

    public boolean isRaining() {
        return this.levelData.getRainLevel() > 0;
    }

    public boolean setRaining(boolean raining) {
        WeatherChangeEvent ev = new WeatherChangeEvent(this, raining);
        this.getServer().getEventManager().fire(ev);

        if (ev.isCancelled()) {
            return false;
        }

        this.levelData.setRainLevel(raining ? 1 : 0);

        LevelEventPacket packet = new LevelEventPacket();
        // These numbers are from Minecraft

        if (raining) {
            packet.setType(LevelEvent.START_RAINING);
            packet.setData(this.random.nextInt(50000) + 10000);
            setRainTime(this.random.nextInt(12000) + 12000);
        } else {
            packet.setType(LevelEvent.STOP_RAINING);
            setRainTime(this.random.nextInt(168000) + 12000);
        }
        packet.setPosition(Vector3f.ZERO);

        CloudServer.broadcastPacket(this.getPlayers().values().toArray(new CloudPlayer[0]), packet);

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
        this.getServer().getEventManager().fire(ev);

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
            packet.setType(LevelEvent.START_THUNDERSTORM);
            packet.setData(this.random.nextInt(50000) + 10000);
            setThunderTime(this.random.nextInt(12000) + 3600);
        } else {
            packet.setType(LevelEvent.STOP_THUNDERSTORM);
            setThunderTime(this.random.nextInt(168000) + 12000);
        }
        packet.setPosition(Vector3f.ZERO);

        CloudServer.broadcastPacket(this.getPlayers().values().toArray(new CloudPlayer[0]), packet);

        return true;
    }

    public int getThunderTime() {
        return this.levelData.getLightningTime();
    }

    public void setThunderTime(int thunderTime) {
        this.levelData.setLightningTime(thunderTime);
    }

    public void sendWeather(Player[] toSend) {
        CloudPlayer[] players;
        if (toSend == null) {
            players = this.getPlayers().values().toArray(new CloudPlayer[0]);
        } else {
            players = Arrays.asList(toSend).toArray(new CloudPlayer[0]);
        }

        LevelEventPacket rainEvent = new LevelEventPacket();
        if (this.isRaining()) {
            rainEvent.setType(LevelEvent.START_RAINING);
            rainEvent.setData(this.random.nextInt(50000) + 10000);
        } else {
            rainEvent.setType(LevelEvent.STOP_RAINING);
        }

        rainEvent.setPosition(Vector3f.ZERO);
        CloudServer.broadcastPacket(players, rainEvent);

        LevelEventPacket thunderEvent = new LevelEventPacket();
        if (this.isThundering()) {
            thunderEvent.setType(LevelEvent.START_THUNDERSTORM);
            thunderEvent.setData(this.random.nextInt(50000) + 10000);
        } else {
            thunderEvent.setType(LevelEvent.STOP_THUNDERSTORM);
        }
        thunderEvent.setPosition(Vector3f.ZERO);
        CloudServer.broadcastPacket(players, thunderEvent);
    }

    public void sendWeather(Player player) {
        if (player != null) {
            this.sendWeather(new Player[]{player});
        }
    }

    public void sendWeather(Collection<Player> players) {
        if (players == null) {
            this.sendWeather(this.getPlayers().values().toArray(new CloudPlayer[0]));
        }
        this.sendWeather(players.toArray(new Player[0]));
    }

    public int getDimension() {
        return this.levelData.getDimension();
    }

    public void setDimension(int dimension) {
        this.levelData.setDimension(dimension);
    }

    public int getDifficulty() {
        return this.levelData.getDifficulty();
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


    public boolean isAreaLoaded(BoundingBox boundingBox) {
        if (boundingBox.getMaxY() < -64 || boundingBox.getMinY() >= 320) {
            return false;
        }
        int minX = GenericMath.floor(boundingBox.getMinX()) >> 4;
        int minZ = GenericMath.floor(boundingBox.getMinZ()) >> 4;
        int maxX = GenericMath.floor(boundingBox.getMaxX()) >> 4;
        int maxZ = GenericMath.floor(boundingBox.getMaxZ()) >> 4;

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
}
