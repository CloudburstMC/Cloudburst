package org.cloudburstmc.server.entity.hostile;

import net.kyori.adventure.text.Component;
import org.cloudburstmc.api.boss.BossBar;
import org.cloudburstmc.api.boss.BossBarColor;
import org.cloudburstmc.api.boss.BossBarStyle;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.hostile.EnderDragon;
import org.cloudburstmc.api.event.entity.EntityDeathEvent;
import org.cloudburstmc.api.event.entity.EntityRemoveCause;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.level.gamerule.GameRules;
import org.cloudburstmc.api.level.particle.ParticleEmitter;
import org.cloudburstmc.api.level.particle.ParticleEmitterTypes;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.protocol.bedrock.data.AttributeData;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityEventType;
import org.cloudburstmc.protocol.bedrock.packet.AddEntityPacket;
import org.cloudburstmc.protocol.bedrock.packet.EntityEventPacket;
import org.cloudburstmc.protocol.bedrock.packet.UpdateAttributesPacket;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.boss.CloudStandaloneBossBar;
import org.cloudburstmc.server.entity.data.CloudEnderDragonData;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.network.NetworkEntityAttribute;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes.STRUCTURAL_INTEGRITY;

public class EntityEnderDragon extends EntityHostile implements EnderDragon {

    private static final float BOSS_BAR_RANGE = 192;

    private static final int DEATH_DURATION = 200;
    private static final int EXPERIENCE_START_TICK = 150;
    private static final int EXPERIENCE_INTERVAL = 5;
    private static final int INITIAL_AMBIENT_SOUND_DELAY = 100;
    private static final int MIN_AMBIENT_SOUND_DELAY = 200;
    private static final int AMBIENT_SOUND_DELAY_RANGE = 200;

    private BossBar bossBar;
    private boolean ownsBossBar;
    private boolean dying;
    private int ambientSoundDelay;

    private final CloudEnderDragonData persistenceState;

    public EntityEnderDragon(EntityType<EnderDragon> type, Location location) {
        CloudEnderDragonData state = new CloudEnderDragonData();
        super(type, location, state);
        this.persistenceState = state;
    }

    @Override
    public float getWidth() {
        return 13f;
    }

    @Override
    public float getHeight() {
        return 4f;
    }

    @Override
    public float getGravity() {
        return 0;
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.noPhysics = true;
        this.setImmobile(true);
        this.setDefaultMaxHealth(200);
        this.setHealth(this.getMaxHealth());
        this.ownsBossBar = this.getLevel().getDimension() != CloudLevel.DIMENSION_THE_END;
        this.bossBar = this.ownsBossBar
                ? new CloudStandaloneBossBar(
                        Component.translatable("entity.ender_dragon.name"),
                        BossBarColor.PURPLE,
                        BossBarStyle.SOLID)
                : this.getLevel().getEndFight().getBossBar();
        this.bossBar.setProgress(1);
        this.ambientSoundDelay = INITIAL_AMBIENT_SOUND_DELAY;
    }

    @Override
    public void setHealth(float health) {
        if (this.dying && health > 0 && Float.isFinite(health)) {
            return;
        }

        if (health == 0 && this.isAlive() && !this.dying) {
            this.beginDeath();
            return;
        }

        super.setHealth(health);
        if (this.bossBar != null) {
            this.bossBar.setProgress(this.getHealth() / this.getMaxHealth());
        }

        this.broadcastHealth();
    }

    @Override
    protected void addAdditionalSpawnData(AddEntityPacket packet) {
        packet.getAttributes().add(createHealthAttribute());
    }

    @Override
    protected String getDefaultName() {
        return "EnderDragon";
    }

    @Override
    public BossBar getBossBar() {
        return this.bossBar;
    }

    @Override
    protected CloudEnderDragonData data() {
        return this.persistenceState;
    }

    @Override
    protected void loadCustom(NbtMap tag) {
        if (tag.containsKey("DragonDeathTime")) {
            this.data().setDragonDeathTime(getInt(tag, "DragonDeathTime", this.data().getDragonDeathTime()));
        }

        this.dying = this.data().getDragonDeathTime() > 0 || getFloat(tag, "Health", this.getHealth()) == 0f;
        if (tag.containsKey("DragonDeathExperience")) {
            this.data().setDragonDeathExperience(getInt(tag, "DragonDeathExperience", this.data().getDragonDeathExperience()));
        } else {
            this.data().setDragonDeathExperience(this.getExperienceReward());
        }

        super.loadCustom(tag);

        if (this.dying) {
            this.health = 0f;
            this.data.set(STRUCTURAL_INTEGRITY, 1);
            this.setHealth(0f);
        }
    }

    @Override
    protected boolean tickEntity(int currentTick, int elapsedTicks) {
        if (this.dying) {
            return tickDeath();
        }

        if (!super.tickEntity(currentTick, elapsedTicks)) {
            return false;
        }

        if (!this.isAlive()) {
            return true;
        }

        this.bossBar.setProgress(this.getHealth() / this.getMaxHealth());
        tickAmbientSound();
        if (currentTick % 20 == 0 && this.getLevel().getDimension() != CloudLevel.DIMENSION_THE_END) {
            updateBossBarPlayers();
        }

        return true;
    }

