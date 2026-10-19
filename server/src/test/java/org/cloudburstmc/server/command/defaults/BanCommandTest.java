package org.cloudburstmc.server.command.defaults;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import org.cloudburstmc.api.Server;
import org.cloudburstmc.api.ban.BanEntry;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.command.CommandSourceStack;
import org.cloudburstmc.api.command.Commands;
import org.cloudburstmc.api.command.argument.CommandArguments;
import org.cloudburstmc.api.player.OfflinePlayer;
import org.cloudburstmc.server.player.UserBanList;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BanCommandTest {

    @TempDir
    Path directory;

    @Test
    void consoleBansKnownIdentityAndSavesReasonAndIssuer() {
        UUID uuid = UUID.randomUUID();
        UserBanList bans = bans();

        assertEquals(1, execute("ban Player Repeated griefing", uuid, bans));

        BanEntry<UUID> entry = bans.getBanEntry(uuid);
        assertNotNull(entry);
        assertEquals("Repeated griefing", entry.reason());
        assertEquals("CONSOLE", entry.source());
        assertTrue(bans().isBanned(uuid));
    }

    @Test
    void existingActiveBanIsNotReplaced() {
        UUID uuid = UUID.randomUUID();
        UserBanList bans = bans();
        bans.addBan(uuid, "Original", null, "Admin");

        assertEquals(0, execute("ban Player Replacement", uuid, bans));
        BanEntry<UUID> entry = bans.getBanEntry(uuid);
        assertNotNull(entry);
        assertEquals("Original", entry.reason());
        assertEquals("Admin", entry.source());
    }

    @Test
    void unknownNameDoesNotCreateABan() {
        UserBanList bans = bans();

        assertEquals(0, execute("ban Unknown", null, bans));
        assertTrue(bans.getBanEntries().isEmpty());
    }

    private UserBanList bans() {
        return new UserBanList(directory.resolve("banned-players.json"), uuid -> "Player");
    }

    private int execute(String input, UUID uuid, UserBanList bans) {
        OfflinePlayer player = InterfaceProxy.create(OfflinePlayer.class, uuid == null ? Map.of() : Map.of("getUniqueId", uuid, "getName", "Player", "getPlayer", Optional.empty()));
        Server server = InterfaceProxy.create(Server.class, Map.of("getOfflinePlayer", player, "getPlayerBanList", bans));
        CommandSender console = InterfaceProxy.create(CommandSender.class, Map.of("getServer", server, "getName", "CONSOLE"));
        CommandSourceStack source = InterfaceProxy.create(CommandSourceStack.class, Map.of("sender", console));
        BanCommand command = new BanCommand();
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("ban");
        command.configure(root, "ban", InterfaceProxy.create(CommandArguments.class));
        dispatcher.register(root);
        return command.execute(dispatcher.parse(input, source).getContext().build(input));
    }
}
