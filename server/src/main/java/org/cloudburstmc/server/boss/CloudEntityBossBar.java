package org.cloudburstmc.server.boss;

import net.kyori.adventure.text.Component;
import org.cloudburstmc.api.boss.BossBarColor;
import org.cloudburstmc.api.boss.BossBarStyle;
import org.cloudburstmc.api.entity.Entity;

import java.util.Objects;

public class CloudEntityBossBar extends CloudBossBar {

    private Entity entity;

    public CloudEntityBossBar(Component title, BossBarColor color, BossBarStyle style) {
        super(title, color, style);
    }

    public void bindEntity(Entity entity) {
        Objects.requireNonNull(entity, "entity");
        if (this.entity == entity) {
            return;
        }

        replaceBinding(() -> this.entity = entity);
    }

    @Override
    protected boolean isBound() {
        return this.entity != null;
    }

    @Override
    protected long getBossEntityId() {
        if (this.entity == null) {
            throw new IllegalStateException("Boss bar is not bound to an entity");
        }

        return this.entity.getUniqueId();
    }
}
