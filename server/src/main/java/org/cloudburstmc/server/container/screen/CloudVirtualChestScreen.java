package org.cloudburstmc.server.container.screen;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.inventory.ScreenTypes;
import org.cloudburstmc.api.inventory.VirtualChestScreen;
import org.cloudburstmc.api.inventory.view.SlotGroupTypes;
import org.cloudburstmc.api.inventory.view.StorageView;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType;
import org.cloudburstmc.protocol.bedrock.packet.BlockEntityDataPacket;
import org.cloudburstmc.protocol.bedrock.packet.ContainerOpenPacket;
import org.cloudburstmc.protocol.bedrock.packet.UpdateBlockPacket;
import org.cloudburstmc.server.block.CloudBlock;
import org.cloudburstmc.server.container.CloudContainer;
import org.cloudburstmc.server.container.mapping.SimpleContainerMapping;
import org.cloudburstmc.server.container.view.CloudVirtualStorageSection;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudBlockRegistry;


/**
 * A {@link VirtualChestScreen} implementation that presents a 27-slot chest GUI to a player
 * without any backing block entity in the world.
 */
public class CloudVirtualChestScreen extends CloudContainerScreen implements VirtualChestScreen {

    private static final int CLOSE_RESTORE_DELAY_TICKS = 2;
    private static final int CHEST_SIZE = 27;

    private final CloudContainer backingContainer;
    private final CloudVirtualStorageSection storageSection;

    private String title;
    private Vector3i fakePos;
    private BlockState[] originalStates;

    public CloudVirtualChestScreen(CloudPlayer player, String title) {
        super(ScreenTypes.VIRTUAL_CHEST, player);
        this.title = title;
        this.backingContainer = new CloudContainer(CHEST_SIZE);
        this.storageSection = new CloudVirtualStorageSection(SlotGroupTypes.VIRTUAL_CHEST, backingContainer);
    }

    @Override
    public boolean isOpen() {
        return fakePos != null;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public StorageView getStorage() {
        return getSlotsOrThrow(SlotGroupTypes.VIRTUAL_CHEST);
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

        BlockState chestState = CloudBlockRegistry.REGISTRY.getBlock(BlockTypes.CHEST);
        BlockState airState = CloudBlockRegistry.REGISTRY.getBlock(BlockTypes.AIR);

        CloudBlock fakeBlock = new CloudBlock(level, fakePos, new BlockState[]{chestState, airState});
        level.sendBlocks(
                new Player[]{player},
                new Block[]{fakeBlock},
                UpdateBlockPacket.FLAG_ALL_PRIORITY
        );

        NbtMap chestEntityNbt = NbtMap.builder()
                .putString("id", "Chest")
                .putInt("x", fakePos.getX())
                .putInt("y", fakePos.getY())
                .putInt("z", fakePos.getZ())
                .putString("CustomName", title != null ? title : "Chest")
                .build();

        BlockEntityDataPacket beData = new BlockEntityDataPacket();
        beData.setBlockPosition(fakePos);
        beData.setData(chestEntityNbt);
        player.sendPacket(beData);

        byte windowId = player.assignContainerId(backingContainer);

        ContainerOpenPacket openPkt = new ContainerOpenPacket();
        openPkt.setId(windowId);
        openPkt.setType(ContainerType.CONTAINER);
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

        player.getServer().getScheduler().scheduleDelayedTask(null, () -> {
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
