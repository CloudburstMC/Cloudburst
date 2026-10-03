package org.cloudburstmc.server.player.handler;

import io.netty.buffer.Unpooled;
import lombok.extern.log4j.Log4j2;
import net.kyori.adventure.text.Component;
import org.cloudburstmc.protocol.bedrock.BedrockServerSession;
import org.cloudburstmc.protocol.bedrock.data.DisconnectFailReason;
import org.cloudburstmc.protocol.bedrock.data.ResourcePackType;
import org.cloudburstmc.protocol.bedrock.packet.*;
import org.cloudburstmc.protocol.common.PacketSignal;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.pack.CloudResourcePack;
import org.cloudburstmc.server.player.PlayerLoginContext;

import java.io.IOException;

@Log4j2
public class ResourcePackPacketHandler implements BedrockPacketHandler {

    private static final int RESOURCE_PACK_CHUNK_SIZE = 8 * 1024;

    private final BedrockServerSession session;
    private final CloudServer server;
    private final PlayerLoginContext loginContext;
    private boolean completed;

    public ResourcePackPacketHandler(BedrockServerSession session, CloudServer server, PlayerLoginContext loginContext) {
        this.session = session;
        this.server = server;
        this.loginContext = loginContext;
    }

    @Override
    public PacketSignal handle(ResourcePackClientResponsePacket packet) {
        this.server.getGlobalScheduler().execute(null, () -> this.handleResponse(packet));
        return PacketSignal.HANDLED;
    }

    private void handleResponse(ResourcePackClientResponsePacket packet) {
        if (this.completed || !this.loginContext.isActive()) {
            return;
        }

        switch (packet.getStatus()) {
            case REFUSED -> this.disconnect(DisconnectFailReason.NO_REASON, "disconnectionScreen.noReason");
            case SEND_PACKS -> {
                for (String entry : packet.getPackIds()) {
                    CloudResourcePack pack = this.server.getPackManager().getPackByIdVersion(entry);
                    if (pack == null) {
                        this.disconnect(DisconnectFailReason.RESOURCE_PACK_PROBLEM, "disconnectionScreen.resourcePack");
                        return;
                    }

                    ResourcePackDataInfoPacket dataInfoPacket = new ResourcePackDataInfoPacket();
                    dataInfoPacket.setPackId(pack.getId());
                    dataInfoPacket.setPackVersion(pack.getVersion().toString());
                    dataInfoPacket.setMaxChunkSize(RESOURCE_PACK_CHUNK_SIZE);
                    dataInfoPacket.setChunkCount(Math.toIntExact((pack.getSize() - 1) / RESOURCE_PACK_CHUNK_SIZE + 1));
                    dataInfoPacket.setCompressedPackSize(pack.getSize());
                    dataInfoPacket.setHash(pack.getHash());
                    dataInfoPacket.setType(ResourcePackType.RESOURCES);
                    this.session.sendPacket(dataInfoPacket);
                }
            }
            case HAVE_ALL_PACKS -> this.session.sendPacket(this.server.getPackManager().createStackPacket(this.server.getForceResources()));
            case COMPLETED -> {
                this.completed = true;
                this.loginContext.completeResourcePacks();
            }
            default -> {
            }
        }
    }

    @Override
    public PacketSignal handle(ResourcePackChunkRequestPacket packet) {
        CloudResourcePack resourcePack = this.server.getPackManager().getPackByIdVersion(packet.getPackId() + "_" + packet.getPackVersion());
        if (resourcePack == null) {
            this.disconnect(DisconnectFailReason.RESOURCE_PACK_PROBLEM, "disconnectionScreen.resourcePack");
            return PacketSignal.HANDLED;
        }

        long offset = (long) RESOURCE_PACK_CHUNK_SIZE * packet.getChunkIndex();
        if (packet.getChunkIndex() < 0) {
            this.disconnect(DisconnectFailReason.RESOURCE_PACK_PROBLEM, "disconnectionScreen.resourcePack");
            return PacketSignal.HANDLED;
        }

        byte[] chunk;
        try {
            chunk = resourcePack.readChunk(offset, RESOURCE_PACK_CHUNK_SIZE);
        } catch (IOException | IllegalArgumentException failure) {
            log.warn("Unable to read resource pack {} for {}", packet.getPackId(), this.session.getSocketAddress(), failure);
            this.disconnect(DisconnectFailReason.RESOURCE_PACK_PROBLEM, "disconnectionScreen.resourcePack");
            return PacketSignal.HANDLED;
        }

        if (chunk.length == 0) {
            this.disconnect(DisconnectFailReason.RESOURCE_PACK_PROBLEM, "disconnectionScreen.resourcePack");
            return PacketSignal.HANDLED;
        }

        ResourcePackChunkDataPacket dataPacket = new ResourcePackChunkDataPacket();
        dataPacket.setPackId(packet.getPackId());
        dataPacket.setPackVersion(packet.getPackVersion());
        dataPacket.setChunkIndex(packet.getChunkIndex());
        dataPacket.setData(Unpooled.wrappedBuffer(chunk));
        dataPacket.setProgress(offset);
        this.session.sendPacket(dataPacket);
        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(ClientCacheStatusPacket packet) {
        this.loginContext.setClientCacheEnabled(packet.isSupported());
        return PacketSignal.HANDLED;
    }

    private void disconnect(DisconnectFailReason reason, String translationKey) {
        if (this.server.isPrimaryThread()) {
            if (!this.completed) {
                this.loginContext.disconnect(reason, Component.text(this.server.getLanguage().translate(translationKey)));
            }
        } else {
            this.server.getGlobalScheduler().execute(null, () -> this.disconnect(reason, translationKey));
        }
    }
}
