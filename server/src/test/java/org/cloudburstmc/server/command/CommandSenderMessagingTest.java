package org.cloudburstmc.server.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class CommandSenderMessagingTest {

    @Test
    void sendsPlainTextWithoutParsingFormatting() {
        List<Component> messages = new ArrayList<>();
        CommandSender sender = sender(CommandSender.class, messages);

        sender.sendPlainMessage("<red>literal</red>");

        assertEquals(List.of(Component.text("<red>literal</red>")), messages);
    }

    @Test
    void parsesRichTextBeforeSendingTheComponent() {
        List<Component> messages = new ArrayList<>();
        CommandSender sender = sender(CommandSender.class, messages);

        sender.sendRichMessage("<green>Hello</green>");

        assertEquals(List.of(Component.text("Hello", NamedTextColor.GREEN)), messages);
    }

    @Test
    void insertsUntrustedReplacementTextWithoutParsingIt() {
        List<Component> messages = new ArrayList<>();
        CommandSender sender = sender(CommandSender.class, messages);

        sender.sendRichMessage("<green><input></green>",
                Placeholder.unparsed("input", "<red>literal</red>"));

        assertEquals(List.of(Component.text("<red>literal</red>", NamedTextColor.GREEN)), messages);
    }

    @Test
    void suppliesTheSenderToResolversAndAllowsStandardTagOverrides() {
        List<Component> messages = new ArrayList<>();
        CommandSender sender = sender(CommandSender.class, messages);
        TagResolver resolver = TagResolver.resolver("green", (arguments, context) -> {
            assertSame(sender, context.target());
            return Tag.inserting(Component.text("custom", NamedTextColor.RED));
        });

        sender.sendRichMessage("<green>", resolver);

        assertEquals(List.of(Component.text("custom", NamedTextColor.RED)), messages);
    }

    @Test
    void playersInheritTheSameMessagingHelpers() {
        List<Component> messages = new ArrayList<>();
        Player player = sender(Player.class, messages);

        player.sendPlainMessage("<blue>literal</blue>");
        player.sendRichMessage("<blue>formatted</blue>");

        assertEquals(List.of(Component.text("<blue>literal</blue>"),
                Component.text("formatted", NamedTextColor.BLUE)), messages);
    }

    private static <T extends CommandSender> T sender(Class<T> type, List<Component> messages) {
        return InterfaceProxy.create(type, (proxy, method, arguments) -> {
            if (method.getName().equals("sendMessage") && arguments != null
                    && arguments.length == 1 && arguments[0] instanceof Component message) {
                messages.add(message);
                return null;
            }

            if (method.isDefault()) {
                return InvocationHandler.invokeDefault(proxy, method, arguments);
            }

            throw new AssertionError("Unexpected sender access: " + method);
        });
    }
}
