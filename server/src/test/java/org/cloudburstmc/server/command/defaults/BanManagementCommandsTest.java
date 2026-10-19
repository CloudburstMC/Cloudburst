package org.cloudburstmc.server.command.defaults;

import com.google.common.net.InetAddresses;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.kyori.adventure.text.Component;
import org.cloudburstmc.api.Server;
import org.cloudburstmc.api.ban.BanEntry;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.command.CommandSourceStack;
import org.cloudburstmc.api.command.Commands;
import org.cloudburstmc.api.command.argument.CommandArguments;
import org.cloudburstmc.api.player.KickReason;
import org.cloudburstmc.api.player.OfflinePlayer;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.server.command.ServerCommand;
import org.cloudburstmc.server.player.IpBanList;
import org.cloudburstmc.server.player.UserBanList;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BanManagementCommandsTest {

    @TempDir
    Path directory;

    @Test
    void literalIpBanPersistsAndCannotReplaceAnActiveBan() {
        IpBanList ips = ips();
        Server server = server(ips, players(), Map.of(), UUID.randomUUID());
        BanIpCommand command = new BanIpCommand();

        assertEquals(0, command.execute(context(command, "ban-ip 2001:db8::1 Reason with spaces", server)));
        assertTrue(ips().isBanned(InetAddresses.forString("2001:db8::1")));
        BanEntry<InetAddress> entry = ips.getBanEntry(InetAddresses.forString("2001:db8::1"));
        assertNotNull(entry);
        assertEquals("CONSOLE", entry.source());
        assertEquals(0, command.execute(context(command, "ban-ip 2001:db8::1 Replacement", server)));
        assertEquals(entry, ips.getBanEntry(InetAddresses.forString("2001:db8::1")));
        assertEquals("Reason with spaces", entry.reason());
    }

    @Test
    void onlineNameBansAndKicksEveryPlayerSharingTheAddress() {
        IpBanList ips = ips();
        List<String> kicked = new ArrayList<>();
        Server server = server(ips, players(), Map.of(
                UUID.randomUUID(), player("First", "192.0.2.1", 1000, kicked),
                UUID.randomUUID(), player("Second", "192.0.2.1", 2000, kicked),
                UUID.randomUUID(), player("Other", "192.0.2.2", 1000, kicked)), UUID.randomUUID());
        BanIpCommand command = new BanIpCommand();

        assertEquals(2, command.execute(context(command, "ban-ip first", server)));
        assertEquals(2, kicked.size());
        assertTrue(kicked.containsAll(List.of("First", "Second")));
        assertTrue(ips.isBanned(InetAddresses.forString("192.0.2.1")));
        assertFalse(ips.isBanned(InetAddresses.forString("192.0.2.2")));
    }

    @Test
    void hostnameIsRejectedWithoutCreatingABan() {
        IpBanList ips = ips();
        BanIpCommand command = new BanIpCommand();
        Server server = server(ips, players(), Map.of(), UUID.randomUUID());

        assertEquals(0, command.execute(context(command, "ban-ip localhost", server)));
        assertTrue(ips.getBanEntries().isEmpty());
    }

    @Test
    void ipPardonRequiresAnActiveLiteralAddress() {
        IpBanList ips = ips();
        ips.addBan(InetAddresses.forString("2001:db8::1"), null, null, null);
        PardonIpCommand command = new PardonIpCommand();
        Server server = server(ips, players(), Map.of(), UUID.randomUUID());

        assertEquals(0, command.execute(context(command, "pardon-ip localhost", server)));
        assertEquals(1, command.execute(context(command, "pardon-ip 2001:db8:0:0:0:0:0:1", server)));
        assertEquals(0, command.execute(context(command, "pardon-ip 2001:db8::1", server)));
        assertTrue(ips().getBanEntries().isEmpty());
    }

    @Test
    void playerPardonDoesNotRemoveIpBans() {
        UUID uuid = UUID.randomUUID();
        UserBanList players = players();
        IpBanList ips = ips();
        players.addBan(uuid, null, null, null);
        ips.addBan(InetAddresses.forString("192.0.2.1"), null, null, null);
        Server server = server(ips, players, Map.of(), uuid);
        PardonCommand command = new PardonCommand();

        assertEquals(1, command.execute(context(command, "pardon " + uuid, server)));
        assertFalse(players.isBanned(uuid));
        assertTrue(ips.isBanned(InetAddresses.forString("192.0.2.1")));
        assertEquals(0, command.execute(context(command, "pardon Player", server)));
    }

    @Test
    void banListCountsOnlyActiveBans() {
        UUID uuid = UUID.randomUUID();
        UserBanList players = players();
        IpBanList ips = ips();
        players.addBan(uuid, null, null, null);
        players.addBan(UUID.randomUUID(), null, Instant.EPOCH, null);
        ips.addBan(InetAddresses.forString("192.0.2.1"), null, null, null);
        BanListCommand command = new BanListCommand();

        assertEquals(2, command.execute(context(command, "banlist", server(ips, players, Map.of(), uuid))));
    }

    @Test
    void banListHasSeparatePlayerAndIpBranches() {
        BanListCommand command = new BanListCommand();
        LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal("banlist");
        command.configure(builder, "banlist", InterfaceProxy.create(CommandArguments.class));

        assertNotNull(builder.build().getChild("players").getCommand());
        assertNotNull(builder.build().getChild("ips").getCommand());
    }

    private IpBanList ips() {
        return new IpBanList(directory.resolve("banned-ips.json"));
    }

    private UserBanList players() {
        return new UserBanList(directory.resolve("banned-players.json"), uuid -> "Player");
    }

    private Server server(IpBanList ips, UserBanList players, Map<UUID, Player> online, UUID uuid) {
        OfflinePlayer offline = InterfaceProxy.create(OfflinePlayer.class, Map.of("getUniqueId", uuid, "getName", "Player"));
        return InterfaceProxy.create(Server.class, Map.of("getIpBanList", ips, "getPlayerBanList", players, "getOnlinePlayers", online, "getOfflinePlayer", offline));
    }

    private CommandContext<CommandSourceStack> context(ServerCommand command, String input, Server server) {
        CommandSender console = InterfaceProxy.create(CommandSender.class, Map.of("getServer", server, "getName", "CONSOLE"));
        CommandSourceStack source = InterfaceProxy.create(CommandSourceStack.class, Map.of("sender", console));
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(command.getName());
        command.configure(root, command.getName(), InterfaceProxy.create(CommandArguments.class));
        dispatcher.register(root);
        return dispatcher.parse(input, source).getContext().build(input);
    }

    private Player player(String name, String ip, int port, List<String> kicked) {
        InetSocketAddress address = new InetSocketAddress(InetAddresses.forString(ip), port);
        return (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "getName" -> name;
                    case "getAddress" -> address;
                    case "kick" -> {
                        assertTrue(ips().isBanned(address.getAddress()));
                        assertInstanceOf(Component.class, arguments[0]);
                        assertEquals(KickReason.IP_BANNED, arguments[1]);
                        kicked.add(name);
                        yield null;
                    }
                    default -> throw new AssertionError("Unexpected player method: " + method.getName());
                });
    }
}
