package org.cloudburstmc.server.entity.hostile;

import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.boss.BossBar;
import org.cloudburstmc.api.boss.BossBarColor;
import org.cloudburstmc.api.boss.BossBarStyle;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.hostile.Wither;
import org.cloudburstmc.api.event.entity.EntityDeathEvent;
import org.cloudburstmc.api.event.entity.EntityRemoveCause;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.server.boss.CloudStandaloneBossBar;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.Objects;

public class EntityWither extends EntityHostile implements Wither {

    private final BossBar bossBar = new CloudStandaloneBossBar(Component.translatable("entity.wither.name"), BossBarColor.PURPLE, BossBarStyle.SOLID);

    public EntityWither(EntityType<Wither> type, Location location) {
        super(type, location);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setDefaultMaxHealth(300);
    }

    @Override
    public float getWidth() {
        return 0.9f;
    }

    @Override
    public float getHeight() {
        return 3.5f;
    }

    @Override
    protected String getDefaultName() {
        return "Wither";
    }

    @Override
    public void customName(@Nullable Component name) {
        Component previousName = this.customName();
        super.customName(name);
        if (!Objects.equals(previousName, name)) {
            this.bossBar.setTitle(this.displayName());
        }
    }

    @Override
    public BossBar getBossBar() {
        return this.bossBar;
    }

    @Override
    public void setHealth(float health) {
        super.setHealth(health);
        this.updateBossBarProgress();
    }

    @Override
    public void setMaxHealth(int baseMaxHealth) {
        super.setMaxHealth(baseMaxHealth);
        this.updateBossBarProgress();
    }

    @Override
    protected boolean tickEntity(int currentTick, int elapsedTicks) {
        boolean active = super.tickEntity(currentTick, elapsedTicks);
        if (!this.isClosed()) {
            this.updateBossBarProgress();
        }
        return active;
    }

    @Override
    protected void processDeath(EntityDeathEvent event) {
        this.bossBar.setProgress(0);
        super.processDeath(event);
    }

    @Override
    public void spawnTo(CloudPlayer player) {
        boolean wasViewing = this.hasViewer(player);
        super.spawnTo(player);
        if (!wasViewing && this.hasViewer(player) && this.isAlive()) {
            this.bossBar.addPlayer(player);
        }
    }

    @Override
    public void despawnFrom(CloudPlayer player) {
        this.bossBar.removePlayer(player);
        super.despawnFrom(player);
    }

    @Override
    public void remove(EntityRemoveCause cause) {
        try {
            super.remove(cause);
        } finally {
            this.bossBar.removeAll();
        }
    }

    private void updateBossBarProgress() {
        this.bossBar.setProgress(this.getHealth() / (double) this.getMaxHealth());
    }
}
