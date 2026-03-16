package org.cloudburstmc.server.permission;

import lombok.extern.log4j.Log4j2;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.permission.Permission;
import org.cloudburstmc.api.permission.PermissionDefault;
import org.cloudburstmc.server.CloudServer;

/**
 * Registers all built-in Cloudburst permission nodes at server startup.
 *
 * <p>All permissions are registered first, then a single {@code recalculatePermissibles()} is
 * issued on the root node so that online players are updated exactly once.</p>
 */
@Log4j2
public final class DefaultPermissions {

    public static final String ROOT = "cloudburst";

    private DefaultPermissions() {
    }

    /**
     * Registers {@code perm} with no parent.
     *
     * @param perm the permission to register
     * @return the registered permission (may be a pre-existing instance on reload)
     */
    public static CloudPermission registerPermission(CloudPermission perm) {
        return registerPermission(perm, null);
    }

    /**
     * Registers {@code perm}, optionally wiring it as a child of {@code parent}.
     *
     * <p>If the permission is already registered (e.g. on a server reload), the existing
     * registration is reused and the parent wiring is still applied.</p>
     *
     * @param perm   the permission to register
     * @param parent the parent permission to wire {@code perm} under, or {@code null} for none
     * @return the registered permission (maybe a pre-existing instance on reload)
     */
    public static CloudPermission registerPermission(CloudPermission perm, @Nullable CloudPermission parent) {
        Permission existing = CloudServer.getInstance().getPermissionManager().getPermission(perm.getName()).orElse(null);
        if (existing == null) {
            CloudServer.getInstance().getPermissionManager().addPermission(perm);
            existing = perm;
        }

        if (parent != null) {
            parent.getMutableChildren().put(existing.getName(), true);
        }

        if (existing instanceof CloudPermission cp) {
            return cp;
        }
        log.error("Permission '{}' was registered as {} instead of CloudPermission; startup permission tree wiring may be broken", existing.getName(), existing.getClass().getName());
        return perm;
    }

