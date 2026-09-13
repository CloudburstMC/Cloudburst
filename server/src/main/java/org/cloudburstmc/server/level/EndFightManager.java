package org.cloudburstmc.server.level;

import net.kyori.adventure.text.Component;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.boss.*;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.hostile.EnderDragon;
import org.cloudburstmc.api.entity.misc.EnderCrystal;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.LevelEvent;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.protocol.bedrock.packet.LevelEventPacket;
import org.cloudburstmc.server.boss.CloudEntityBossBar;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.entity.hostile.EntityEnderDragon;
import org.cloudburstmc.server.entity.misc.EntityEnderCrystal;
import org.cloudburstmc.server.level.feature.EndGatewayFeature;
import org.cloudburstmc.server.level.feature.EndPodiumFeature;
import org.cloudburstmc.server.level.generator.standard.population.EndSpikeLayout;
import org.cloudburstmc.server.level.generator.standard.population.EndSpikePopulator;
import org.cloudburstmc.server.registry.CloudEntityRegistry;

import java.util.*;

public class EndFightManager implements DragonBattle {

    private static final int GATEWAY_COUNT = 20;
    private static final int GATEWAY_RADIUS = 96;
    private static final float ARENA_PLAYER_RANGE = 192;
    private static final int INITIAL_RECONCILIATION_DELAY = 20;
    private static final int MISSING_DRAGON_RESPAWN_DELAY = 1200;
    private static final int RESPAWN_PREPARATION_TICKS = 100;
    private static final int PILLAR_RESPAWN_TICKS = 40;
    private static final int DRAGON_SUMMON_TICKS = 100;

    private final CloudLevel level;
    private final CloudEndFightData data;
    private final CloudEntityBossBar bossBar = new CloudEntityBossBar(Component.translatable("entity.ender_dragon.name"), BossBarColor.PURPLE, BossBarStyle.SOLID);
    private final List<EntityEnderCrystal> respawnCrystals = new ArrayList<>();
    private EndDragonRespawnStage respawnStage;
    private boolean started;
    private boolean spawnDragonAfterInitialization;
    private int initializationTime;
    private int missingDragonTime;
    private int reconciliationTime;
    private int respawnTime;

    public EndFightManager(CloudLevel level, CloudEndFightData data) {
        this.level = level;
        this.data = data;
    }

    public void ensureStarted() {
        if (!this.started) {
            this.started = true;
            this.initializationTime = INITIAL_RECONCILIATION_DELAY;
        }

        loadCentralChunks();

        if (!this.data.isInitialized()) {
            this.data.setInitialized(true);
            this.data.setDragonKilled(false);
            this.spawnDragonAfterInitialization = true;
            EndPodiumFeature.place(this.level, exitPortalPosition(), false);
        }
    }

    public void tick() {
        if (!hasArenaPlayer()) {
            return;
        }

        if (this.initializationTime > 0) {
            if (--this.initializationTime == 0) {
                reconcileDragons(this.spawnDragonAfterInitialization);
                this.spawnDragonAfterInitialization = false;
                if (this.respawnStage == null && !restoreRespawn()) {
                    this.initiateRespawn();
                }
            }

            return;
        }

        if (this.respawnStage == null) {
            tickMissingDragon();
            if (this.started && ++this.reconciliationTime >= 20) {
                this.reconciliationTime = 0;
                reconcileDragons(false);
            }
            return;
        }

        if (this.respawnCrystals.stream().anyMatch(CloudEntity::isClosed)) {
            abortRespawn();
            return;
        }

        switch (this.respawnStage) {
            case START -> tickStart();
            case PREPARING_TO_SUMMON_PILLARS -> tickPreparing();
            case SUMMONING_PILLARS -> tickPillars();
            case SUMMONING_DRAGON -> tickDragonSummoning();
            case END -> tickRespawnEnd();
        }

        saveRespawnState();
    }

    @Override
    public void initiateRespawn() {
        if (isRespawnUnavailable()) {
            return;
        }

        List<EntityEnderCrystal> crystals = findRespawnCrystals();
        if (!crystals.isEmpty()) {
            beginRespawn(crystals);
        }
    }

