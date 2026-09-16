package org.cloudburstmc.server.command.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.command.CommandSourceStack;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.player.GameMode;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CloudTargetSelectorParserTest {

    @Test
    void parsesNativeAndExtendedFilters() throws CommandSyntaxException {
        CloudTargetSelector selector = parse(
                "@e[distance=2..5,limit=3,sort=nearest,m=!creative,l=10,lm=2,rx=45,rxm=-45,ry=90,rym=-90]",
                false);

        assertEquals(2.0f, selector.minDistance());
        assertEquals(5.0f, selector.maxDistance());
        assertEquals(3, selector.maxResults());
        assertEquals(CloudSelectorSort.NEAREST, selector.sort());
        assertEquals(GameMode.CREATIVE, selector.gameMode());
        assertTrue(selector.invertedGameMode());
        assertEquals(2, selector.minLevel());
        assertEquals(10, selector.maxLevel());
        assertEquals(-45.0f, selector.minPitch());
        assertEquals(45.0f, selector.maxPitch());
        assertEquals(-90.0f, selector.minYaw());
        assertEquals(90.0f, selector.maxYaw());
    }

    @Test
    void parsesQuotedOptionValueContainingComma() throws CommandSyntaxException {
        CloudTargetSelector selector = parse("@a[name=\"Last, First\"]", true);

        assertEquals("Last, First", selector.name());
        assertFalse(selector.invertedName());
    }

    @Test
    void preservesRepeatedTagFilters() throws CommandSyntaxException {
        CloudTargetSelector selector = parse("@e[tag=hostile,tag=!boss]", false);

        assertEquals(List.of(new CloudSelectorTag("hostile", false), new CloudSelectorTag("boss", true)), selector.tags());
    }

    @Test
    void preservesEmptyTagFilters() throws CommandSyntaxException {
        CloudTargetSelector withoutTags = parse("@e[tag=]", false);
        CloudTargetSelector withTags = parse("@e[tag=!]", false);

        assertEquals(List.of(new CloudSelectorTag("", false)), withoutTags.tags());
        assertEquals(List.of(new CloudSelectorTag("", true)), withTags.tags());
    }

    @Test
    void acceptsEntityTypesFromTheRuntimeRegistry() throws CommandSyntaxException {
        Identifier customType = Identifier.parse("example:custom_entity");
        CloudTargetSelector selector = CloudTargetSelectorParser.parse("@e[type=example:custom_entity]", false, customType::equals);

        assertEquals(customType, selector.type());
    }

    @Test
    void rejectsInvalidNativeRanges() {
        assertThrows(CommandSyntaxException.class, () -> parse("@e[rm=5,r=2]", false));
        assertThrows(CommandSyntaxException.class, () -> parse("@e[rx=91]", false));
        assertThrows(CommandSyntaxException.class, () -> parse("@a[c=0]", true));
    }

    @Test
    void requiresPermissionToResolveTargetSelector() throws CommandSyntaxException {
        CloudTargetSelector selector = parse("@s", false);

        assertThrows(CommandSyntaxException.class, () -> selector.resolveEntities(sourceWithoutSelectorPermission()));
    }

    private static CommandSourceStack sourceWithoutSelectorPermission() {
        CommandSender sender = InterfaceProxy.create(CommandSender.class, Map.of("hasPermission", false));
        return InterfaceProxy.create(CommandSourceStack.class, Map.of("sender", sender));
    }

    private static CloudTargetSelector parse(String input, boolean playersOnly) throws CommandSyntaxException {
        return CloudTargetSelectorParser.parse(input, playersOnly, CloudTargetSelectorParserTest::isBuiltInEntityType);
    }

    private static boolean isBuiltInEntityType(Identifier identifier) {
        return EntityTypes.get(identifier).isPresent();
    }
}
