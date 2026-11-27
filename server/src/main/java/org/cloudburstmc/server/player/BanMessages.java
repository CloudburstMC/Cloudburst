package org.cloudburstmc.server.player;

import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.cloudburstmc.api.ban.BanEntry;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Builds the message shown to a banned player: the ban, its reason and, for a temporary ban, when it ends.
 */
@UtilityClass
public class BanMessages {

    private static final DateTimeFormatter EXPIRATION = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z").withZone(ZoneId.systemDefault());

    public static Component player(BanEntry<?> entry) {
        return message("cloudburst.disconnect.banned", entry);
    }

    public static Component address(BanEntry<?> entry) {
        return message("cloudburst.disconnect.bannedIp", entry);
    }

    private static Component message(String key, BanEntry<?> entry) {
        TextComponent.Builder message = Component.text()
                .append(Component.translatable(key))
                .append(Component.newline())
                .append(Component.translatable("cloudburst.disconnect.banReason", Component.text(entry.reason())));

        Instant expiration = entry.expiration();
        if (expiration != null) {
            message.append(Component.newline()).append(Component.translatable("cloudburst.disconnect.banExpiration", Component.text(EXPIRATION.format(expiration))));
        }

        return message.build();
    }
}