    @Override
    public boolean initiateRespawn(Collection<? extends EnderCrystal> suppliedCrystals) {
        Objects.requireNonNull(suppliedCrystals, "suppliedCrystals");
        if (isRespawnUnavailable()) {
            return false;
        }

        List<EntityEnderCrystal> crystals = resolveRespawnCrystals(suppliedCrystals);
        if (crystals.isEmpty()) {
            return false;
        }

        beginRespawn(crystals);
        return true;
    }

    public void onDragonKilled(CloudEntity dragon) {
        if (this.data.isDragonKilled() || dragon.getType() != EntityTypes.ENDER_DRAGON) {
            return;
        }

        Vector3i portal = exitPortalPosition();
        EndPodiumFeature.place(this.level, portal, true);
        if (!this.data.isDragonPreviouslyKilled()) {
            this.level.setBlockState(portal.add(0, 4, 0), BlockStates.DRAGON_EGG);
        }

        spawnGateway();
        this.data.setDragonPreviouslyKilled(true);
        this.data.setDragonKilled(true);
        for (EntityEnderDragon other : findDragons()) {
            if (other != dragon) {
                removeDragon(other);
            }
        }
    }

    public int getDragonExperienceReward() {
        return this.data.isDragonPreviouslyKilled() ? 500 : 12000;
    }

    @Override
    public EnderDragon getEnderDragon() {
        List<EntityEnderDragon> dragons = findDragons();
        return dragons.isEmpty() ? null : dragons.getFirst();
    }

    @Override
    public BossBar getBossBar() {
        return this.bossBar;
    }

    @Override
    public Location getEndPortalLocation() {
        Integer y = this.data.getExitPortalY();
        return y == null ? null : Location.from(0, y, 0, this.level);
    }

    @Override
    public boolean generateEndPortal(boolean active) {
        Vector3i portal = exitPortalPosition();
        if (this.level.getBlockState(portal.add(2, 0, 0)) == BlockStates.BEDROCK) {
            return false;
        }

        EndPodiumFeature.place(this.level, portal, active);
        return true;
    }

    @Override
    public boolean hasBeenPreviouslyKilled() {
        return this.data.isDragonPreviouslyKilled();
    }

    @Override
    public void setPreviouslyKilled(boolean previouslyKilled) {
        this.data.setDragonPreviouslyKilled(previouslyKilled);
    }

    @Override
    public DragonRespawnPhase getRespawnPhase() {
        return this.respawnStage == null ? DragonRespawnPhase.NONE : DragonRespawnPhase.valueOf(this.respawnStage.name());
    }

    @Override
    public boolean setRespawnPhase(DragonRespawnPhase phase) {
        Objects.requireNonNull(phase, "phase");
        if (this.respawnStage == null || phase == DragonRespawnPhase.NONE) {
            return false;
        }

        this.respawnStage = EndDragonRespawnStage.valueOf(phase.name());
        this.respawnTime = 0;
        saveRespawnState();
        return true;
    }

    @Override
    public void resetCrystals() {
        resetSpikeCrystals();
    }

    @Override
    public int getGatewayCount() {
        return this.data.getGatewayIndex();
    }

