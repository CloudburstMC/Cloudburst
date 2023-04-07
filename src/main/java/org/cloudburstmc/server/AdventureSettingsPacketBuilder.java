

package org.cloudburstmc.server;

import com.nukkitx.protocol.bedrock.data.PlayerPermission;
import com.nukkitx.protocol.bedrock.data.command.CommandPermission;
import com.nukkitx.protocol.bedrock.packet.AdventureSettingsPacket;
import org.cloudburstmc.api.player.AdventureSetting;
import org.cloudburstmc.api.player.AdventureSettings;
import org.cloudburstmc.server.CloudAdventureSettings;
import org.cloudburstmc.server.player.CloudPlayer;

public class AdventureSettingsPacketBuilder {

    public AdventureSettingsPacket build(AdventureSettings settings) {
        AdventureSettingsPacket packet = new AdventureSettingsPacket();
        for (CloudAdventureSettings.Type t : CloudAdventureSettings.Type.values()) {
            AdventureSetting setting = AdventureSetting.values()[t.ordinal()];
            if (settings.get(setting)) {
                packet.getSettings().add(t.getSetting());
            }
        }

        CloudPlayer player = ((CloudAdventureSettings)settings).getPlayer();

        packet.setCommandPermission((player.isOp() ? CommandPermission.OPERATOR : CommandPermission.NORMAL));
        packet.setPlayerPermission((player.isOp() && !player.isSpectator() ? PlayerPermission.OPERATOR : PlayerPermission.MEMBER));
        packet.setUniqueEntityId(player.getUniqueId());

        return packet;
    }
}
