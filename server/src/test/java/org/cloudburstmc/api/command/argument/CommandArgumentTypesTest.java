package org.cloudburstmc.api.command.argument;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.cloudburstmc.api.command.CommandSourceStack;
import org.cloudburstmc.api.command.Commands;
import org.cloudburstmc.api.command.argument.resolver.BlockPositionResolver;
import org.cloudburstmc.api.command.argument.resolver.RotationResolver;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class CommandArgumentTypesTest {

    @Test
    void boundedIntegerUsesBrigadierRangeValidation() throws CommandSyntaxException {
        CommandArgumentType<Integer> argument = CommandArgumentTypes.integer(1, 3);

        assertEquals(2, argument.parse(new StringReader("2")));
        assertThrows(CommandSyntaxException.class, () -> argument.parse(new StringReader("0")));
        assertThrows(CommandSyntaxException.class, () -> argument.parse(new StringReader("4")));
    }

    @Test
    void boundedFloatUsesBrigadierRangeValidation() throws CommandSyntaxException {
        CommandArgumentType<Float> argument = CommandArgumentTypes.floatingPoint(-1.5f, 1.5f);

        assertEquals(0.5f, argument.parse(new StringReader("0.5")));
        assertThrows(CommandSyntaxException.class, () -> argument.parse(new StringReader("-2")));
        assertThrows(CommandSyntaxException.class, () -> argument.parse(new StringReader("2")));
    }

    @Test
    void booleanOnlyAcceptsAdvertisedValues() throws CommandSyntaxException {
        CommandArgumentType<Boolean> argument = CommandArgumentTypes.bool();

        assertTrue(argument.parse(new StringReader("true")));
        assertFalse(argument.parse(new StringReader("false")));
        assertThrows(CommandSyntaxException.class, () -> argument.parse(new StringReader("yes")));
    }

    @Test
    void rotationPreservesAbsoluteAndRelativeValues() throws CommandSyntaxException {
        CommandArgumentType<RotationResolver> argument = CommandArgumentTypes.rotation();

        assertEquals(15.0f, argument.parse(new StringReader("15")).resolve(40.0f));
        assertEquals(55.0f, argument.parse(new StringReader("~15")).resolve(40.0f));
        assertEquals(40.0f, argument.parse(new StringReader("~")).resolve(40.0f));
        assertEquals(0.5f, argument.parse(new StringReader(".5")).resolve(40.0f));
        assertThrows(CommandSyntaxException.class, () -> argument.parse(new StringReader("relative")));
        assertThrows(CommandSyntaxException.class, () -> argument.parse(new StringReader("9999999999999999999999999999999999999999")));
    }

    @Test
    void relativeBlockPositionConsumesAllCoordinatesBeforeTheBlockArgument() throws CommandSyntaxException {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        AtomicReference<Vector3i> position = new AtomicReference<>();
        AtomicReference<String> block = new AtomicReference<>();
        dispatcher.register(Commands.literal("setblock")
                .then(Commands.argument("position", CommandArgumentTypes.blockPosition())
                        .then(Commands.argument("block", CommandArgumentTypes.string()).executes(context -> {
                            position.set(context.getArgument("position", BlockPositionResolver.class).resolve(context.getSource()));
                            block.set(context.getArgument("block", String.class));
                            return 1;
                        }))));
        CommandSourceStack source = source(Vector3f.from(-0.25f, 80.5f, 10.75f));
        assertEquals(1, dispatcher.execute("setblock ~ ~ ~ redstone_block", source));
        assertEquals(Vector3i.from(-1, 80, 10), position.get());
        assertEquals("redstone_block", block.get());
        assertEquals(1, dispatcher.execute("setblock ~-1.5 90 ~2 redstone_block", source));
        assertEquals(Vector3i.from(-2, 90, 12), position.get());
    }

    @Test
    void positionSupportsAbsoluteAndRelativeFractions() throws CommandSyntaxException {
        StringReader reader = new StringReader("~.5 -2.25 ~-3 next");
        Vector3f position = CommandArgumentTypes.position().parse(reader).resolve(source(Vector3f.from(10, 20, 30)));
        assertEquals(Vector3f.from(10.5f, -2.25f, 27), position);
        assertEquals(" next", reader.getRemaining());
    }

    @Test
    void coordinatesRejectMissingMalformedAndNonFiniteInput() {
        for (String input : new String[]{"", "~", "~ ~", " ~ ~", "~  ~", "~x ~ ~", "0.5 1 2",
                "~ ~ 9999999999999999999999999999999999999999", "1~2 3 4"}) {
            assertThrows(CommandSyntaxException.class, () -> CommandArgumentTypes.blockPosition().parse(new StringReader(input)), input);
        }
        assertThrows(CommandSyntaxException.class, () -> CommandArgumentTypes.rotation().parse(new StringReader("~oops")));
        assertThrows(CommandSyntaxException.class, () -> CommandArgumentTypes.rotation().parse(new StringReader("1~2")));
    }

    private static CommandSourceStack source(Vector3f position) {
        Level level = InterfaceProxy.create(Level.class);
        return InterfaceProxy.create(CommandSourceStack.class, Map.of("location", Location.from(position, level)));
    }
}
