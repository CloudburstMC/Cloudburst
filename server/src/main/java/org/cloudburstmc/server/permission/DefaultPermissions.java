package org.cloudburstmc.server.permission;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.command.CommandPermissions;
import org.cloudburstmc.api.permission.Permission;
import org.cloudburstmc.api.permission.PermissionDefault;
import org.cloudburstmc.api.permission.PermissionManager;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Registers all built-in Cloudburst permission definitions at server startup.
 */
@UtilityClass
public class DefaultPermissions {

    private static final String ROOT = "cloudburst";

    /**
     * Registers the complete built-in permission graph.
     *
     * @param manager permission registry receiving the definitions
     */
    public static void registerCorePermissions(PermissionManager manager) {
        Objects.requireNonNull(manager, "manager");
        DefinitionSet definitions = new DefinitionSet();
        Permission parent = definitions.add(new Permission(ROOT, "Allows using all Cloudburst commands and utilities", PermissionDefault.OPERATORS));

        Permission broadcasts = definitions.add(new Permission(ROOT + ".broadcast", "Allows the user to receive all broadcast messages"), parent);
        definitions.add(new Permission(ROOT + ".broadcast.admin", "Allows the user to receive administrative broadcasts", PermissionDefault.OPERATORS), broadcasts);
        definitions.add(new Permission(ROOT + ".broadcast.user", "Allows the user to receive user broadcasts", PermissionDefault.EVERYONE), broadcasts);

        Permission commands = definitions.add(new Permission(ROOT + ".command", "Allows using all Cloudburst commands"), parent);
        definitions.add(new Permission(CommandPermissions.TARGET_SELECTORS,
                "Allows target selectors in commands", PermissionDefault.OPERATORS), commands);

        Permission whitelist = definitions.add(new Permission(ROOT + ".command.whitelist", "Allows the user to modify the server whitelist", PermissionDefault.OPERATORS), commands);
        definitions.add(new Permission(ROOT + ".command.whitelist.add", "Allows the user to add a player to the server whitelist", PermissionDefault.OPERATORS), whitelist);
        definitions.add(new Permission(ROOT + ".command.whitelist.remove", "Allows the user to remove a player from the server whitelist", PermissionDefault.OPERATORS), whitelist);
        definitions.add(new Permission(ROOT + ".command.whitelist.reload", "Allows the user to reload the server whitelist", PermissionDefault.OPERATORS), whitelist);
        definitions.add(new Permission(ROOT + ".command.whitelist.enable", "Allows the user to enable the server whitelist", PermissionDefault.OPERATORS), whitelist);
        definitions.add(new Permission(ROOT + ".command.whitelist.disable", "Allows the user to disable the server whitelist", PermissionDefault.OPERATORS), whitelist);
        definitions.add(new Permission(ROOT + ".command.whitelist.list", "Allows the user to list all players on the server whitelist", PermissionDefault.OPERATORS), whitelist);

        Permission op = definitions.add(new Permission(ROOT + ".command.op", "Allows the user to change operators", PermissionDefault.OPERATORS), commands);
        definitions.add(new Permission(ROOT + ".command.op.give", "Allows the user to give a player operator status", PermissionDefault.OPERATORS), op);
        definitions.add(new Permission(ROOT + ".command.op.take", "Allows the user to take a player's operator status", PermissionDefault.OPERATORS), op);

        Permission save = definitions.add(new Permission(ROOT + ".command.save", "Allows the user to save the worlds", PermissionDefault.OPERATORS), commands);
        definitions.add(new Permission(ROOT + ".command.save.enable", "Allows the user to enable automatic saving", PermissionDefault.OPERATORS), save);
        definitions.add(new Permission(ROOT + ".command.save.disable", "Allows the user to disable automatic saving", PermissionDefault.OPERATORS), save);
        definitions.add(new Permission(ROOT + ".command.save.perform", "Allows the user to perform a manual save", PermissionDefault.OPERATORS), save);

        Permission time = definitions.add(new Permission(ROOT + ".command.time", "Allows the user to alter the time", PermissionDefault.OPERATORS), commands);
        definitions.add(new Permission(ROOT + ".command.time.add", "Allows the user to fast-forward time", PermissionDefault.OPERATORS), time);
        definitions.add(new Permission(ROOT + ".command.time.set", "Allows the user to change the time", PermissionDefault.OPERATORS), time);
        definitions.add(new Permission(ROOT + ".command.time.start", "Allows the user to restart the time", PermissionDefault.OPERATORS), time);
        definitions.add(new Permission(ROOT + ".command.time.stop", "Allows the user to stop the time", PermissionDefault.OPERATORS), time);
        definitions.add(new Permission(ROOT + ".command.time.query", "Allows the user to query the time", PermissionDefault.OPERATORS), time);

        Permission kill = definitions.add(new Permission(ROOT + ".command.kill", "Allows the user to kill players", PermissionDefault.OPERATORS), commands);
        definitions.add(new Permission(ROOT + ".command.kill.self", "Allows the user to kill themselves", PermissionDefault.OPERATORS), kill);
        definitions.add(new Permission(ROOT + ".command.kill.other", "Allows the user to kill other players", PermissionDefault.OPERATORS), kill);

        Permission gamemode = definitions.add(new Permission(ROOT + ".command.gamemode", "Allows the user to change the gamemode of players", PermissionDefault.OPERATORS), commands);
        definitions.add(new Permission(ROOT + ".command.gamemode.other", "Allows the user to change the gamemode of other players", PermissionDefault.OPERATORS), gamemode);

        definitions.add(new Permission(ROOT + ".command.me", "Allows the user to perform a chat action", PermissionDefault.EVERYONE), commands);
        definitions.add(new Permission(ROOT + ".command.tell", "Allows the user to privately message another player", PermissionDefault.EVERYONE), commands);
        definitions.add(new Permission(ROOT + ".command.say", "Allows the user to talk as the console", PermissionDefault.OPERATORS), commands);
        definitions.add(new Permission(ROOT + ".command.give", "Allows the user to give items to players", PermissionDefault.OPERATORS), commands);
        definitions.add(new Permission(ROOT + ".command.effect", "Allows the user to give/take potion effects", PermissionDefault.OPERATORS), commands);
        definitions.add(new Permission(ROOT + ".command.particle", "Allows the user to create particle effects", PermissionDefault.OPERATORS), commands);
        definitions.add(new Permission(ROOT + ".command.setblock", "Allows the user to change blocks", PermissionDefault.OPERATORS), commands);
        definitions.add(new Permission(ROOT + ".command.teleport", "Allows the user to teleport players", PermissionDefault.OPERATORS), commands);
        definitions.add(new Permission(ROOT + ".command.kick", "Allows the user to kick players", PermissionDefault.OPERATORS), commands);
        definitions.add(new Permission(ROOT + ".command.stop", "Allows the user to stop the server", PermissionDefault.OPERATORS), commands);
        definitions.add(new Permission(ROOT + ".command.list", "Allows the user to list all online players", PermissionDefault.OPERATORS), commands);
        definitions.add(new Permission(ROOT + ".command.help", "Allows the user to view the help menu", PermissionDefault.EVERYONE), commands);
        definitions.add(new Permission(ROOT + ".command.plugins", "Allows the user to view the list of plugins", PermissionDefault.OPERATORS), commands);
        definitions.add(new Permission(ROOT + ".command.version", "Allows the user to view the version of the server", PermissionDefault.EVERYONE), commands);
        definitions.add(new Permission(ROOT + ".command.defaultgamemode", "Allows the user to change the default gamemode", PermissionDefault.OPERATORS), commands);
        definitions.add(new Permission(ROOT + ".command.seed", "Allows the user to view the seed of the world", PermissionDefault.OPERATORS), commands);
        definitions.add(new Permission(ROOT + ".command.status", "Allows the user to view the server performance", PermissionDefault.OPERATORS), commands);
        definitions.add(new Permission(ROOT + ".command.gc", "Allows the user to fire garbage collection tasks", PermissionDefault.OPERATORS), commands);
        definitions.add(new Permission(ROOT + ".command.gamerule", "Sets or queries a game rule value", PermissionDefault.OPERATORS), commands);
        definitions.add(new Permission(ROOT + ".command.timings", "Allows the user to record timings for all plugin events", PermissionDefault.OPERATORS), commands);
        definitions.add(new Permission(ROOT + ".command.title", "Allows the user to send titles to players", PermissionDefault.OPERATORS), commands);
        definitions.add(new Permission(ROOT + ".command.spawnpoint", "Allows the user to change a player's spawnpoint", PermissionDefault.OPERATORS), commands);
        definitions.add(new Permission(ROOT + ".command.setworldspawn", "Allows the user to change the world spawn", PermissionDefault.OPERATORS), commands);
        definitions.add(new Permission(ROOT + ".command.weather", "Allows the user to change the weather", PermissionDefault.OPERATORS), commands);
        definitions.add(new Permission(ROOT + ".command.xp", "Allows the user to give experience", PermissionDefault.OPERATORS), commands);
        definitions.add(new Permission(ROOT + ".command.enchant", "Allows the user to enchant items", PermissionDefault.OPERATORS), commands);
        definitions.add(new Permission(ROOT + ".command.difficulty", "Allows the user to change the difficulty", PermissionDefault.OPERATORS), commands);

        Permission debug = definitions.add(new Permission(ROOT + ".command.debug", "Allows the user to use debug commands", PermissionDefault.OPERATORS), commands);
        definitions.add(new Permission(ROOT + ".command.debug.perform", "Allows the user to use the debug paste command", PermissionDefault.OPERATORS), debug);

        definitions.add(new Permission(ROOT + ".textcolor", "Allows the user to write colored text", PermissionDefault.OPERATORS), parent);
        definitions.register(manager);
    }

    private static final class DefinitionSet {

        private final Map<String, Permission> definitions = new LinkedHashMap<>();

        private Permission add(Permission permission) {
            if (this.definitions.putIfAbsent(permission.name(), permission) != null) {
                throw new IllegalArgumentException("Duplicate built-in permission: " + permission.name());
            }
            return permission;
        }

        private Permission add(Permission permission, Permission parent) {
            this.add(permission);
            Permission currentParent = this.definitions.get(parent.name());
            if (currentParent == null) {
                throw new IllegalArgumentException("Unknown parent permission: " + parent.name());
            }
            LinkedHashMap<String, Boolean> children = new LinkedHashMap<>(currentParent.children());
            children.put(permission.name(), true);
            this.definitions.put(parent.name(), currentParent.toBuilder().children(children).build());
            return permission;
        }

        private void register(PermissionManager manager) {
            manager.registerAll(this.definitions.values());
        }
    }
}