    public static boolean isCentralGateway(Vector3i position) {
        Objects.requireNonNull(position, "position");
        for (int gateway = 0; gateway < GATEWAY_COUNT; gateway++) {
            if (gatewayPosition(gateway).equals(position)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean spawnNewGateway() {
        return spawnGateway();
    }

    @Override
    public void spawnNewGateway(Vector3i position) {
        Objects.requireNonNull(position, "position");
        playGatewaySpawnEffect(position);
        EndGatewayFeature.place(this.level, position);
    }

    @Override
    public List<EnderCrystal> getRespawnCrystals() {
        return List.copyOf(this.respawnCrystals);
    }

    @Override
    public List<EnderCrystal> getHealingCrystals() {
        List<EnderCrystal> crystals = new ArrayList<>();
        List<EndSpikeLayout.Spike> spikes = EndSpikeLayout.create(this.level.getSeed());
        for (Entity entity : this.level.getEntities()) {
            if (!(entity instanceof EntityEnderCrystal crystal)) {
                continue;
            }

            for (EndSpikeLayout.Spike spike : spikes) {
                if (Math.abs(crystal.getX() - spike.x()) <= spike.radius() + 1
                        && Math.abs(crystal.getY() - spike.height()) <= 2
                        && Math.abs(crystal.getZ() - spike.z()) <= spike.radius() + 1) {
                    crystals.add(crystal);
                    break;
                }
            }
        }

        return List.copyOf(crystals);
    }

    private EnderDragon spawnDragon() {
        EnderDragon dragon = CloudEntityRegistry.get().newEntity(EntityTypes.ENDER_DRAGON, Location.from(Vector3f.from(0, 128, 0), this.level));
        dragon.spawnToAll();
        this.bossBar.bindEntity(dragon);
        return dragon;
    }

    private boolean hasArenaPlayer() {
        for (Player player : this.level.getPlayers().values()) {
            if (player.getPosition().distanceSquared(Vector3f.from(0.5f, 128, 0.5f))
                    <= ARENA_PLAYER_RANGE * ARENA_PLAYER_RANGE) {
                return true;
            }
        }

        return false;
    }

    private List<EntityEnderDragon> findDragons() {
        List<EntityEnderDragon> dragons = new ArrayList<>();
        for (Entity entity : this.level.getEntities()) {
            if (entity instanceof EntityEnderDragon dragon && dragon.isAlive()) {
                dragons.add(dragon);
            }
        }

        return dragons;
    }

    private EntityEnderCrystal findCrystal(Vector3i expected) {
        for (Entity entity : this.level.getEntities()) {
            if (entity instanceof EntityEnderCrystal crystal && crystal.getPosition().distanceSquared(expected.toFloat().add(0.5f, 0, 0.5f)) < 1) {
                return crystal;
            }
        }

        return null;
    }

    private List<EntityEnderCrystal> findRespawnCrystals() {
        Vector3i center = exitPortalPosition().add(0, 1, 0);
        List<EntityEnderCrystal> crystals = new ArrayList<>(4);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            Vector3i expected = center.add(direction.getStepX() * 3, 0, direction.getStepZ() * 3);
            EntityEnderCrystal crystal = findCrystal(expected);
            if (crystal == null) {
                return List.of();
            }
            crystals.add(crystal);
        }

        return crystals;
    }

    private boolean restoreRespawn() {
        EndDragonRespawnStage stage = this.data.getRespawnStage();
        if (!this.data.isDragonKilled() || stage == null) {
            return false;
        }

        List<EntityEnderCrystal> crystals = findRespawnCrystals();
        if (crystals.isEmpty()) {
            clearRespawnState();
            return false;
        }

        this.respawnStage = stage;
        this.respawnTime = this.data.getRespawnTime();
        this.respawnCrystals.addAll(crystals);
        return true;
    }

    private void tickStart() {
        Vector3i beamTarget = Vector3i.from(0, 128, 0);
        this.respawnCrystals.forEach(crystal -> crystal.setBeamTarget(beamTarget));
        this.respawnStage = EndDragonRespawnStage.PREPARING_TO_SUMMON_PILLARS;
        this.respawnTime = 0;
    }

    private void tickPreparing() {
        if (this.respawnTime == 0 || this.respawnTime == 50 || this.respawnTime == 51
                || this.respawnTime == 52 || this.respawnTime >= 95) {
            playRespawnGrowl();
        }

        if (++this.respawnTime >= RESPAWN_PREPARATION_TICKS) {
            this.respawnStage = EndDragonRespawnStage.SUMMONING_PILLARS;
            this.respawnTime = 0;
        }
    }

    private void tickPillars() {
        List<EndSpikeLayout.Spike> spikes = EndSpikeLayout.create(this.level.getSeed());
        int spikeIndex = this.respawnTime / PILLAR_RESPAWN_TICKS;
        int spikeTime = this.respawnTime % PILLAR_RESPAWN_TICKS;
        if (spikeIndex >= spikes.size()) {
            this.respawnStage = EndDragonRespawnStage.SUMMONING_DRAGON;
            this.respawnTime = 0;
            return;
        }

        EndSpikeLayout.Spike spike = spikes.get(spikeIndex);
        if (spikeTime == 0) {
            Vector3i beamTarget = Vector3i.from(spike.x(), spike.height() + 1, spike.z());
            this.respawnCrystals.forEach(crystal -> crystal.setBeamTarget(beamTarget));
        } else if (spikeTime == PILLAR_RESPAWN_TICKS - 1) {
            regenerateSpike(spike);
        }

        this.respawnTime++;
    }

    private void tickDragonSummoning() {
        if (this.respawnTime == 0) {
            Vector3i beamTarget = Vector3i.from(0, 128, 0);
            this.respawnCrystals.forEach(crystal -> crystal.setBeamTarget(beamTarget));
        } else if (this.respawnTime < 5 || this.respawnTime >= 80) {
            playRespawnGrowl();
        }

        if (++this.respawnTime < DRAGON_SUMMON_TICKS) {
            return;
        }

        this.respawnStage = EndDragonRespawnStage.END;
        this.respawnTime = 0;
    }

    private void tickRespawnEnd() {
        resetSpikeCrystals();
        this.respawnCrystals.forEach(this::destroyRespawnCrystal);
        this.respawnCrystals.clear();
        this.data.setDragonKilled(false);
        clearRespawnState();
        EnderDragon dragon = spawnDragon();
        this.level.addLevelSoundEvent(dragon.getPosition(), SoundEvent.SPAWN, -1, EntityTypes.ENDER_DRAGON, false, false);
    }

    private List<EntityEnderCrystal> resolveRespawnCrystals(Collection<? extends EnderCrystal> suppliedCrystals) {
        if (suppliedCrystals.isEmpty()) {
            return findRespawnCrystals();
        }

        List<EntityEnderCrystal> crystals = new ArrayList<>();
        for (EnderCrystal supplied : suppliedCrystals) {
            if (supplied instanceof EntityEnderCrystal crystal
                    && crystal.getLevel() == this.level
                    && !crystal.isClosed()
                    && !crystals.contains(crystal)) {
                crystals.add(crystal);
            }
        }

        return crystals;
    }

    private boolean isRespawnUnavailable() {
        return !this.data.isDragonKilled() || this.respawnStage != null;
    }

    private void beginRespawn(List<EntityEnderCrystal> crystals) {
        EndPodiumFeature.place(this.level, exitPortalPosition(), false);
        this.respawnCrystals.addAll(crystals);
        this.respawnStage = EndDragonRespawnStage.START;
        this.respawnTime = 0;
        saveRespawnState();
    }

    private void removeDragon(EntityEnderDragon dragon) {
        dragon.close();
    }

    private void destroyRespawnCrystal(EntityEnderCrystal crystal) {
        Explosion explosion = new Explosion(this.level, crystal.getPosition(), 6, crystal);
        explosion.setDestroysBlocks(false);
        crystal.close();
        if (explosion.explodeA()) {
            explosion.explodeB();
        }
    }

    private void regenerateSpike(EndSpikeLayout.Spike spike) {
        for (Entity entity : this.level.getEntities()) {
            if (entity instanceof EntityEnderCrystal crystal
                    && Math.abs(crystal.getPosition().getX() - spike.x()) <= 10
                    && Math.abs(crystal.getPosition().getY() - spike.height()) <= 10
                    && Math.abs(crystal.getPosition().getZ() - spike.z()) <= 10) {
                crystal.close();
            }
        }

        Explosion explosion = new Explosion(this.level, Vector3f.from(spike.x() + 0.5f, spike.height(), spike.z() + 0.5f), 5, null);
        if (explosion.explodeA()) {
            explosion.explodeB();
        }

        EndSpikePopulator.placeSpike(this.level, spike);
        EntityEnderCrystal crystal = EndDimension.spawnSpikeCrystal(this.level, spike);
        crystal.setInvulnerable(true);
        crystal.setBeamTarget(Vector3i.from(0, 128, 0));
    }

    private void resetSpikeCrystals() {
        List<EndSpikeLayout.Spike> spikes = EndSpikeLayout.create(this.level.getSeed());
        for (Entity entity : this.level.getEntities()) {
            if (!(entity instanceof EntityEnderCrystal crystal)) {
                continue;
            }

            for (EndSpikeLayout.Spike spike : spikes) {
                Vector3f position = crystal.getPosition();
                if (Math.abs(position.getX() - spike.x()) <= spike.radius() + 1
                        && Math.abs(position.getY() - spike.height()) <= 2
                        && Math.abs(position.getZ() - spike.z()) <= spike.radius() + 1) {
                    crystal.setInvulnerable(false);
                    crystal.setBeamTarget(null);
                    break;
                }
            }
        }
    }

    private void playRespawnGrowl() {
        this.level.addLevelSoundEvent(Vector3f.from(0, 128, 0), SoundEvent.MAD, -1, EntityTypes.ENDER_DRAGON, false, false);
    }

    private void abortRespawn() {
        this.respawnCrystals.stream()
                .filter(crystal -> !crystal.isClosed())
                .forEach(crystal -> crystal.setBeamTarget(null));
        resetSpikeCrystals();
        this.respawnCrystals.clear();
        clearRespawnState();
        EndPodiumFeature.place(this.level, exitPortalPosition(), true);
    }

    private void saveRespawnState() {
        if (this.respawnStage != null) {
            this.data.setRespawnStage(this.respawnStage);
            this.data.setRespawnTime(this.respawnTime);
        }
    }

    private void clearRespawnState() {
        this.respawnStage = null;
        this.respawnTime = 0;
        this.data.setRespawnStage(null);
        this.data.setRespawnTime(0);
    }

    private void tickMissingDragon() {
        if (this.data.isDragonKilled()) {
            this.missingDragonTime = 0;
            return;
        }

        if (!findDragons().isEmpty()) {
            this.missingDragonTime = 0;
            return;
        }

        if (++this.missingDragonTime >= MISSING_DRAGON_RESPAWN_DELAY) {
            this.missingDragonTime = 0;
            spawnDragon();
        }
    }

    private void reconcileDragons(boolean spawnMissing) {
        List<EntityEnderDragon> dragons = findDragons();
        if (this.data.isDragonKilled()) {
            dragons.forEach(this::removeDragon);
        } else if (dragons.isEmpty() && spawnMissing) {
            spawnDragon();
        } else {
            if (!dragons.isEmpty()) {
                this.bossBar.bindEntity(dragons.getFirst());
            }
            dragons.stream().skip(1).forEach(this::removeDragon);
        }
    }

    private boolean spawnGateway() {
        int index = this.data.getGatewayIndex();
        if (index >= GATEWAY_COUNT) {
            return false;
        }

        int gateway = gatewayOrder().get(GATEWAY_COUNT - index - 1);
        Vector3i position = gatewayPosition(gateway);

        this.level.getChunk(position.getX() >> 4, position.getZ() >> 4);
        playGatewaySpawnEffect(position);
        EndGatewayFeature.place(this.level, position);
        this.data.setGatewayIndex(index + 1);
        return true;
    }

    private void playGatewaySpawnEffect(Vector3i position) {
        LevelEventPacket particle = new LevelEventPacket();
        particle.setType(LevelEvent.PARTICLE_EXPLOSION);
        particle.setPosition(position.toFloat());
        particle.setData(0);
        this.level.addChunkPacket(position, particle);
        this.level.addLevelSoundEvent(position, SoundEvent.EXPLODE);
    }

    private List<Integer> gatewayOrder() {
        List<Integer> gateways = new ArrayList<>(GATEWAY_COUNT);
        for (int gateway = 0; gateway < GATEWAY_COUNT; gateway++) {
            gateways.add(gateway);
        }

        Collections.shuffle(gateways, new Random(this.level.getSeed()));
        return gateways;
    }

    private static Vector3i gatewayPosition(int gateway) {
        double angle = 2 * (-Math.PI + Math.PI / GATEWAY_COUNT * gateway);
        return Vector3i.from(
                (int) Math.floor(GATEWAY_RADIUS * Math.cos(angle)), 75,
                (int) Math.floor(GATEWAY_RADIUS * Math.sin(angle))
        );
    }

    private Vector3i exitPortalPosition() {
        Integer storedY = this.data.getExitPortalY();
        int y;
        if (storedY == null) {
            y = this.level.getHighestBlock(0, 0);
            while (y > 63 && this.level.getBlockState(0, y, 0).getType() == BlockStates.BEDROCK.getType()) {
                y--;
            }
            y = Math.max(this.level.getMinHeight() + 1, y);
            this.data.setExitPortalY(y);
        } else {
            y = storedY;
        }

        return Vector3i.from(0, y, 0);
    }

    private void loadCentralChunks() {
        for (int chunkX = -1; chunkX <= 1; chunkX++) {
            for (int chunkZ = -1; chunkZ <= 1; chunkZ++) {
                this.level.getChunk(chunkX, chunkZ);
            }
        }
    }
}