    public static void registerCorePermissions() {
        CloudPermission parent = registerPermission(new CloudPermission(ROOT, "Allows using all Cloudburst commands and utilities", PermissionDefault.OP));

        CloudPermission broadcasts = registerPermission(new CloudPermission(ROOT + ".broadcast", "Allows the user to receive all broadcast messages"), parent);
        registerPermission(new CloudPermission(ROOT + ".broadcast.admin", "Allows the user to receive administrative broadcasts", PermissionDefault.OP), broadcasts);
        registerPermission(new CloudPermission(ROOT + ".broadcast.user", "Allows the user to receive user broadcasts", PermissionDefault.TRUE), broadcasts);

        CloudPermission commands = registerPermission(new CloudPermission(ROOT + ".command", "Allows using all Cloudburst commands"), parent);

        CloudPermission whitelist = registerPermission(new CloudPermission(ROOT + ".command.whitelist", "Allows the user to modify the server whitelist", PermissionDefault.OP), commands);
        registerPermission(new CloudPermission(ROOT + ".command.whitelist.add", "Allows the user to add a player to the server whitelist", PermissionDefault.OP), whitelist);
        registerPermission(new CloudPermission(ROOT + ".command.whitelist.remove", "Allows the user to remove a player from the server whitelist", PermissionDefault.OP), whitelist);
        registerPermission(new CloudPermission(ROOT + ".command.whitelist.reload", "Allows the user to reload the server whitelist", PermissionDefault.OP), whitelist);
        registerPermission(new CloudPermission(ROOT + ".command.whitelist.enable", "Allows the user to enable the server whitelist", PermissionDefault.OP), whitelist);
        registerPermission(new CloudPermission(ROOT + ".command.whitelist.disable", "Allows the user to disable the server whitelist", PermissionDefault.OP), whitelist);
        registerPermission(new CloudPermission(ROOT + ".command.whitelist.list", "Allows the user to list all players on the server whitelist", PermissionDefault.OP), whitelist);

        CloudPermission ban = registerPermission(new CloudPermission(ROOT + ".command.ban", "Allows the user to ban people", PermissionDefault.OP), commands);
        registerPermission(new CloudPermission(ROOT + ".command.ban.player", "Allows the user to ban players", PermissionDefault.OP), ban);
        registerPermission(new CloudPermission(ROOT + ".command.ban.ip", "Allows the user to ban IP addresses", PermissionDefault.OP), ban);
        registerPermission(new CloudPermission(ROOT + ".command.ban.list", "Allows the user to list all banned IPs or players", PermissionDefault.OP), ban);

        CloudPermission unban = registerPermission(new CloudPermission(ROOT + ".command.unban", "Allows the user to unban people", PermissionDefault.OP), commands);
        registerPermission(new CloudPermission(ROOT + ".command.unban.player", "Allows the user to unban players", PermissionDefault.OP), unban);
        registerPermission(new CloudPermission(ROOT + ".command.unban.ip", "Allows the user to unban IP addresses", PermissionDefault.OP), unban);

        CloudPermission op = registerPermission(new CloudPermission(ROOT + ".command.op", "Allows the user to change operators", PermissionDefault.OP), commands);
        registerPermission(new CloudPermission(ROOT + ".command.op.give", "Allows the user to give a player operator status", PermissionDefault.OP), op);
        registerPermission(new CloudPermission(ROOT + ".command.op.take", "Allows the user to take a player's operator status", PermissionDefault.OP), op);

        CloudPermission save = registerPermission(new CloudPermission(ROOT + ".command.save", "Allows the user to save the worlds", PermissionDefault.OP), commands);
        registerPermission(new CloudPermission(ROOT + ".command.save.enable", "Allows the user to enable automatic saving", PermissionDefault.OP), save);
        registerPermission(new CloudPermission(ROOT + ".command.save.disable", "Allows the user to disable automatic saving", PermissionDefault.OP), save);
        registerPermission(new CloudPermission(ROOT + ".command.save.perform", "Allows the user to perform a manual save", PermissionDefault.OP), save);

        CloudPermission time = registerPermission(new CloudPermission(ROOT + ".command.time", "Allows the user to alter the time", PermissionDefault.OP), commands);
        registerPermission(new CloudPermission(ROOT + ".command.time.add", "Allows the user to fast-forward time", PermissionDefault.OP), time);
        registerPermission(new CloudPermission(ROOT + ".command.time.set", "Allows the user to change the time", PermissionDefault.OP), time);
        registerPermission(new CloudPermission(ROOT + ".command.time.start", "Allows the user to restart the time", PermissionDefault.OP), time);
        registerPermission(new CloudPermission(ROOT + ".command.time.stop", "Allows the user to stop the time", PermissionDefault.OP), time);
        registerPermission(new CloudPermission(ROOT + ".command.time.query", "Allows the user to query the time", PermissionDefault.OP), time);

        CloudPermission kill = registerPermission(new CloudPermission(ROOT + ".command.kill", "Allows the user to kill players", PermissionDefault.OP), commands);
        registerPermission(new CloudPermission(ROOT + ".command.kill.self", "Allows the user to commit suicide", PermissionDefault.TRUE), kill);
        registerPermission(new CloudPermission(ROOT + ".command.kill.other", "Allows the user to kill other players", PermissionDefault.OP), kill);

        CloudPermission gamemode = registerPermission(new CloudPermission(ROOT + ".command.gamemode", "Allows the user to change the gamemode of players", PermissionDefault.OP), commands);
        registerPermission(new CloudPermission(ROOT + ".command.gamemode.survival", "Allows the user to change the gamemode to survival", PermissionDefault.OP), gamemode);
        registerPermission(new CloudPermission(ROOT + ".command.gamemode.creative", "Allows the user to change the gamemode to creative", PermissionDefault.OP), gamemode);
        registerPermission(new CloudPermission(ROOT + ".command.gamemode.adventure", "Allows the user to change the gamemode to adventure", PermissionDefault.OP), gamemode);
        registerPermission(new CloudPermission(ROOT + ".command.gamemode.spectator", "Allows the user to change the gamemode to spectator", PermissionDefault.OP), gamemode);
        registerPermission(new CloudPermission(ROOT + ".command.gamemode.other", "Allows the user to change the gamemode of other players", PermissionDefault.OP), gamemode);

        registerPermission(new CloudPermission(ROOT + ".command.me", "Allows the user to perform a chat action", PermissionDefault.TRUE), commands);
        registerPermission(new CloudPermission(ROOT + ".command.tell", "Allows the user to privately message another player", PermissionDefault.TRUE), commands);
        registerPermission(new CloudPermission(ROOT + ".command.say", "Allows the user to talk as the console", PermissionDefault.OP), commands);
        registerPermission(new CloudPermission(ROOT + ".command.give", "Allows the user to give items to players", PermissionDefault.OP), commands);
        registerPermission(new CloudPermission(ROOT + ".command.effect", "Allows the user to give/take potion effects", PermissionDefault.OP), commands);
        registerPermission(new CloudPermission(ROOT + ".command.particle", "Allows the user to create particle effects", PermissionDefault.OP), commands);
        registerPermission(new CloudPermission(ROOT + ".command.teleport", "Allows the user to teleport players", PermissionDefault.OP), commands);
        registerPermission(new CloudPermission(ROOT + ".command.kick", "Allows the user to kick players", PermissionDefault.OP), commands);
        registerPermission(new CloudPermission(ROOT + ".command.stop", "Allows the user to stop the server", PermissionDefault.OP), commands);
        registerPermission(new CloudPermission(ROOT + ".command.list", "Allows the user to list all online players", PermissionDefault.OP), commands);
        registerPermission(new CloudPermission(ROOT + ".command.help", "Allows the user to view the help menu", PermissionDefault.TRUE), commands);
        registerPermission(new CloudPermission(ROOT + ".command.plugins", "Allows the user to view the list of plugins", PermissionDefault.OP), commands);
        registerPermission(new CloudPermission(ROOT + ".command.reload", "Allows the user to reload the server settings", PermissionDefault.OP), commands);
        registerPermission(new CloudPermission(ROOT + ".command.version", "Allows the user to view the version of the server", PermissionDefault.TRUE), commands);
        registerPermission(new CloudPermission(ROOT + ".command.defaultgamemode", "Allows the user to change the default gamemode", PermissionDefault.OP), commands);
        registerPermission(new CloudPermission(ROOT + ".command.seed", "Allows the user to view the seed of the world", PermissionDefault.OP), commands);
        registerPermission(new CloudPermission(ROOT + ".command.status", "Allows the user to view the server performance", PermissionDefault.OP), commands);
        registerPermission(new CloudPermission(ROOT + ".command.gc", "Allows the user to fire garbage collection tasks", PermissionDefault.OP), commands);
        registerPermission(new CloudPermission(ROOT + ".command.gamerule", "Sets or queries a game rule value", PermissionDefault.OP), commands);
        registerPermission(new CloudPermission(ROOT + ".command.timings", "Allows the user to record timings for all plugin events", PermissionDefault.OP), commands);
        registerPermission(new CloudPermission(ROOT + ".command.title", "Allows the user to send titles to players", PermissionDefault.OP), commands);
        registerPermission(new CloudPermission(ROOT + ".command.spawnpoint", "Allows the user to change a player's spawnpoint", PermissionDefault.OP), commands);
        registerPermission(new CloudPermission(ROOT + ".command.setworldspawn", "Allows the user to change the world spawn", PermissionDefault.OP), commands);
        registerPermission(new CloudPermission(ROOT + ".command.weather", "Allows the user to change the weather", PermissionDefault.OP), commands);
        registerPermission(new CloudPermission(ROOT + ".command.xp", "Allows the user to give experience", PermissionDefault.OP), commands);
        registerPermission(new CloudPermission(ROOT + ".command.enchant", "Allows the user to enchant items", PermissionDefault.OP), commands);
        registerPermission(new CloudPermission(ROOT + ".command.difficulty", "Allows the user to change the difficulty", PermissionDefault.OP), commands);

        CloudPermission debug = registerPermission(new CloudPermission(ROOT + ".command.debug", "Allows the user to use debug commands", PermissionDefault.OP), commands);
        registerPermission(new CloudPermission(ROOT + ".command.debug.perform", "Allows the user to use the debug paste command", PermissionDefault.OP), debug);

        registerPermission(new CloudPermission(ROOT + ".textcolor", "Allows the user to write colored text", PermissionDefault.OP), parent);

        parent.recalculatePermissibles();
    }
}