    @Override
    protected boolean isInDeathSequence() {
        return this.dying;
    }

    @Override
    public void broadcastCriticalHit() {
        super.broadcastCriticalHit();
        this.getLevel().spawnParticleEffect(
                new ParticleEmitter(ParticleEmitterTypes.CRITICAL_HIT_EMITTER, this, Map.of()),
                Vector3f.from(0, this.getHeight() * 0.5f, 0)
        );
    }

    @Override
    public void kill() {
        if (!this.isAlive() || this.dying) {
            return;
        }

        this.beginDeath();
    }

    @Override
    protected EntityDeathEvent createDeathEvent() {
        return new EntityDeathEvent(this, this.getDeathDamageSource(), Arrays.asList(this.getDrops()), this.getExperienceReward());
    }

    @Override
    protected void dropDeathExperience(int experience) {
        this.data().setDragonDeathExperience(experience);
    }

    private int getExperienceReward() {
        CloudLevel level = this.getLevel();
        return level != null && level.getDimension() == CloudLevel.DIMENSION_THE_END
                ? level.getEndFight().getDragonExperienceReward() : 500;
    }

    private void beginDeath() {
        EntityDeathEvent event = this.createDeathEvent();
        if (this.shouldAbortDeath(event)) {
            return;
        }

        this.data().setDragonDeathExperience(event.getDroppedExperience());
        this.dying = true;
        this.health = 0;
        this.data.set(STRUCTURAL_INTEGRITY, 1);
        this.processDeath(event);
        this.broadcastHealth();
        this.bossBar.setProgress(0);
    }

    private boolean tickDeath() {
        this.data().setDragonDeathTime(this.data().getDragonDeathTime() + 1);
        int deathTime = this.data().getDragonDeathTime();
        int deathExperience = this.data().getDragonDeathExperience();
        if (deathTime == 1) {
            this.getLevel().addLevelSoundEvent(this.getPosition(), SoundEvent.DEATH, -1, this.getType(), false, false);
        }

        if (this.getLevel().getGameRules().get(GameRules.DO_MOB_LOOT)
                && deathTime > EXPERIENCE_START_TICK
                && deathTime % EXPERIENCE_INTERVAL == 0) {
            this.getLevel().dropExpOrb(this.getPosition(), (int) Math.floor(deathExperience * 0.08));
        }

        if (this.getLevel().getGameRules().get(GameRules.DO_MOB_LOOT) && deathTime == DEATH_DURATION) {
            this.getLevel().dropExpOrb(this.getPosition(), (int) Math.floor(deathExperience * 0.2));
        }

        if (deathTime < DEATH_DURATION) {
            return true;
        }

        this.bossBar.setProgress(0);

        if (!this.ownsBossBar) {
            this.bossBar.removeAll();
        }

        if (this.getLevel().getDimension() == CloudLevel.DIMENSION_THE_END) {
            this.getLevel().getEndFight().onDragonKilled(this);
        }

        this.despawnFromAll();
        this.remove(EntityRemoveCause.DEATH);
        return false;
    }

    @Override
    public void remove(EntityRemoveCause cause) {
        if (this.isClosed()) {
            return;
        }

        try {
            super.remove(cause);
        } finally {
            if (this.ownsBossBar) {
                this.bossBar.removeAll();
            }
        }
    }

    private void tickAmbientSound() {
        if (--this.ambientSoundDelay >= 0) {
            return;
        }

        this.ambientSoundDelay = MIN_AMBIENT_SOUND_DELAY + ThreadLocalRandom.current().nextInt(AMBIENT_SOUND_DELAY_RANGE);
        EntityEventPacket packet = new EntityEventPacket();
        packet.setRuntimeEntityId(this.getRuntimeId());
        packet.setType(EntityEventType.PLAY_AMBIENT);
        CloudServer.broadcastPacket(this.getViewers(), packet);
    }

    private void updateBossBarPlayers() {
        for (Player player : List.copyOf(this.bossBar.getPlayers())) {
            if (player.getLevel() != this.getLevel() || player.getPosition().distanceSquared(this.getPosition()) > BOSS_BAR_RANGE * BOSS_BAR_RANGE) {
                this.bossBar.removePlayer(player);
            }
        }

        for (Player player : this.getLevel().getPlayers().values()) {
            if (player.getPosition().distanceSquared(this.getPosition()) <= BOSS_BAR_RANGE * BOSS_BAR_RANGE) {
                this.bossBar.addPlayer(player);
            }
        }
    }

    private AttributeData createHealthAttribute() {
        return NetworkEntityAttribute.encodeHealth(this.getHealth(), this.getMaxHealth());
    }

    private void broadcastHealth() {
        if (this.getViewers().isEmpty()) {
            return;
        }

        UpdateAttributesPacket packet = new UpdateAttributesPacket();
        packet.setRuntimeEntityId(this.getRuntimeId());
        packet.setAttributes(List.of(createHealthAttribute()));
        CloudServer.broadcastPacket(this.getViewers(), packet);
    }
}
