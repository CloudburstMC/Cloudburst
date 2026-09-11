package org.cloudburstmc.api.command.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.cloudburstmc.api.command.argument.resolver.RotationResolver;
import org.junit.jupiter.api.Test;

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
}
