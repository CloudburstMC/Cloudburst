package org.cloudburstmc.server.entity.hostile;

import net.kyori.adventure.text.Component;
import org.cloudburstmc.api.boss.BossBar;
import org.cloudburstmc.api.boss.BossBarColor;
import org.cloudburstmc.api.boss.BossBarStyle;
import org.cloudburstmc.api.entity.Attribute;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.hostile.EnderDragon;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.level.gamerule.GameRules;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.protocol.bedrock.data.AttributeData;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityEventType;
import org.cloudburstmc.protocol.bedrock.packet.AddEntityPacket;
import org.cloudburstmc.protocol.bedrock.packet.EntityEventPacket;
import org.cloudburstmc.protocol.bedrock.packet.UpdateAttributesPacket;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.boss.CloudBossBar;
import org.cloudburstmc.server.boss.CloudStandaloneBossBar;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.level.ParticleEffectIds;
import org.cloudburstmc.server.network.NetworkUtils;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes.STRUCTURAL_INTEGRITY;

public class EntityEnderDragon extends EntityHostile implements EnderDragon {

    private static final String TAG_DRAGON_DEATH_TIME = "DragonDeathTime";
    private static final float BOSS_BAR_RANGE = 192;
    private static final int DEATH_DURATION = 200;
    private static final int EXPERIENCE_START_TICK = 150;
    private static final int EXPERIENCE_INTERVAL = 5;
    private static final int INITIAL_AMBIENT_SOUND_DELAY = 100;
    private static final int MIN_AMBIENT_SOUND_DELAY = 200;
    private static final int AMBIENT_SOUND_DELAY_RANGE = 200;

    private BossBar bossBar;
    private boolean dying;
    private int dragonDeathTime;
    private int ambientSoundDelay;

    public EntityEnderDragon(EntityType<EnderDragon> type, Location location) {
        super(type, location);
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
    public void initEntity() {
        super.initEntity();
        this.noPhysics = true;
        this.setImmobile();
        this.setMaxHealth(200);
        this.setHealth(this.getMaxHealth());
        this.bossBar = this.getLevel().getDimension() == CloudLevel.DIMENSION_THE_END
                ? this.getLevel().getEndFight().getBossBar()
                : new CloudStandaloneBossBar(
                        Component.translatable("entity.ender_dragon.name"),
                        BossBarColor.PURPLE,
                        BossBarStyle.SOLID);
        this.bossBar.setProgress(1);
        this.ambientSoundDelay = INITIAL_AMBIENT_SOUND_DELAY;
    }

    @Override
    public void setHealth(float health) {
        if (health < 1 && this.isAlive() && !this.dying) {
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
    public String getName() {
        return "EnderDragon";
    }

    @Override
    public BossBar getBossBar() {
        return this.bossBar;
    }

    @Override
    public void loadAdditionalData(NbtMap tag) {
        super.loadAdditionalData(tag);
        tag.listenForInt(TAG_DRAGON_DEATH_TIME, dragonDeathTime -> {
            this.dragonDeathTime = dragonDeathTime;
            this.dying = dragonDeathTime > 0;
        });
    }

    @Override
    public void saveAdditionalData(NbtMapBuilder tag) {
        super.saveAdditionalData(tag);
        tag.putInt(TAG_DRAGON_DEATH_TIME, this.dragonDeathTime);
    }

    @Override
    public boolean onUpdate(int currentTick) {
        if (this.dying) {
            return tickDeath();
        }

        if (!super.onUpdate(currentTick)) {
            return false;
        }

        if (!this.isAlive()) {
            return true;
        }

        this.bossBar.setProgress(this.getHealth() / this.getMaxHealth());
        tickAmbientSound();
        if (currentTick % 20 == 0) {
            updateBossBarPlayers();
        }

        return true;
    }

    @Override
    protected boolean isInDeathSequence() {
        return this.dying;
    }

    @Override
    protected void showCriticalHit(Entity impactEntity) {
        super.showCriticalHit(impactEntity);
        this.getLevel().addParticleEffect(
                this.getPosition().add(0, this.getHeight() * 0.5f, 0),
                ParticleEffectIds.CRITICAL_HIT_EMITTER,
                this.getUniqueId()
        );
    }

    @Override
    public void kill() {
        if (!this.isAlive() || this.dying) {
            return;
        }

        this.beginDeath();
    }

    private void beginDeath() {
        this.dying = true;
        this.health = 0;
        this.data.set(STRUCTURAL_INTEGRITY, 1);
        this.processDeath();
        this.broadcastHealth();
        this.bossBar.setProgress(0);
    }

    private boolean tickDeath() {
        this.dragonDeathTime++;
        if (this.dragonDeathTime == 1) {
            this.getLevel().addLevelSoundEvent(this.getPosition(), SoundEvent.DEATH, -1, this.getType(), false, false);
        }

        if (this.getLevel().getGameRules().get(GameRules.DO_MOB_LOOT)
                && this.dragonDeathTime > EXPERIENCE_START_TICK
                && this.dragonDeathTime % EXPERIENCE_INTERVAL == 0) {
            int experience = this.getLevel().getDimension() == CloudLevel.DIMENSION_THE_END
                    ? this.getLevel().getEndFight().getDragonExperienceReward()
                    : 500;
            this.getLevel().dropExpOrb(this.getPosition(), (int) Math.floor(experience * 0.08));
        }

        if (this.getLevel().getGameRules().get(GameRules.DO_MOB_LOOT) && this.dragonDeathTime == DEATH_DURATION) {
            int experience = this.getLevel().getDimension() == CloudLevel.DIMENSION_THE_END
                    ? this.getLevel().getEndFight().getDragonExperienceReward()
                    : 500;
            this.getLevel().dropExpOrb(this.getPosition(), (int) Math.floor(experience * 0.2));
        }

        if (this.dragonDeathTime < DEATH_DURATION) {
            return true;
        }

        this.bossBar.setProgress(0);
        this.bossBar.removeAll();

        if (this.getLevel().getDimension() == CloudLevel.DIMENSION_THE_END) {
            this.getLevel().getEndFight().onDragonKilled(this);
        }

        this.despawnFromAll();
        this.close();
        return false;
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
        Attribute health = Attribute.getAttribute(Attribute.MAX_HEALTH)
                .setMaxValue(this.getMaxHealth())
                .setDefaultValue(this.getMaxHealth())
                .setValue(this.getHealth());
        return NetworkUtils.attributeToNetwork(health);
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
