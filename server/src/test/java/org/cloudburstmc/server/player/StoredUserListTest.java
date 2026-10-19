package org.cloudburstmc.server.player;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class StoredUserListTest {

    @TempDir
    Path directory;

    @Test
    void jsonListsUseConsistentMultilineFormatting() throws Exception {
        Path file = directory.resolve("ops.json");
        ServerOpList list = new ServerOpList(file);
        UUID uuid = UUID.fromString("75cd5b39-da8c-31ec-a7ef-4efdf2fe451b");
        assertEquals("[]", Files.readString(file));

        list.add(new ServerOpListEntry(uuid, "VibingGuy69", false));

        assertEquals("""
                [
                  {
                    "uuid": "75cd5b39-da8c-31ec-a7ef-4efdf2fe451b",
                    "name": "VibingGuy69",
                    "bypassesPlayerLimit": false
                  }
                ]
                """.stripTrailing(), Files.readString(file));
    }

    @Test
    void repeatedJsonSavesDoNotCreateBackupFiles() throws Exception {
        Path file = directory.resolve("ops.json");
        ServerOpList list = new ServerOpList(file);
        UUID uuid = UUID.randomUUID();
        list.add(new ServerOpListEntry(uuid, "Player", false));
        list.add(new ServerOpListEntry(uuid, "Renamed", true));

        assertEquals("Renamed", new ServerOpList(file).get(uuid).orElseThrow().name());
        try (Stream<Path> files = Files.list(directory)) {
            assertEquals(List.of(file), files.toList());
        }
    }

    @Test
    void membershipUsesUuidRatherThanDisplayName() throws Exception {
        Path file = directory.resolve("whitelist.json");
        UserWhiteList list = new UserWhiteList(file);
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        list.add(new UserWhiteListEntry(first, "Player"));

        assertTrue(list.contains(first));
        assertFalse(list.contains(second));

        list.add(new UserWhiteListEntry(first, "Renamed"));
        UserWhiteList reloaded = new UserWhiteList(file);

        assertEquals(1, reloaded.entries().size());
        assertEquals("Renamed", reloaded.get(first).orElseThrow().name());
        assertTrue(Files.readString(file).contains(first.toString()));

        reloaded.remove(first);
        assertTrue(new UserWhiteList(file).entries().isEmpty());
    }

    @Test
    void invalidReloadPreservesActiveMembershipAndTheInputFile() throws Exception {
        Path file = directory.resolve("whitelist.json");
        UserWhiteList list = new UserWhiteList(file);
        UUID uuid = UUID.randomUUID();
        list.add(new UserWhiteListEntry(uuid, "Player"));
        String duplicate = "[{\"uuid\":\"%s\"},{\"uuid\":\"%s\"}]".formatted(uuid, uuid);
        Files.writeString(file, duplicate);

        assertThrows(RuntimeException.class, list::reload);
        assertTrue(list.contains(uuid));
        assertEquals(duplicate, Files.readString(file));
    }

    @Test
    void failedSaveDoesNotPublishOrOverwriteMembership() throws Exception {
        Path file = directory.resolve("whitelist.json");
        UserWhiteList list = new UserWhiteList(file);
        String original = Files.readString(file);
        UUID uuid = UUID.randomUUID();

        assertThrows(IllegalArgumentException.class, () -> list.add(new UserWhiteListEntry(uuid, "x".repeat(1024 * 1024))));
        assertFalse(list.contains(uuid));
        assertEquals(original, Files.readString(file));
    }

    @Test
    void playerLimitBypassIsExplicitAndSurvivesReload() {
        Path file = directory.resolve("ops.json");
        ServerOpList list = new ServerOpList(file);
        UUID regular = UUID.randomUUID();
        UUID bypass = UUID.randomUUID();
        list.add(new ServerOpListEntry(regular, "Regular", false));
        list.add(new ServerOpListEntry(bypass, "Bypass", true));

        ServerOpList reloaded = new ServerOpList(file);

        assertFalse(reloaded.canBypassPlayerLimit(regular));
        assertTrue(reloaded.canBypassPlayerLimit(bypass));
        assertFalse(reloaded.canBypassPlayerLimit(UUID.randomUUID()));
    }

    @Test
    void malformedEntriesAndTrailingDocumentsAreRejected() throws Exception {
        Path file = directory.resolve("whitelist.json");
        for (String invalid : new String[]{"null", "[null]", "[{\"uuid\":null}]", "[] []"}) {
            Files.writeString(file, invalid);
            assertThrows(RuntimeException.class, () -> new UserWhiteList(file));
        }
    }
}
