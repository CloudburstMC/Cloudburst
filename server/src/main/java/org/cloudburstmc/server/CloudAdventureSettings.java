package org.cloudburstmc.server;

import org.cloudburstmc.api.player.AdventureSetting;
import org.cloudburstmc.api.player.AdventureSettings;
import org.cloudburstmc.protocol.bedrock.data.PlayerPermission;
import org.cloudburstmc.protocol.bedrock.data.command.CommandPermission;
import org.cloudburstmc.protocol.bedrock.packet.AdventureSettingsPacket;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.EnumMap;
import java.util.Map;

/**
 * Nukkit Project
 * Author: MagicDroidX
 */
public class CloudAdventureSettings implements AdventureSettings, Cloneable {

    private final Map<AdventureSetting, Boolean> values = new EnumMap<>(AdventureSetting.class);

    private CloudPlayer player;

    public CloudAdventureSettings(CloudPlayer player) {
        this.player = player;
    }

    public CloudAdventureSettings(CloudPlayer player, Map<AdventureSetting, Boolean> values) {
        this.player = player;
        this.values.putAll(values);
    }

    public CloudAdventureSettings clone(CloudPlayer newPlayer) {
        try {
            CloudAdventureSettings settings = (CloudAdventureSettings) super.clone();
            settings.player = newPlayer;
            return settings;
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    public CloudAdventureSettings set(AdventureSetting type, boolean value) {
        this.values.put(type, value);
        return this;
    }

    public boolean get(AdventureSetting type) {
        Boolean value = this.values.get(type);

        return value == null ? type.getDefaultValue() : value;
    }

    public void update() {
        AdventureSettingsPacket pk = new AdventureSettingsPacket();
        for (Type t : Type.values()) {
            AdventureSetting setting = AdventureSetting.values()[t.ordinal()];
            if (get(setting)) {
                pk.getSettings().add(t.getSetting());
            }
        }

        pk.setCommandPermission((player.isOp() ? CommandPermission.ADMIN : CommandPermission.ANY));
        pk.setPlayerPermission((player.isOp() && !player.isSpectator() ? PlayerPermission.OPERATOR : PlayerPermission.MEMBER));
        pk.setUniqueEntityId(player.getUniqueId());

        CloudServer.broadcastPacket(player.getViewers(), pk);
        player.sendPacket(pk);

        player.resetInAirTicks();
    }

    public enum Type {
        WORLD_IMMUTABLE(org.cloudburstmc.protocol.bedrock.data.AdventureSetting.WORLD_IMMUTABLE, false),
        NO_PVM(org.cloudburstmc.protocol.bedrock.data.AdventureSetting.NO_PVM, false),
        NO_MVP(org.cloudburstmc.protocol.bedrock.data.AdventureSetting.NO_MVP, false),
        SHOW_NAME_TAGS(org.cloudburstmc.protocol.bedrock.data.AdventureSetting.SHOW_NAME_TAGS, true),
        AUTO_JUMP(org.cloudburstmc.protocol.bedrock.data.AdventureSetting.AUTO_JUMP, true),
        ALLOW_FLIGHT(org.cloudburstmc.protocol.bedrock.data.AdventureSetting.MAY_FLY, false),
        NO_CLIP(org.cloudburstmc.protocol.bedrock.data.AdventureSetting.NO_CLIP, false),
        WORLD_BUILDER(org.cloudburstmc.protocol.bedrock.data.AdventureSetting.WORLD_BUILDER, true),
        FLYING(org.cloudburstmc.protocol.bedrock.data.AdventureSetting.FLYING, false),
        MUTED(org.cloudburstmc.protocol.bedrock.data.AdventureSetting.MUTED, false),
        MINE(org.cloudburstmc.protocol.bedrock.data.AdventureSetting.MINE, true),
        DOORS_AND_SWITCHED(org.cloudburstmc.protocol.bedrock.data.AdventureSetting.DOORS_AND_SWITCHES, true),
        OPEN_CONTAINERS(org.cloudburstmc.protocol.bedrock.data.AdventureSetting.OPEN_CONTAINERS, true),
        ATTACK_PLAYERS(org.cloudburstmc.protocol.bedrock.data.AdventureSetting.ATTACK_PLAYERS, true),
        ATTACK_MOBS(org.cloudburstmc.protocol.bedrock.data.AdventureSetting.ATTACK_MOBS, true),
        OPERATOR(org.cloudburstmc.protocol.bedrock.data.AdventureSetting.OPERATOR, false),
        TELEPORT(org.cloudburstmc.protocol.bedrock.data.AdventureSetting.TELEPORT, false),
        BUILD(org.cloudburstmc.protocol.bedrock.data.AdventureSetting.BUILD, true);

        private final org.cloudburstmc.protocol.bedrock.data.AdventureSetting flag;
        private final boolean defaultValue;

        Type(org.cloudburstmc.protocol.bedrock.data.AdventureSetting flag, boolean defaultValue) {
            this.flag = flag;
            this.defaultValue = defaultValue;
        }

        public org.cloudburstmc.protocol.bedrock.data.AdventureSetting getSetting() {
            return flag;
        }

        public boolean getDefaultValue() {
            return this.defaultValue;
        }
    }
}
