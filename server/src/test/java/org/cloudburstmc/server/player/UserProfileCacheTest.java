package org.cloudburstmc.server.player;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserProfileCacheTest {

    @TempDir
    Path directory;

    @Test
    void observedIdentitySurvivesReloadAndRenaming() {
        Path file = directory.resolve("usercache.json");
        UUID uuid = UUID.randomUUID();
        try (UserProfileCache cache = new UserProfileCache(file, 1000, false)) {
            cache.add(uuid, "FirstName");
            cache.add(uuid, "NewName");
            cache.flush();

            try (UserProfileCache reloaded = new UserProfileCache(file, 1000, false)) {
                assertEquals(uuid, reloaded.find("newname").orElseThrow());
                assertTrue(reloaded.find("FirstName").isEmpty());
                assertEquals("NewName", reloaded.find(uuid).orElseThrow().name());
                assertTrue(reloaded.find("Unknown").isEmpty());
            }
        }

        assertFalse(Files.exists(directory.resolve("usercache.json_old")));
    }

    @Test
    void nameReuseReplacesItsPreviousIdentity() {
        try (UserProfileCache cache = new UserProfileCache(directory.resolve("usercache.json"), 1000, false)) {
            UUID old = UUID.randomUUID();
            UUID current = UUID.randomUUID();
            cache.add(old, "Player");
            cache.add(current, "PLAYER");

            assertEquals(current, cache.find("player").orElseThrow());
            assertTrue(cache.find(old).isEmpty());
        }
    }

    @Test
    void expiredEntriesDoNotResolveNamesAndAreRemovedFromSavedSnapshots() throws Exception {
        Path file = directory.resolve("usercache.json");
        Files.writeString(file, "[{\"uuid\":\"%s\",\"name\":\"Expired\",\"expiresOn\":\"2000-01-01T00:00:00Z\"}]"
                .formatted(UUID.randomUUID()));

        try (UserProfileCache cache = new UserProfileCache(file, 1000, false)) {
            assertTrue(cache.find("Expired").isEmpty());
            cache.flush();
            assertEquals("[]", Files.readString(file));
        }
    }

    @Test
    void capacityLimitsSavedProfilesRatherThanLiveIdentityLookups() {
        Path file = directory.resolve("usercache.json");
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID third = UUID.randomUUID();
        try (UserProfileCache cache = new UserProfileCache(file, 2, false)) {
            cache.add(first, "First");
            cache.add(second, "Second");
            cache.add(third, "Third");
            assertTrue(cache.find(first).isPresent());
            assertTrue(cache.find("second").isPresent());
            cache.flush();

            try (UserProfileCache reloaded = new UserProfileCache(file, 2, false)) {
                assertTrue(reloaded.find(first).isPresent());
                assertTrue(reloaded.find(second).isPresent());
                assertTrue(reloaded.find(third).isEmpty());
            }

            assertTrue(cache.find(third).isPresent());
        }
    }

    @Test
    void zeroCapacityDisablesPersistenceWithoutDisablingLiveLookups() throws Exception {
        Path file = directory.resolve("usercache.json");
        UUID uuid = UUID.randomUUID();
        try (UserProfileCache cache = new UserProfileCache(file, 0, false)) {
            cache.add(uuid, "Player");
            cache.flush();

            assertEquals(uuid, cache.find("Player").orElseThrow());
            assertEquals("[]", Files.readString(file));
        }
    }

    @Test
    void shutdownOnlyModeDoesNotWriteIdentityUpdatesUntilClosed() throws Exception {
        Path file = directory.resolve("usercache.json");
        UUID uuid = UUID.randomUUID();
        try (UserProfileCache cache = new UserProfileCache(file, 1000, true)) {
            String original = Files.readString(file);
            cache.add(uuid, "Player");
            assertEquals(original, Files.readString(file));
            assertEquals(uuid, cache.find("player").orElseThrow());
        }

        try (UserProfileCache reloaded = new UserProfileCache(file, 1000, true)) {
            assertEquals(uuid, reloaded.find("player").orElseThrow());
        }
    }

    @Test
    void coalescedSavesAndShutdownPersistTheLatestIdentity() {
        Path file = directory.resolve("usercache.json");
        UUID uuid = UUID.randomUUID();
        try (UserProfileCache cache = new UserProfileCache(file, 1000, false)) {
            for (int i = 0; i < 100; i++) {
                cache.add(uuid, "Player" + i);
            }
        }

        try (UserProfileCache reloaded = new UserProfileCache(file, 1000, false)) {
            assertEquals(uuid, reloaded.find("Player99").orElseThrow());
            assertTrue(reloaded.find("Player0").isEmpty());
        }
    }

    @Test
    void expirationUsesACalendarMonth() throws Exception {
        Clock clock = Clock.fixed(Instant.parse("2026-01-31T12:00:00Z"), ZoneOffset.UTC);
        UUID uuid = UUID.randomUUID();
        Path file = directory.resolve("usercache.json");
        try (UserProfileCache cache = new UserProfileCache(file, 1000, true, clock)) {
            cache.add(uuid, "Player");

            assertEquals(Instant.parse("2026-02-28T12:00:00Z"),
                    cache.find(uuid).orElseThrow().expiresOn());
        }

        assertTrue(Files.readString(file).contains("\"expiresOn\": \"2026-02-28T12:00:00Z\""));
    }

    @Test
    void invalidIdentityDoesNotReplaceTheActiveName() {
        UUID uuid = UUID.randomUUID();
        try (UserProfileCache cache = new UserProfileCache(directory.resolve("usercache.json"), 1000, true)) {
            cache.add(uuid, "Original");

            assertThrows(IllegalArgumentException.class, () -> cache.add(uuid, " "));
            assertEquals(uuid, cache.find("Original").orElseThrow());
        }
    }

    @Test
    void failedFlushKeepsTheLiveCacheAndCanBeRetried() throws Exception {
        Path file = directory.resolve("usercache.json");
        UUID uuid = UUID.randomUUID();
        try (UserProfileCache cache = new UserProfileCache(file, 1000, true)) {
            cache.add(uuid, "Player");
            Files.delete(file);
            Files.createDirectory(file);
            Path blocker = file.resolve("blocker");
            Files.writeString(blocker, "prevent replacement");

            assertThrows(IllegalStateException.class, cache::flush);
            assertEquals(uuid, cache.find("Player").orElseThrow());

            Files.delete(blocker);
            Files.delete(file);
            cache.flush();
            assertTrue(Files.readString(file).contains(uuid.toString()));
        }
    }
}
