package org.cloudburstmc.server.pack;

import org.cloudburstmc.api.pack.ResourcePack;
import org.cloudburstmc.protocol.bedrock.packet.ResourcePackStackPacket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CloudPackManagerTest {

    @TempDir
    private Path directory;
    private final CloudPackManager manager = new CloudPackManager();

    @AfterEach
    void releaseArchives() {
        this.manager.shutdown();
    }

    @Test
    void rejectsDuplicateIdentifiersWithoutReplacingTheRegisteredPack() throws Exception {
        UUID id = UUID.randomUUID();
        this.manager.loadPack(this.source("first", id, ""));
        ResourcePack registered = this.manager.get(id).orElseThrow();

        assertThrows(IOException.class, () -> this.manager.loadPack(this.source("second", id, "")));
        assertSame(registered, this.manager.get(id).orElseThrow());
    }

    @Test
    void preventsRegistrationAfterFinalization() {
        this.manager.close();

        assertThrows(IllegalStateException.class, () -> this.manager.loadPack(this.source("late", UUID.randomUUID(), "")));
    }

    @Test
    void doesNotAdvertiseUnfinalizedPacks() {
        assertThrows(IllegalStateException.class, () -> this.manager.createInfoPacket(false));
        assertThrows(IllegalStateException.class, () -> this.manager.createStackPacket(false));
    }

    @Test
    void ordersDependenciesBeforeTheirDependentsRegardlessOfLoadOrder() throws Exception {
        UUID base = UUID.randomUUID();
        UUID dependent = UUID.randomUUID();
        this.manager.loadPack(this.source("dependent", dependent, CloudPackTestFiles.dependency(base, 1)));
        this.manager.loadPack(this.source("base", base, ""));
        this.manager.close();

        assertEquals(List.of(base.toString(), dependent.toString()), this.manager.createStackPacket(false)
                .getResourcePacks().stream().map(ResourcePackStackPacket.Entry::getPackId).toList());
    }

    @Test
    void rejectsDependentsWhenTheirRequiredVersionIsUnavailable() throws Exception {
        UUID base = UUID.randomUUID();
        UUID dependent = UUID.randomUUID();
        this.manager.loadPack(this.source("base", base, ""));
        this.manager.loadPack(this.source("dependent", dependent, CloudPackTestFiles.dependency(base, 2)));
        this.manager.close();

        assertTrue(this.manager.get(base).isPresent());
        assertTrue(this.manager.get(dependent).isEmpty());
    }

    @Test
    void rejectsAnEntireChainWithAMissingDependency() throws Exception {
        UUID missing = UUID.randomUUID();
        UUID base = UUID.randomUUID();
        UUID dependent = UUID.randomUUID();
        this.manager.loadPack(this.source("dependent", dependent, CloudPackTestFiles.dependency(base, 1)));
        this.manager.loadPack(this.source("base", base, CloudPackTestFiles.dependency(missing, 1)));
        this.manager.close();

        assertTrue(this.manager.values().isEmpty());
    }

    @Test
    void rejectsCyclicDependenciesAndTheirDependents() throws Exception {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID dependent = UUID.randomUUID();
        this.manager.loadPack(this.source("first", first, CloudPackTestFiles.dependency(second, 1)));
        this.manager.loadPack(this.source("second", second, CloudPackTestFiles.dependency(first, 1)));
        this.manager.loadPack(this.source("dependent", dependent, CloudPackTestFiles.dependency(first, 1)));
        this.manager.close();

        assertTrue(this.manager.values().isEmpty());
    }

    @Test
    void returnsAnImmutableRegistrySnapshot() throws Exception {
        Collection<ResourcePack> snapshot = this.manager.values();
        this.manager.loadPack(this.source("pack", UUID.randomUUID(), ""));

        assertTrue(snapshot.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> this.manager.values().clear());
    }

    @Test
    void createsIndependentPacketsForEachConnection() throws Exception {
        this.manager.loadPack(this.source("pack", UUID.randomUUID(), ""));
        this.manager.close();
        this.manager.createInfoPacket(false).getResourcePackInfos().clear();
        this.manager.createStackPacket(false).getResourcePacks().clear();

        assertEquals(1, this.manager.createInfoPacket(false).getResourcePackInfos().size());
        assertEquals(1, this.manager.createStackPacket(false).getResourcePacks().size());
    }

    @Test
    void appliesTheRequiredFlagToBothPacketTypes() {
        this.manager.close();

        assertTrue(this.manager.createInfoPacket(true).isForcedToAccept());
        assertTrue(this.manager.createStackPacket(true).isForcedToAccept());
        assertFalse(this.manager.createInfoPacket(false).isForcedToAccept());
        assertFalse(this.manager.createStackPacket(false).isForcedToAccept());
    }

    @Test
    void validatesTheRequestedIdentifierAndVersion() throws Exception {
        UUID id = UUID.randomUUID();
        this.manager.loadPack(this.source("pack", id, ""));
        this.manager.close();

        assertNotNull(this.manager.getPackByIdVersion(id + "_1.0.0"));
        assertNull(this.manager.getPackByIdVersion(id + "_2.0.0"));
        assertNull(this.manager.getPackByIdVersion("invalid_1.0.0"));
        assertNull(this.manager.getPackByIdVersion(id.toString()));
    }

    @Test
    void continuesLoadingValidPacksAfterAnInvalidArchive() throws Exception {
        UUID id = UUID.randomUUID();
        Files.writeString(this.directory.resolve("a-invalid.mcpack"), "invalid archive");
        this.source("b-valid", id, "");
        this.manager.loadPacks(this.directory);
        this.manager.close();

        assertTrue(this.manager.get(id).isPresent());
        assertEquals(1, this.manager.values().size());
    }

    @Test
    void shutdownReleasesTransfersAndPreventsFurtherRegistration() throws Exception {
        UUID id = UUID.randomUUID();
        this.manager.loadPack(this.source("pack", id, ""));
        this.manager.close();
        CloudResourcePack pack = this.manager.getPackByIdVersion(id + "_1.0.0");

        assertNotNull(pack);

        this.manager.shutdown();
        this.manager.shutdown();

        assertThrows(IOException.class, () -> pack.readChunk(0, 1));
        assertThrows(IllegalStateException.class, () -> this.manager.createInfoPacket(false));
        assertThrows(IllegalStateException.class, () -> this.manager.loadPack(this.source("late", UUID.randomUUID(), "")));
    }

    private Path source(String name, UUID id, String dependencies) throws IOException {
        return CloudPackTestFiles.directory(this.directory.resolve(name), id, dependencies);
    }
}
