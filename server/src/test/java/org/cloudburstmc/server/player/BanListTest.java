package org.cloudburstmc.server.player;

import com.google.common.net.InetAddresses;
import org.cloudburstmc.api.ban.BanEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BanListTest {

    @TempDir
    Path directory;

    @Test
    void uuidBansPreserveMetadataAndDoNotFollowNames() {
        Path file = directory.resolve("banned-players.json");
        UUID uuid = UUID.randomUUID();
        UserBanList list = new UserBanList(file, ignored -> "Player");
        BanEntry<UUID> entry = list.addBan(uuid, "Reason", null, "Console");
        UserBanList reloaded = new UserBanList(file, ignored -> "Renamed");

        assertEquals(entry, reloaded.getBanEntry(uuid));
        assertEquals("Player", reloaded.findName(uuid).orElseThrow());
        assertTrue(reloaded.findIdentities("player").contains(uuid));
        assertFalse(reloaded.isBanned(UUID.randomUUID()));

        reloaded.pardon(uuid);
        assertFalse(new UserBanList(file, ignored -> null).isBanned(uuid));
    }

    @Test
    void refreshingABanPreservesItsNameWhenTheIdentityCacheHasExpired() {
        Path file = directory.resolve("banned-players.json");
        UUID uuid = UUID.randomUUID();
        new UserBanList(file, ignored -> "Player").addBan(uuid, "Original", null, "Console");
        UserBanList reloaded = new UserBanList(file, ignored -> null);

        reloaded.addBan(uuid, "Updated", null, "Console");

        UserBanList saved = new UserBanList(file, ignored -> null);
        assertEquals("Player", saved.findName(uuid).orElseThrow());
        assertTrue(saved.findIdentities("PLAYER").contains(uuid));
        BanEntry<UUID> entry = saved.getBanEntry(uuid);
        assertNotNull(entry);
        assertEquals("Updated", entry.reason());
    }

    @Test
    void expiredBansAreExcludedFromQueriesAndSnapshots() {
        UserBanList list = new UserBanList(directory.resolve("banned-players.json"), ignored -> null);
        UUID uuid = UUID.randomUUID();
        list.addBan(uuid, null, Instant.parse("2000-01-01T00:00:00Z"), null);

        assertFalse(list.isBanned(uuid));
        assertTrue(list.getBanEntries().isEmpty());
    }

    @Test
    void ipBansCanonicalizeAddressesWithoutNameResolution() throws Exception {
        Path file = directory.resolve("banned-ips.json");
        IpBanList list = new IpBanList(file);
        BanEntry<InetAddress> entry = list.addBan(InetAddresses.forString("2001:db8:0:0:0:0:0:1"),
                "Reason", Instant.parse("2999-01-01T00:00:00Z"), null);

        assertEquals(entry, new IpBanList(file).getBanEntry(InetAddresses.forString("2001:db8::1")));
        assertTrue(Files.readString(file).contains("\"created\": \"" + entry.created() + "\""));
        assertTrue(Files.readString(file).contains("\"expires\": \"2999-01-01T00:00:00Z\""));
        assertFalse(list.isBanned(InetAddresses.forString("2001:db8::2")));
    }

    @Test
    void invalidIpBanTimestampDoesNotReplaceActiveBans() throws Exception {
        Path file = directory.resolve("banned-ips.json");
        IpBanList list = new IpBanList(file);
        InetAddress address = InetAddresses.forString("192.0.2.1");
        BanEntry<InetAddress> entry = list.addBan(address, "Reason", null, "Console");
        Files.writeString(file, Files.readString(file).replace(entry.created().toString(), "invalid-timestamp"));

        assertThrows(IllegalArgumentException.class, list::reload);
        assertEquals(entry, list.getBanEntry(address));
    }

    @Test
    void invalidPlayerBanTimestampDoesNotReplaceActiveBans() throws Exception {
        Path file = directory.resolve("banned-players.json");
        UserBanList list = new UserBanList(file, ignored -> "Player");
        UUID uuid = UUID.randomUUID();
        BanEntry<UUID> entry = list.addBan(uuid, "Reason", null, "Console");
        Files.writeString(file, Files.readString(file).replace(entry.created().toString(), "invalid-timestamp"));

        assertThrows(IllegalArgumentException.class, list::reload);
        assertEquals(entry, list.getBanEntry(uuid));
    }

    @Test
    void invalidReloadPreservesActiveBans() throws Exception {
        Path file = directory.resolve("banned-players.json");
        UserBanList list = new UserBanList(file, ignored -> null);
        UUID uuid = UUID.randomUUID();
        list.addBan(uuid, null, null, null);
        Files.writeString(file, "[{\"name\":\"LegacyNameOnlyBan\"}]");

        assertThrows(IllegalArgumentException.class, list::reload);
        assertTrue(list.isBanned(uuid));
    }
}
