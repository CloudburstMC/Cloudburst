package org.cloudburstmc.server.boss;

import net.kyori.adventure.text.Component;
import org.cloudburstmc.api.boss.BossBarColor;
import org.cloudburstmc.api.boss.BossBarStyle;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.math.vector.Vector2f;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.packet.AddEntityPacket;
import org.cloudburstmc.protocol.bedrock.packet.MoveEntityAbsolutePacket;
import org.cloudburstmc.protocol.bedrock.packet.RemoveEntityPacket;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudEntityRegistry;

public class CloudStandaloneBossBar extends CloudBossBar {

    private final long entityId = CloudEntityRegistry.get().newEntityId();

    public CloudStandaloneBossBar(Component title, BossBarColor color, BossBarStyle style) {
        super(title, color, style);
    }

    @Override
    public void updatePosition(CloudPlayer player) {
        if (!isShownTo(player)) {
            return;
        }

        MoveEntityAbsolutePacket packet = new MoveEntityAbsolutePacket();
        packet.setRuntimeEntityId(this.entityId);
        packet.setPosition(player.getPosition().sub(0, 10, 0));
        packet.setRotation(Vector3f.ZERO);
        player.sendPacket(packet);
    }

    @Override
    protected boolean isBound() {
        return true;
    }

    @Override
    protected long getBossEntityId() {
        return this.entityId;
    }

    @Override
    protected void showBackingEntity(CloudPlayer player) {
        player.sendPacket(createBackingEntity(player));
    }

    @Override
    protected void hideBackingEntity(CloudPlayer player) {
        RemoveEntityPacket packet = new RemoveEntityPacket();
        packet.setUniqueEntityId(this.entityId);
        player.sendPacket(packet);
    }

    private AddEntityPacket createBackingEntity(CloudPlayer player) {
        AddEntityPacket packet = new AddEntityPacket();
        packet.setIdentifier(EntityTypes.CREEPER.getId().toString());
        packet.setUniqueEntityId(this.entityId);
        packet.setRuntimeEntityId(this.entityId);
        packet.setEntityType(CloudEntityRegistry.get().getRuntimeType(EntityTypes.CREEPER));
        packet.setPosition(player.getPosition().sub(0, 10, 0));
        packet.setRotation(Vector2f.ZERO);
        packet.setMotion(Vector3f.ZERO);
        packet.getMetadata().put(EntityDataTypes.SCALE, 0f);
        packet.getMetadata().put(EntityDataTypes.WIDTH, 0f);
        packet.getMetadata().put(EntityDataTypes.HEIGHT, 0f);
        return packet;
    }
}
