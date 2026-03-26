package org.cloudburstmc.server.container.screen;

import net.kyori.adventure.text.Component;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.inventory.ScreenTypes;
import org.cloudburstmc.api.inventory.VirtualHopperScreen;
import org.cloudburstmc.api.inventory.view.HopperView;
import org.cloudburstmc.api.inventory.view.SlotGroupTypes;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.protocol.adventure.BedrockLegacyTextSerializer;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType;
import org.cloudburstmc.protocol.bedrock.packet.BlockEntityDataPacket;
import org.cloudburstmc.protocol.bedrock.packet.ContainerOpenPacket;
import org.cloudburstmc.protocol.bedrock.packet.UpdateBlockPacket;
import org.cloudburstmc.server.block.CloudBlock;
import org.cloudburstmc.server.container.CloudContainer;
import org.cloudburstmc.server.container.mapping.SimpleContainerMapping;
import org.cloudburstmc.server.container.view.CloudVirtualHopperSection;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudBlockRegistry;

/**
 * A {@link VirtualHopperScreen} implementation that presents a 5-slot hopper GUI to a player
 * without any backing block entity in the world.
 */
public class CloudVirtualHopperScreen extends CloudContainerScreen implements VirtualHopperScreen {

    private static final int CLOSE_RESTORE_DELAY_TICKS = 2;
    private static final int HOPPER_SIZE = 5;

    private final CloudContainer backingContainer;
    private final CloudVirtualHopperSection storageSection;

    private Component title;
    private Vector3i fakePos;
    private BlockState[] originalStates;

    public CloudVirtualHopperScreen(CloudPlayer player, Component title) {
        super(ScreenTypes.VIRTUAL_HOPPER, player);
        this.title = title;
        this.backingContainer = new CloudContainer(HOPPER_SIZE);
        this.storageSection = new CloudVirtualHopperSection(backingContainer);
    }

    @Override
    public boolean isOpen() {
        return fakePos != null;
    }

    @Override
    public Component getTitle() {
        return title;
    }

    @Override
    public void setTitle(Component title) {
        this.title = title;
    }

    @Override
    public HopperView getHopper() {
        return getSlotsOrThrow(SlotGroupTypes.VIRTUAL_HOPPER);
    }

    @Override
    protected void setupMappings() {
        super.setupMappings();
        this.addMapping(new SimpleContainerMapping(ContainerSlotType.LEVEL_ENTITY, storageSection));
    }

    @Override
    public void open() {
        CloudLevel level = player.getLevel();

        Vector3f pos = player.getPosition();
        int fakeY = Math.min((int) pos.getY() + 2, level.getMaxHeight() - 1);
        fakePos = Vector3i.from((int) pos.getX(), fakeY, (int) pos.getZ());

        originalStates = new BlockState[]{
                level.getBlockState(fakePos.getX(), fakePos.getY(), fakePos.getZ(), 0),
                level.getBlockState(fakePos.getX(), fakePos.getY(), fakePos.getZ(), 1)
        };

        BlockState hopperState = CloudBlockRegistry.REGISTRY.getBlock(BlockTypes.HOPPER);
        BlockState airState = CloudBlockRegistry.REGISTRY.getBlock(BlockTypes.AIR);

        CloudBlock fakeBlock = new CloudBlock(level, fakePos, new BlockState[]{hopperState, airState});
        level.sendBlocks(
                new Player[]{player},
                new Block[]{fakeBlock},
                UpdateBlockPacket.FLAG_ALL_PRIORITY
        );

        NbtMap hopperEntityNbt = NbtMap.builder()
                .putString("id", "Hopper")
                .putInt("x", fakePos.getX())
                .putInt("y", fakePos.getY())
                .putInt("z", fakePos.getZ())
                .putString("CustomName", title != null ? BedrockLegacyTextSerializer.getInstance().serialize(title) : "Hopper")
                .build();

        BlockEntityDataPacket beData = new BlockEntityDataPacket();
        beData.setBlockPosition(fakePos);
        beData.setData(hopperEntityNbt);
        player.sendPacket(beData);

        byte windowId = player.assignContainerId(backingContainer);

        ContainerOpenPacket openPkt = new ContainerOpenPacket();
        openPkt.setId(windowId);
        openPkt.setType(ContainerType.HOPPER);
        openPkt.setBlockPosition(fakePos);
        player.sendPacket(openPkt);

        player.getInventoryManager().sendAllInventories();
    }

    @Override
    public void close() {
        if (fakePos == null) {
            return;
        }

        final Vector3i posToRestore = fakePos;
        final BlockState[] statesToRestore = originalStates;
        fakePos = null;
        originalStates = null;

        player.getServer().getGlobalScheduler().runDelayed(null, t -> {
            if (!player.isConnected()) {
                return;
            }
            CloudLevel level = player.getLevel();
            CloudBlock restore = new CloudBlock(level, posToRestore, statesToRestore);
            level.sendBlocks(
                    new Player[]{player},
                    new Block[]{restore},
                    UpdateBlockPacket.FLAG_ALL_PRIORITY
            );
        }, CLOSE_RESTORE_DELAY_TICKS);
    }
}
