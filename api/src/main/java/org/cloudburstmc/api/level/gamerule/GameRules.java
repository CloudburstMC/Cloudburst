package org.cloudburstmc.api.level.gamerule;

import lombok.experimental.UtilityClass;

/**
 * Canonical vanilla game-rule definitions used as registry keys.
 */
@UtilityClass
public class GameRules {
    public static final BooleanGameRule COMMAND_BLOCK_OUTPUT = BooleanGameRule.of("commandblockoutput", true, true);
    public static final BooleanGameRule COMMAND_BLOCKS_ENABLED = BooleanGameRule.of("commandblocksenabled", true, true);
    public static final BooleanGameRule DO_DAYLIGHT_CYCLE = BooleanGameRule.of("dodaylightcycle", true, true);
    public static final BooleanGameRule DO_ENTITY_DROPS = BooleanGameRule.of("doentitydrops", true, true);
    public static final BooleanGameRule DO_FIRE_TICK = BooleanGameRule.of("dofiretick", true);
    public static final BooleanGameRule DO_IMMEDIATE_RESPAWN = BooleanGameRule.of("doimmediaterespawn", false);
    public static final BooleanGameRule DO_INSOMNIA = BooleanGameRule.of("doinsomnia", true, true);
    public static final BooleanGameRule DO_LIMITED_CRAFTING = BooleanGameRule.of("dolimitedcrafting", false);
    public static final BooleanGameRule DO_MOB_LOOT = BooleanGameRule.of("domobloot", true);
    public static final BooleanGameRule DO_MOB_SPAWNING = BooleanGameRule.of("domobspawning", true, true);
    public static final BooleanGameRule DO_TILE_DROPS = BooleanGameRule.of("dotiledrops", true);
    public static final BooleanGameRule DO_WEATHER_CYCLE = BooleanGameRule.of("doweathercycle", true, true);
    public static final BooleanGameRule DROWNING_DAMAGE = BooleanGameRule.of("drowningdamage", true, true);
    public static final BooleanGameRule FALL_DAMAGE = BooleanGameRule.of("falldamage", true, true);
    public static final BooleanGameRule FIRE_DAMAGE = BooleanGameRule.of("firedamage", true, true);
    public static final BooleanGameRule FREEZE_DAMAGE = BooleanGameRule.of("freezedamage", true, true);
    public static final BooleanGameRule KEEP_INVENTORY = BooleanGameRule.of("keepinventory", false, true);
    public static final BooleanGameRule MOB_GRIEFING = BooleanGameRule.of("mobgriefing", true, true);
    public static final BooleanGameRule NATURAL_REGENERATION = BooleanGameRule.of("naturalregeneration", true);
    public static final EnumGameRule<PlayerWaypointVisibility> PLAYER_WAYPOINTS = EnumGameRule.of("playerwaypoints", PlayerWaypointVisibility.class, PlayerWaypointVisibility.EVERYONE, PlayerWaypointVisibility.EVERYONE, PlayerWaypointVisibility.OFF);
    public static final BooleanGameRule PROJECTILES_CAN_BREAK_BLOCKS = BooleanGameRule.of("projectilescanbreakblocks", true);
    public static final BooleanGameRule PVP = BooleanGameRule.of("pvp", true);
    public static final BooleanGameRule RESPAWN_BLOCKS_EXPLODE = BooleanGameRule.of("respawnblocksexplode", true);
    public static final BooleanGameRule SEND_COMMAND_FEEDBACK = BooleanGameRule.of("sendcommandfeedback", true, true);
    public static final BooleanGameRule SHOW_BORDER_EFFECT = BooleanGameRule.of("showbordereffect", true, true);
    public static final BooleanGameRule SHOW_COORDINATES = BooleanGameRule.of("showcoordinates", false);
    public static final BooleanGameRule SHOW_DAYS_PLAYED = BooleanGameRule.of("showdaysplayed", false);
    public static final BooleanGameRule SHOW_DEATH_MESSAGES = BooleanGameRule.of("showdeathmessages", true);
    public static final BooleanGameRule SHOW_RECIPE_MESSAGES = BooleanGameRule.of("showrecipemessages", true);
    public static final BooleanGameRule SHOW_TAGS = BooleanGameRule.of("showtags", true);
    public static final BooleanGameRule TNT_EXPLODES = BooleanGameRule.of("tntexplodes", true);
    public static final BooleanGameRule TNT_EXPLOSION_DROP_DECAY = BooleanGameRule.of("tntexplosiondropdecay", false);
    public static final IntegerGameRule FUNCTION_COMMAND_LIMIT = IntegerGameRule.of("functioncommandlimit", 10000, true);
    public static final IntegerGameRule MAX_COMMAND_CHAIN_LENGTH = IntegerGameRule.of("maxcommandchainlength", 65535, true);
    public static final IntegerGameRule PLAYERS_SLEEPING_PERCENTAGE = IntegerGameRule.of("playerssleepingpercentage", 100);
    public static final IntegerGameRule RANDOM_TICK_SPEED = IntegerGameRule.of("randomtickspeed", 1, true);
    public static final IntegerGameRule SPAWN_RADIUS = IntegerGameRule.of("spawnradius", 10);
}
